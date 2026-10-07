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
package de.gematik.refv.lib.fhir_context.entity;

import java.net.URI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContextConfigurationValidationTest {

  @DisplayName("Given a supported locale, when created, then the configuration is valid")
  @ParameterizedTest
  @ValueSource(strings = {"de", "en-GB", "en-US"})
  void expectSupportedLocalesAccepted(String locale) {
    final var config =
        Assertions.assertDoesNotThrow(
            () ->
                new ContextConfiguration(
                    FhirRelease.asR4(),
                    locale,
                    DisplayBehaviorConfiguration.defaultConfiguration(),
                    PackageDownloadConfiguration.defaultConfiguration(),
                    TerminologyConfiguration.defaultConfiguration(),
                    ValidationPolicyConfiguration.defaultConfiguration()));
    Assertions.assertEquals(locale, config.locale());
  }

  @DisplayName(
      "Given an unsupported locale, when created, then an IllegalArgumentException is thrown")
  @ParameterizedTest
  @ValueSource(strings = {"fr", "it", "de-AT", "  "})
  void expectUnsupportedLocaleRejected(String locale) {
    final var release = FhirRelease.asR4();
    final var displayBehavior = DisplayBehaviorConfiguration.defaultConfiguration();
    final var packageLoading = PackageDownloadConfiguration.defaultConfiguration();
    final var terminology = TerminologyConfiguration.defaultConfiguration();
    final var validationPolicy = ValidationPolicyConfiguration.defaultConfiguration();
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            new ContextConfiguration(
                release, locale, displayBehavior, packageLoading, terminology, validationPolicy));
  }

  @DisplayName("Given a null FHIR release, when created, then a NullPointerException is thrown")
  @Test
  void expectNullFhirReleaseThrows() {
    final var displayBehavior = DisplayBehaviorConfiguration.defaultConfiguration();
    final var packageLoading = PackageDownloadConfiguration.defaultConfiguration();
    final var terminology = TerminologyConfiguration.defaultConfiguration();
    final var validationPolicy = ValidationPolicyConfiguration.defaultConfiguration();
    Assertions.assertThrows(
        NullPointerException.class,
        () ->
            new ContextConfiguration(
                null, "de", displayBehavior, packageLoading, terminology, validationPolicy));
  }

  @DisplayName(
      "Given a specific release, when using the default factory, then that release is used")
  @Test
  void expectDefaultConfigurationWithRelease() {
    final var config = ContextConfiguration.defaultConfiguration(FhirRelease.asR5());
    Assertions.assertEquals(FhirRelease.asR5(), config.fhirRelease());
    Assertions.assertEquals("de", config.locale());
  }

  @DisplayName("Assume the default configuration can be initialized correctly")
  @Test
  void expectDefaultConfigurationCanBeInitialized() {
    final var contextConfig =
        Assertions.assertDoesNotThrow(
            (ThrowingSupplier<ContextConfiguration>) ContextConfiguration::defaultConfiguration);
    Assertions.assertNotNull(contextConfig);
    Assertions.assertEquals(FhirRelease.ofVersion("R4"), contextConfig.fhirRelease());
    Assertions.assertEquals("de", contextConfig.locale());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.defaultConfiguration(), contextConfig.displayBehavior());
    Assertions.assertEquals(
        PackageDownloadConfiguration.defaultConfiguration(), contextConfig.packageLoading());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.defaultConfiguration(), contextConfig.validationPolicy());
  }

  @DisplayName("Assume ZTS Configuration is correctly initialized")
  @Test
  void expectZtsConfigurationWorks() {
    final var ztsConfig =
        Assertions.assertDoesNotThrow(ZtsTerminologyServerConfiguration::defaultConfig);
    Assertions.assertEquals(
        URI.create("https://terminologien.bfarm.de"), ztsConfig.terminologyServerUri());
    Assertions.assertEquals(
        URI.create("https://terminologien.bfarm.de/tx/fhir"), ztsConfig.expansionEndpointUri());
  }
}
