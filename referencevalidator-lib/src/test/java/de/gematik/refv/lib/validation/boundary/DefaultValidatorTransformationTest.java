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
package de.gematik.refv.lib.validation.boundary;

import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.validation.control.DefaultValidator;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationRequest;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import de.gematik.refv.valmodule.api.entity.MessageTransformation;
import de.gematik.refv.valmodule.api.entity.SuppressionRule;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that {@link DefaultValidator} applies the module-declared message transformations and
 * suppression rules (carried by the {@link ValidationOptions}) to the engine output.
 */
class DefaultValidatorTransformationTest {
  private static final Logger log =
      LoggerFactory.getLogger(DefaultValidatorTransformationTest.class);

  private static ValidationRequest requestWith() {
    return new ValidationRequest(FhirResource.fromJson("{\"resourceType\":\"Patient\"}"));
  }

  private static ValidationOptions optionsWith(
      List<MessageTransformation> transformations, List<SuppressionRule> suppressions) {
    return new ValidationOptions(
        null,
        null,
        ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
        ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE,
        transformations,
        suppressions);
  }

  @DisplayName(
      "Given a transformation in the options, when validating, then the matching message severity is rewritten")
  @Test
  void expectTransformationAppliedDuringValidation() {
    final var transformation1 =
        new MessageTransformation(
            IssueSeverity.WARNING.getCode(),
            IssueSeverity.INFORMATION.getCode(),
            null,
            ".*dom-6.*",
            "http://hl7.org/fhir/StructureDefinition/Patient#dom-6");
    final var transformation2 =
        new MessageTransformation(
            IssueSeverity.WARNING.getCode(),
            IssueSeverity.INFORMATION.getCode(),
            null,
            ".*dom-6.*",
            "http://hl7.org/fhir/StructureDefinition/DomainResource#dom-6");
    try (var validator = new DefaultValidator(ContextConfiguration.defaultConfiguration())) {
      final var result =
          validator.validate(
              requestWith(), optionsWith(List.of(transformation1, transformation2), List.of()));
      printValidationResults(result);
      Assertions.assertEquals(
          IssueSeverity.INFORMATION, result.messages().iterator().next().severity());
    }
  }

  @DisplayName(
      "Given a suppression rule in the options, when validating, then the matching message is removed")
  @Test
  void expectSuppressionAppliedDuringValidation() {
    final var suppression = new SuppressionRule("R1", ".*dom-6.*", "Best Practice filter");
    try (var validator = new DefaultValidator(ContextConfiguration.defaultConfiguration())) {
      final var result =
          validator.validate(requestWith(), optionsWith(List.of(), List.of(suppression)));
      printValidationResults(result);
      Assertions.assertEquals(1, result.messages().size());
      Assertions.assertEquals(
          "No Validation Errors found", result.messages().iterator().next().messageContent());
    }
  }

  @DisplayName("Given the default options, when validating, then no rules are applied")
  @Test
  void expectDefaultOptionsApplyNoRules() {
    try (var validator = new DefaultValidator(ContextConfiguration.defaultConfiguration())) {
      final var result =
          validator.validate(requestWith(), ValidationOptions.defaultConfiguration());
      printValidationResults(result);
      Assertions.assertEquals(
          IssueSeverity.WARNING, result.messages().iterator().next().severity());
    }
  }

  void printValidationResults(@NonNull ValidationResult validationResult) {
    validationResult
        .messages()
        .forEach(
            resultMessage ->
                log.info(
                    "{}: {} - {}",
                    resultMessage.severity(),
                    resultMessage.messageId(),
                    resultMessage.messageContent()));
  }
}
