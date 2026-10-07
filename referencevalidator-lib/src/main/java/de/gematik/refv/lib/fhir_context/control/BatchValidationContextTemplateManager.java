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

import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.ValidationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds reusable validation-context templates for one bounded batch lifetime.
 *
 * <p>Templates are only used to create isolated contexts. A template is never returned directly for
 * validation. The manager is safe for concurrent acquisition and closes templates on eviction or
 * when the manager is closed.
 */
final class BatchValidationContextTemplateManager implements AutoCloseable {

  private static final Logger log =
      LoggerFactory.getLogger(BatchValidationContextTemplateManager.class);

  private final ValidationContextTemplateFactory templateFactory;
  private final int capacity;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition stateChanged = lock.newCondition();
  private final LinkedHashMap<ValidationPackageSetKey, Entry> templates =
      new LinkedHashMap<>(16, 0.75f, true);
  private int closingTemplates;
  private boolean closed;

  public BatchValidationContextTemplateManager(
      @NonNull ContextProvider contextProvider, int capacity) {
    this(Objects.requireNonNull(contextProvider, "contextProvider")::validationContext, capacity);
  }

  BatchValidationContextTemplateManager(
      ValidationContextTemplateFactory templateFactory, int capacity) {
    this.templateFactory = Objects.requireNonNull(templateFactory, "templateFactory");
    if (capacity < 1) {
      throw new IllegalArgumentException("Template capacity must be at least one");
    }
    this.capacity = capacity;
  }

  /**
   * Acquires a lease for a package-set template. The returned lease can create a fresh isolated
   * context for each resource using that package set.
   *
   * @param configuration complete FHIR context configuration used as part of the cache key
   * @param packageCoordinates ordered package coordinates to load
   * @return a lease that must be closed when the caller no longer needs the template
   * @throws InitializationException if template initialization fails
   * @throws InterruptedException if waiting for cache capacity or another initialization is
   *     interrupted
   */
  public @NonNull TemplateLease acquire(
      @NonNull ContextConfiguration configuration, @NonNull Collection<String> packageCoordinates)
      throws InitializationException, InterruptedException {
    final var key = ValidationPackageSetKey.of(configuration, packageCoordinates);
    final var reservation = reserve(key);
    final var entry = reservation.entry();

    if (!reservation.initialize()) {
      return new TemplateLease(this, entry);
    }

    initializeTemplate(configuration, key, entry);
    return new TemplateLease(this, entry);
  }

  private Reservation reserve(ValidationPackageSetKey key)
      throws InterruptedException, InitializationException {
    while (true) {
      final var attempt = reserveOnce(key);
      if (attempt instanceof Reservation reservation) {
        return reservation;
      }
      if (attempt instanceof Eviction(ValidationContext template)) {
        try {
          closeContext(template);
        } finally {
          finishEviction();
        }
      }
    }
  }

  private ReservationAttempt reserveOnce(ValidationPackageSetKey key)
      throws InterruptedException, InitializationException {
    lock.lockInterruptibly();
    try {
      ensureOpen();
      final var existing = templates.get(key);
      if (existing != null) {
        return reserveExisting(key, existing);
      }
      return reserveNew(key);
    } finally {
      lock.unlock();
    }
  }

  private ReservationAttempt reserveExisting(ValidationPackageSetKey key, Entry entry)
      throws InterruptedException, InitializationException {
    while (entry.initializing && !closed && templates.get(key) == entry) {
      stateChanged.await();
    }
    ensureOpen();
    if (entry.failure != null) {
      rethrow(entry.failure);
    }
    if (templates.get(key) != entry) {
      return Retry.INSTANCE;
    }
    entry.leases++;
    return new Reservation(entry, false);
  }

  private ReservationAttempt reserveNew(ValidationPackageSetKey key) throws InterruptedException {
    while (true) {
      if (templates.containsKey(key)) {
        return Retry.INSTANCE;
      }
      if (templates.size() + closingTemplates < capacity) {
        return addTemplateEntry(key);
      }

      final var candidate = leastRecentlyUsedInactiveEntry();
      if (candidate != null) {
        templates.remove(candidate.getKey());
        closingTemplates++;
        return new Eviction(candidate.getValue().template);
      }

      stateChanged.await();
      ensureOpen();
    }
  }

  private Reservation addTemplateEntry(ValidationPackageSetKey key) {
    final var entry = new Entry();
    entry.leases = 1;
    entry.initializing = true;
    templates.put(key, entry);
    return new Reservation(entry, true);
  }

  private Map.Entry<ValidationPackageSetKey, Entry> leastRecentlyUsedInactiveEntry() {
    return templates.entrySet().stream()
        .filter(entry -> !entry.getValue().initializing && entry.getValue().leases == 0)
        .findFirst()
        .orElse(null);
  }

  private void finishEviction() {
    lock.lock();
    try {
      closingTemplates--;
      stateChanged.signalAll();
    } finally {
      lock.unlock();
    }
  }

