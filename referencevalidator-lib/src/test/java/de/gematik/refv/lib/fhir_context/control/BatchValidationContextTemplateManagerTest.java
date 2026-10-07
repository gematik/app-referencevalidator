/*-
 * #%L
 * Reference Validator Library
 * %%
 * Copyright (C) 2024 - 2026 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.lib.fhir_context.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.ValidationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BatchValidationContextTemplateManagerTest {

  private final ContextConfiguration configuration = ContextConfiguration.defaultConfiguration();

  /// Requirement R6.1; Requirement R6.2
  @Test
  void sameConfigurationAndOrderedPackageSetReuseTemplateButReturnDistinctContexts()
      throws Exception {
    final var provider = mock(ContextProvider.class);
    final var template = contextTemplate();
    when(provider.validationContext(configuration, List.of("core#1", "guide#2")))
        .thenReturn(template);

    try (var manager = new BatchValidationContextTemplateManager(provider, 2);
        var first = manager.acquire(configuration, List.of("core#1", "guide#2"));
        var second = manager.acquire(configuration, List.of("core#1", "guide#2"))) {
      final var firstContext = first.newIsolatedContext();
      final var secondContext = second.newIsolatedContext();

      assertNotSame(firstContext, secondContext);
      try (var _ =
          verify(provider, times(1))
              .validationContext(configuration, List.of("core#1", "guide#2"))) {
        verify(template, times(2)).cloneContext(List.of());
      }
    }
  }

  @Test
  void configurationPackageOrderAndNormalizedArchiveCoordinatesArePartOfTheKey() throws Exception {
    final var provider = mock(ContextProvider.class);
    final var template = contextTemplate();
    final var otherConfiguration =
        new ContextConfiguration(
            configuration.fhirRelease(),
            "en-US",
            configuration.displayBehavior(),
            configuration.packageLoading(),
            configuration.terminology(),
            configuration.validationPolicy());
    when(provider.validationContext(argThat(configuration::equals), anyCollection()))
        .thenReturn(template);
    when(provider.validationContext(argThat(otherConfiguration::equals), anyCollection()))
        .thenReturn(template);

    try (var manager = new BatchValidationContextTemplateManager(provider, 4)) {
      manager.acquire(configuration, List.of("core#1", "guide#2")).close();
      manager.acquire(configuration, List.of("guide#2", "core#1")).close();
      manager.acquire(configuration, List.of("guide-2.tgz")).close();
      manager.acquire(configuration, List.of("guide#2")).close();
      manager.acquire(otherConfiguration, List.of("core#1", "guide#2")).close();

      verify(provider, times(4))
          .validationContext(any(ContextConfiguration.class), anyCollection());
      verify(provider, times(1)).validationContext(configuration, List.of("guide#2"));
      verify(provider, times(1))
          .validationContext(otherConfiguration, List.of("core#1", "guide#2"));
      verify(provider, times(1)).validationContext(configuration, List.of("guide#2", "core#1"));
    }
  }

  /// Requirement R6.1
  @Test
  void concurrentRequestsForSameKeyInitializeOnlyOneTemplate() throws Exception {
    final var provider = mock(ContextProvider.class);
    final var template = contextTemplate();
    final var initializationStarted = new CountDownLatch(1);
    final var allowInitialization = new CountDownLatch(1);
    final var initializationCount = new AtomicInteger();
    when(provider.validationContext(configuration, List.of("guide#2")))
        .thenAnswer(
            _ -> {
              initializationCount.incrementAndGet();
              initializationStarted.countDown();
              assertTrue(allowInitialization.await(5, TimeUnit.SECONDS));
              return template;
            });

    try (var manager = new BatchValidationContextTemplateManager(provider, 2);
        var executor = Executors.newFixedThreadPool(6)) {
      final var ready = new CountDownLatch(6);
      final var start = new CountDownLatch(1);
      final var futures =
          new ArrayList<Future<BatchValidationContextTemplateManager.TemplateLease>>();
      for (int i = 0; i < 6; i++) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  assertTrue(start.await(5, TimeUnit.SECONDS));
                  return manager.acquire(configuration, List.of("guide#2"));
                }));
      }

      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      assertTrue(initializationStarted.await(5, TimeUnit.SECONDS));
      allowInitialization.countDown();

      final var leases = futures.stream().map(this::get).toList();
      try {
        assertEquals(
            6,
            leases.stream()
                .map(BatchValidationContextTemplateManager.TemplateLease::newIsolatedContext)
                .distinct()
                .count());
        assertEquals(1, initializationCount.get());
        verify(provider, times(1)).validationContext(configuration, List.of("guide#2"));
      } finally {
        leases.forEach(BatchValidationContextTemplateManager.TemplateLease::close);
      }
    }
  }

  /// Requirement R6.3
  @Test
  void waitsAtCapacityEvictsOnlyAfterLeaseReleaseAndClosesTemplates() throws Exception {
    final var provider = mock(ContextProvider.class);
    final var firstTemplate = contextTemplate();
    final var secondTemplate = contextTemplate();
    when(provider.validationContext(configuration, List.of("first#1"))).thenReturn(firstTemplate);
    when(provider.validationContext(configuration, List.of("second#1"))).thenReturn(secondTemplate);

    try (var manager = new BatchValidationContextTemplateManager(provider, 1);
        var executor = Executors.newSingleThreadExecutor()) {
      final var firstLease = manager.acquire(configuration, List.of("first#1"));
      final var secondRequestStarted = new CountDownLatch(1);
      final var secondLeaseFuture =
          executor.submit(
              () -> {
                secondRequestStarted.countDown();
                return manager.acquire(configuration, List.of("second#1"));
              });

      assertTrue(secondRequestStarted.await(5, TimeUnit.SECONDS));
      assertThrows(TimeoutException.class, () -> secondLeaseFuture.get(100, TimeUnit.MILLISECONDS));
      verify(provider, times(0)).validationContext(configuration, List.of("second#1"));

      firstLease.close();
      final var secondLease = secondLeaseFuture.get(5, TimeUnit.SECONDS);
      verify(firstTemplate).close();
      verify(provider, times(1)).validationContext(configuration, List.of("second#1"));

      final var closeFuture = executor.submit(manager::close);
      assertThrows(TimeoutException.class, () -> closeFuture.get(100, TimeUnit.MILLISECONDS));
      secondLease.close();
      closeFuture.get(5, TimeUnit.SECONDS);
    }

    verify(secondTemplate).close();
  }

  @Test
  void failedInitializationIsRemovedSoLaterAcquisitionCanRetry() throws Exception {
    final var provider = mock(ContextProvider.class);
    final var template = contextTemplate();
    when(provider.validationContext(configuration, List.of("guide#2")))
        .thenThrow(new de.gematik.refv.lib.exceptions.InitializationException("failed"))
        .thenReturn(template);

    try (var manager = new BatchValidationContextTemplateManager(provider, 1)) {
      assertThrows(
          de.gematik.refv.lib.exceptions.InitializationException.class,
          () -> manager.acquire(configuration, List.of("guide#2")));
      try (var lease = manager.acquire(configuration, List.of("guide#2"))) {
        assertNotSame(template, lease.newIsolatedContext());
      }
      verify(provider, times(2)).validationContext(configuration, List.of("guide#2"));
    }
  }

  private ValidationContext contextTemplate() {
    final var template = mock(ValidationContext.class);
    when(template.cloneContext(anyCollection())).thenAnswer(_ -> mock(ValidationContext.class));
    return template;
  }

  private <T> T get(Future<T> future) {
    try {
      return future.get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new AssertionError("Concurrent template acquisition failed", e);
    }
  }
}
