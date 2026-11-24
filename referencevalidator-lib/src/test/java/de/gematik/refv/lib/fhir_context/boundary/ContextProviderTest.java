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
package de.gematik.refv.lib.fhir_context.boundary;

import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.TerminologyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolver;
import de.gematik.refv.lib.package_resolver.control.DefaultPackageResolver;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ContextProviderTest {

  private static final Logger log = LoggerFactory.getLogger(ContextProviderTest.class);
  @TempDir private static Path packageCachePath;

  private static PackageDownloadConfiguration onlineMode;
  private static PackageDownloadConfiguration offlineMode;

  @BeforeAll
  static void beforeAll() {
    log.debug("#### Cache path: {}", packageCachePath);
    onlineMode =
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED, packageCachePath);
    offlineMode =
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED, packageCachePath);
  }

  @DisplayName("Checks that the same context is returned, given the same parameters")
  @Test
  void expectDoubleInitializationLeadsToDifferentContextSuccessful() {
    // Given a Context Provider
    final var contextProvider = ContextProvider.defaultProvider();
    // And a configuration with offline mode
    ContextConfiguration contextConfiguration =
        new ContextConfiguration(
            FhirRelease.asR4(),
            "de",
            DisplayBehaviorConfiguration.defaultConfiguration(),
            offlineMode,
            TerminologyConfiguration.defaultConfiguration(),
            ValidationPolicyConfiguration.defaultConfiguration());

    // When the first context is generated
    final var firstContext =
        Assertions.assertDoesNotThrow(
            () -> contextProvider.validationContext(contextConfiguration));
    Assertions.assertNotNull(firstContext);
    // And When the second context is generated
    final var secondContext =
        Assertions.assertDoesNotThrow(
            () -> contextProvider.validationContext(contextConfiguration));
    Assertions.assertNotNull(secondContext);

    // Then the two contexts are the same
    Assertions.assertNotEquals(firstContext, secondContext);
  }

  @DisplayName("Checks that a cloned context does not mean that the same validator is used")
  @Test
  void expectCloneContextLeadsToDifferentValidator() {
    // Given a Context Provider
    final var contextProvider = ContextProvider.defaultProvider();
    // And a configuration with offline mode
    ContextConfiguration contextConfiguration =
        new ContextConfiguration(
            FhirRelease.asR4(),
            "de",
            DisplayBehaviorConfiguration.defaultConfiguration(),
            offlineMode,
            TerminologyConfiguration.defaultConfiguration(),
            ValidationPolicyConfiguration.defaultConfiguration());

    // When the first context is generated
    final var firstContext =
        Assertions.assertDoesNotThrow(
            () -> contextProvider.validationContext(contextConfiguration));
    Assertions.assertNotNull(firstContext);
    // And When the second context is generated
    final var secondContext =
        Assertions.assertDoesNotThrow(() -> contextProvider.clone(firstContext));
    Assertions.assertNotNull(secondContext);

    // Then the two contexts are the same
    Assertions.assertNotEquals(firstContext, secondContext);
  }

  @DisplayName("Check that two contexts differ when the package set changes")
  @Test
  void expectConstructionOfDifferentContextsSuccessful() {
    // Given a Context Provider
    final var contextProvider = ContextProvider.defaultProvider();
    // And a configuration with online mode
    ContextConfiguration contextConfiguration =
        new ContextConfiguration(
            FhirRelease.asR4(),
            "de",
            DisplayBehaviorConfiguration.defaultConfiguration(),
            onlineMode,
            TerminologyConfiguration.defaultConfiguration(),
            ValidationPolicyConfiguration.defaultConfiguration());

    // And Given a Registry
    final PackageResolver packageResolver =
        new DefaultPackageResolver(contextConfiguration.packageLoading());

    // When a package is loaded in repository
    packageResolver.loadCore(contextConfiguration.fhirRelease());
    // And When the first context is generated
    final var firstContext =
        Assertions.assertDoesNotThrow(
            () -> contextProvider.validationContext(contextConfiguration));
    Assertions.assertNotNull(firstContext);
    // And When the second context is generated for a custom package
    final var secondContext =
        Assertions.assertDoesNotThrow(
            () -> contextProvider.validationContext(contextConfiguration, List.of()));
    Assertions.assertNotNull(secondContext);

    // Then the two contexts differ
    Assertions.assertNotEquals(firstContext, secondContext);
  }

  @DisplayName("Checks that the R5 Context can be initialized successfully")
  @Test
  void expectInitializationOfR5Works() {
    // Given a Context Provider
    final var contextProvider = ContextProvider.defaultProvider();
    // And a configuration with offline mode
    final var contextConfiguration =
        new ContextConfiguration(
            FhirRelease.asR5(),
            "de",
            DisplayBehaviorConfiguration.defaultConfiguration(),
            offlineMode,
            TerminologyConfiguration.defaultConfiguration(),
            ValidationPolicyConfiguration.defaultConfiguration());

    // When the first context is generated
    final var r5Context =
        Assertions.assertDoesNotThrow(
            () -> contextProvider.validationContext(contextConfiguration));
    Assertions.assertNotNull(r5Context);
  }
}