  private void initializeTemplate(
      ContextConfiguration configuration, ValidationPackageSetKey key, Entry entry)
      throws InitializationException {
    var initialized = false;
    try {
      final var template =
          Objects.requireNonNull(
              templateFactory.create(configuration, key.packageCoordinates()),
              "Context provider returned a null validation context");
      if (template instanceof DefaultValidationContext defaultTemplate) {
        defaultTemplate.prepareForConcurrentCloning();
      }
      entry.template = template;
      initialized = true;
    } catch (Exception exception) {
      failInitialization(key, entry, exception);
      rethrow(exception);
    } finally {
      if (!initialized && entry.initializing) {
        failInitialization(
            key, entry, new IllegalStateException("Template initialization did not complete"));
      }
    }

    lock.lock();
    try {
      entry.initializing = false;
      stateChanged.signalAll();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void close() {
    final List<ValidationContext> contexts;
    lock.lock();
    try {
      if (closed) {
        return;
      }
      closed = true;
      stateChanged.signalAll();
      while (closingTemplates > 0
          || templates.values().stream()
              .anyMatch(entry -> entry.initializing || entry.leases > 0)) {
        stateChanged.awaitUninterruptibly();
      }
      contexts = templates.values().stream().map(entry -> entry.template).toList();
      templates.clear();
      stateChanged.signalAll();
    } finally {
      lock.unlock();
    }
    contexts.forEach(BatchValidationContextTemplateManager::closeContext);
  }

  private void release(Entry entry) {
    lock.lock();
    try {
      if (entry.leases > 0) {
        entry.leases--;
      }
      stateChanged.signalAll();
    } finally {
      lock.unlock();
    }
  }

  private void failInitialization(ValidationPackageSetKey key, Entry entry, Throwable failure) {
    lock.lock();
    try {
      entry.failure = failure;
      entry.initializing = false;
      entry.leases = 0;
      templates.remove(key, entry);
      stateChanged.signalAll();
    } finally {
      lock.unlock();
    }
  }

  private void ensureOpen() {
    if (closed) {
      throw new IllegalStateException("The batch validation context manager is closed");
    }
  }

  private static void closeContext(ValidationContext context) {
    if (context == null) {
      return;
    }
    try {
      context.close();
    } catch (Exception e) {
      log.warn("Failed to close a validation context template", e);
    }
  }

  private static void rethrow(Throwable failure) throws InitializationException {
    if (failure instanceof InitializationException initializationException) {
      throw initializationException;
    }
    if (failure instanceof RuntimeException runtimeException) {
      throw runtimeException;
    }
    if (failure instanceof Error error) {
      throw error;
    }
    throw new IllegalStateException("Unexpected template initialization failure", failure);
  }

  private static final class Entry {
    private ValidationContext template;
    private Throwable failure;
    private int leases;
    private boolean initializing;
  }

  private sealed interface ReservationAttempt permits Reservation, Eviction, Retry {}

  private record Reservation(Entry entry, boolean initialize) implements ReservationAttempt {}

  private record Eviction(ValidationContext template) implements ReservationAttempt {}

  private enum Retry implements ReservationAttempt {
    INSTANCE
  }

  /** A lease protects its template from eviction until it is closed. */
  public static final class TemplateLease implements AutoCloseable {
    private final BatchValidationContextTemplateManager manager;
    private final Entry entry;
    private boolean closed;

    private TemplateLease(BatchValidationContextTemplateManager manager, Entry entry) {
      this.manager = manager;
      this.entry = entry;
    }

    /** Creates a new resource-isolated context from the leased template. */
    public synchronized @NonNull ValidationContext newIsolatedContext() {
      if (closed) {
        throw new IllegalStateException("The template lease is closed");
      }
      synchronized (entry) {
        return entry.template.cloneContext(List.of());
      }
    }

    @Override
    public synchronized void close() {
      if (!closed) {
        closed = true;
        manager.release(entry);
      }
    }
  }

  private record ValidationPackageSetKey(
      ContextConfiguration configuration, List<String> packageCoordinates) {
    ValidationPackageSetKey {
      Objects.requireNonNull(configuration, "configuration");
      packageCoordinates = List.copyOf(packageCoordinates);
    }

    static ValidationPackageSetKey of(
        ContextConfiguration configuration, Collection<String> packageCoordinates) {
      Objects.requireNonNull(packageCoordinates, "packageCoordinates");
      final var normalized = new ArrayList<String>(packageCoordinates.size());
      for (var coordinate : packageCoordinates) {
        Objects.requireNonNull(coordinate, "package coordinate");
        normalized.add(normalize(coordinate));
      }
      return new ValidationPackageSetKey(configuration, normalized);
    }

    private static String normalize(String coordinate) {
      if (coordinate.contains(LocalArchive.PACKAGE_SEPARATOR)
          && coordinate.endsWith(LocalArchive.ARCHIVE_PACKAGE_EXTENSION)) {
        return LocalArchive.parse(coordinate).coordinates();
      }
      return coordinate;
    }
  }
}
