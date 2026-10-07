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

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/** Defines the configuration of Validation Policies for the FHIR Engine. */
public record ValidationPolicyConfiguration(
    @NonNull ValidationLevelPolicy validationLevelPolicy,
    @NonNull UnknownCodeSystemsPolicy unknownCodeSystemsPolicy,
    @NonNull ExampleCodeSystemsUsagePolicy exampleCodeSystemsUsagePolicy,
    @NonNull ExampleUrlsUsagePolicy exampleUrlsUsagePolicy,
    @NonNull ExampleRestReferencesPolicy exampleRestReferencesPolicy,
    @NonNull RecursiveModePolicy recursiveModePolicy,
    @NonNull AnyExtensionPolicy anyExtensionPolicy) {

  /** Policy that defines which level of details should be displayed in messages. */
  public enum ValidationLevelPolicy {
    SHOW_ALL,
    SHOW_WARNINGS_AND_ERRORS,
    SHOW_ONLY_ERRORS
  }

  /**
   * Policy that defines if Unknown Code Systems are allowed or not in FHIR Resources. In case the
   * {@link UnknownCodeSystemsPolicy#DISALLOWED} is selected, it may cause errors during the
   * validation.
   */
  public enum UnknownCodeSystemsPolicy {
    ALLOWED,
    DISALLOWED
  }

  /**
   * Policy that defines if Example Code Systems are allowed or not in FHIR Resources. In case the
   * {@link ExampleCodeSystemsUsagePolicy#DISALLOWED} is selected, it may cause errors during the
   * validation.
   */
  public enum ExampleCodeSystemsUsagePolicy {
    ALLOWED,
    DISALLOWED
  }

  /**
   * Policy that defines if Example URLs are allowed or not in FHIR Resources. In case the {@link
   * ExampleUrlsUsagePolicy#DISALLOWED} is selected, it may cause errors during the validation.
   */
  public enum ExampleUrlsUsagePolicy {
    ALLOWED,
    DISALLOWED
  }

  /**
   * Policy that defines if the REST References defined in a FHIR Resource are valid or not. In case
   * the {@link ExampleRestReferencesPolicy#DISALLOWED} is selected, it may cause errors during the
   * validation.
   */
  public enum ExampleRestReferencesPolicy {
    ALLOWED,
    DISALLOWED
  }

  /**
   * Policy that defines if the validation should be executed recursively for all the entities in a
   * FHIR Resource. If {@link RecursiveModePolicy#ALLOWED} is selected, it may increase time and
   * memory consumption.
   */
  public enum RecursiveModePolicy {
    ALLOWED,
    DISALLOWED
  }

  /** Policy that defines if any extension is allowed or not during the validation. */
  public enum AnyExtensionPolicy {
    ALLOWED,
    DISALLOWED
  }

  public ValidationPolicyConfiguration {
    Objects.requireNonNull(validationLevelPolicy, "The 'validationLevelPolicy' must be defined");
    Objects.requireNonNull(
        unknownCodeSystemsPolicy, "The 'unknownCodeSystemsPolicy' must be defined");
    Objects.requireNonNull(
        exampleCodeSystemsUsagePolicy, "The 'exampleCodeSystemsUsagePolicy' must be defined");
    Objects.requireNonNull(exampleUrlsUsagePolicy, "The 'exampleUrlsUsagePolicy' must be defined");
    Objects.requireNonNull(
        exampleRestReferencesPolicy, "The 'exampleRestReferencesPolicy' must be defined");
    Objects.requireNonNull(recursiveModePolicy, "The 'recursiveModePolicy' must be defined");
  }

  public static ValidationPolicyConfiguration defaultConfiguration() {
    return new ValidationPolicyConfiguration(
        ValidationLevelPolicy.SHOW_WARNINGS_AND_ERRORS,
        UnknownCodeSystemsPolicy.ALLOWED,
        ExampleCodeSystemsUsagePolicy.DISALLOWED,
        ExampleUrlsUsagePolicy.ALLOWED,
        ExampleRestReferencesPolicy.ALLOWED,
        RecursiveModePolicy.DISALLOWED,
        AnyExtensionPolicy.ALLOWED);
  }
}
