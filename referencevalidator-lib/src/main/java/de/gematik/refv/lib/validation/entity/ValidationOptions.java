/*-
 * #%L
 * Validation Core Library
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
package de.gematik.refv.lib.validation.entity;

import de.gematik.refv.valmodule.api.entity.MessageTransformation;
import de.gematik.refv.valmodule.api.entity.SuppressionRule;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Validation Options passed through Input parameters (e.g. Command Line).
 *
 * @param profileToValidate Canonical URL of the FHIR Profile to validate against
 * @param profileFilterRegex Regular expression to proceed only with those resources, where the
 *     profile canonical matches the expression
 * @param validationMessagesFilterStrategy Strategy to select which kind of messages should be
 *     displayed and which not
 * @param profileValidityPeriodCheckStrategy Strategy to enforce that resources matched against
 *     packages with a validity period must be valid
 * @param messageTransformations module-declared severity-rewrite rules to apply to the result
 * @param suppressionRules module-declared rules suppressing known false-positive messages
 * @param ignoredCodeSystems CodeSystem canonicals whose validation errors are informational
 * @param ignoredValueSets ValueSet canonicals whose validation errors are informational
 */
public record ValidationOptions(
    @Nullable ProfileCanonical profileToValidate,
    @Nullable Pattern profileFilterRegex,
    @NonNull ValidationMessagesFilterStrategy validationMessagesFilterStrategy,
    @NonNull ProfileValidityPeriodCheckStrategy profileValidityPeriodCheckStrategy,
    @Nullable List<MessageTransformation> messageTransformations,
    @Nullable List<SuppressionRule> suppressionRules,
    @Nullable List<String> ignoredCodeSystems,
    @Nullable List<String> ignoredValueSets) {

  public enum ValidationMessagesFilterStrategy {
    KEEP_ALL,
    KEEP_ERRORS_ONLY,
    KEEP_ERRORS_AND_WARNINGS_ONLY
  }

  public enum ProfileValidityPeriodCheckStrategy {
    VALIDATE,
    IGNORE
  }

  public ValidationOptions {
    Objects.requireNonNull(
        validationMessagesFilterStrategy,
        "The 'validationMessagesFilterStrategy' parameter must be defined");
    Objects.requireNonNull(
        profileValidityPeriodCheckStrategy,
        "The 'profileValidityPeriodCheckStrategy' parameter must be defined");
    messageTransformations =
        List.copyOf(Objects.requireNonNullElse(messageTransformations, List.of()));
    suppressionRules = List.copyOf(Objects.requireNonNullElse(suppressionRules, List.of()));
    ignoredCodeSystems = List.copyOf(Objects.requireNonNullElse(ignoredCodeSystems, List.of()));
    ignoredValueSets = List.copyOf(Objects.requireNonNullElse(ignoredValueSets, List.of()));
  }

  /** Constructor without ignored terminology options, retained for source compatibility. */
  public ValidationOptions(
      @Nullable ProfileCanonical profileToValidate,
      @Nullable Pattern profileFilterRegex,
      @NonNull ValidationMessagesFilterStrategy validationMessagesFilterStrategy,
      @NonNull ProfileValidityPeriodCheckStrategy profileValidityPeriodCheckStrategy,
      @Nullable List<MessageTransformation> messageTransformations,
      @Nullable List<SuppressionRule> suppressionRules) {
    this(
        profileToValidate,
        profileFilterRegex,
        validationMessagesFilterStrategy,
        profileValidityPeriodCheckStrategy,
        messageTransformations,
        suppressionRules,
        List.of(),
        List.of());
  }

  /** Constructor with empty suppression rules and message transformations. */
  public ValidationOptions(
      @Nullable ProfileCanonical profileToValidate,
      @Nullable Pattern profileFilterRegex,
      @NonNull ValidationMessagesFilterStrategy validationMessagesFilterStrategy,
      @NonNull ProfileValidityPeriodCheckStrategy profileValidityPeriodCheckStrategy) {
    this(
        profileToValidate,
        profileFilterRegex,
        validationMessagesFilterStrategy,
        profileValidityPeriodCheckStrategy,
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

  public static ValidationOptions defaultConfiguration() {
    return new ValidationOptions(
        null,
        null,
        ValidationMessagesFilterStrategy.KEEP_ALL,
        ProfileValidityPeriodCheckStrategy.VALIDATE);
  }
}
