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

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Configuration of the FHIR Engine.
 *
 * @param fhirRelease defines which FHIR Release Version must be used.
 * @param locale defines the language for displaying the messages.
 * @param displayBehavior defines the configuration for displaying messages.
 * @param packageLoading defines the configuration for loading packages.
 * @param terminology defines the configuration for accessing a terminology server.
 * @param validationPolicy defines the configuration for validating resources.
 */
public record ContextConfiguration(
    @NonNull FhirRelease fhirRelease,
    @NonNull String locale,
    @NonNull DisplayBehaviorConfiguration displayBehavior,
    @NonNull PackageDownloadConfiguration packageLoading,
    @NonNull TerminologyConfiguration terminology,
    @NonNull ValidationPolicyConfiguration validationPolicy) {

  public ContextConfiguration {
    Objects.requireNonNull(fhirRelease, "The 'fhirRelease' must be defined");
    Objects.requireNonNull(locale, "The 'locale' configuration must be defined");
    Objects.requireNonNull(displayBehavior, "The 'displayBehavior' configuration must be defined");
    Objects.requireNonNull(packageLoading, "The 'packageLoading' configuration must be defined");
    Objects.requireNonNull(terminology, "The 'terminology' configuration must be defined");
    Objects.requireNonNull(
        validationPolicy, "The 'validationPolicy' configuration must be defined");

    final var allowedLocales =
        List.of(
            Locale.UK.toLanguageTag(), Locale.US.toLanguageTag(), Locale.GERMAN.toLanguageTag());
    if (locale.isBlank() || allowedLocales.stream().noneMatch(s -> s.contentEquals(locale))) {
      throw new IllegalArgumentException(
          "The 'locale' must be one between " + String.join(",", allowedLocales));
    }
  }

  public static @NonNull ContextConfiguration defaultConfiguration() {
    return defaultConfiguration(FhirRelease.ofVersion("R4"));
  }

  public static @NonNull ContextConfiguration defaultConfiguration(
      @NonNull FhirRelease fhirRelease) {
    return new ContextConfiguration(
        fhirRelease,
        "de",
        DisplayBehaviorConfiguration.defaultConfiguration(),
        PackageDownloadConfiguration.defaultConfiguration(),
        TerminologyConfiguration.defaultConfiguration(),
        ValidationPolicyConfiguration.defaultConfiguration());
  }
}
