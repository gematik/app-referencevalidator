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
package de.gematik.refv.lib.validation.control;

import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import de.gematik.refv.valmodule.api.entity.MessageTransformation;
import de.gematik.refv.valmodule.api.entity.SuppressionRule;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageTransformationApplierTest {

  private static ValidationResult resultWith(ResultMessage... messages) {
    return new ValidationResult(List.of(messages));
  }

  private static ResultMessage msg(IssueSeverity severity, String id, String content) {
    return ResultMessage.fromMessage(severity, id, content);
  }

  @DisplayName(
      "Given no transformations and no suppressions, when applying, then the result is unchanged")
  @Test
  void expectNoRulesLeavesResultUnchanged() {
    final var result = resultWith(msg(IssueSeverity.ERROR, "E-1", "an error"));
    final var validationOptions = ValidationOptions.defaultConfiguration();
    final var out = MessageTransformationApplier.apply(result, validationOptions);
    Assertions.assertEquals(1, out.messages().size());
    Assertions.assertEquals(IssueSeverity.ERROR, out.messages().iterator().next().severity());
  }

  @DisplayName("Given null rules, when applying, then the result is unchanged")
  @Test
  void expectNullRulesLeaveResultUnchanged() {
    final var result = resultWith(msg(IssueSeverity.ERROR, "E-1", "an error"));
    final var validationOptions =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            null,
            null,
            null,
            null);
    final var out = MessageTransformationApplier.apply(result, validationOptions);
    Assertions.assertEquals(1, out.messages().size());
  }

  @DisplayName(
      "Given a severity-rewrite transformation matching by messageId, when applying, then the severity is changed")
  @Test
  void expectSeverityRewriteByMessageId() {
    final var result =
        resultWith(
            msg(
                IssueSeverity.ERROR,
                "Unknown_Code_in_Version",
                "Der angegebene Code 'http://hl7.org/fhir/encounter-status#finished' befindet sich nicht im ValueSet 'http://fhir.de/ValueSet/EncounterStatusDe|1.5.4'; Unbekannter Code finished in http://hl7.org/fhir/encounter-status Version 5.0.0"));
    final var transformation =
        new MessageTransformation(
            IssueSeverity.ERROR.getCode(),
            IssueSeverity.INFORMATION.getCode(),
            "http://hl7.org/fhir/encounter-status#finished",
            null,
            "Unknown_Code_in_Version");
    final var validationOptions =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(transformation),
            List.of());
    final var out = MessageTransformationApplier.apply(result, validationOptions);
    Assertions.assertEquals(IssueSeverity.INFORMATION, out.messages().iterator().next().severity());
  }

  @DisplayName(
      "Given a transformation matching by locatorString, when applying, then the severity is changed")
  @Test
  void expectSeverityRewriteByLocatorString() {
    final var result =
        resultWith(msg(IssueSeverity.ERROR, "E-1", "does not match any known slice meta.profile"));
    final var transformation =
        new MessageTransformation(
            IssueSeverity.ERROR.getCode(),
            IssueSeverity.WARNING.getCode(),
            "does not match any known slice",
            null,
            null);
    final var validationOptions =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(transformation),
            List.of());
    final var out = MessageTransformationApplier.apply(result, validationOptions);
    Assertions.assertEquals(IssueSeverity.WARNING, out.messages().iterator().next().severity());
  }

  @DisplayName(
      "Given a transformation matching by location regex, when applying, then the severity is changed")
  @Test
  void expectSeverityRewriteByLocationRegex() {
    final var result = resultWith(msg(IssueSeverity.ERROR, "E-1", "error at meta.profile field"));
    final var transformation =
        new MessageTransformation(
            IssueSeverity.ERROR.getCode(),
            IssueSeverity.WARNING.getCode(),
            null,
            ".*meta\\.profile.*",
            null);
    final var validationOptions =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(transformation),
            List.of());
    final var out = MessageTransformationApplier.apply(result, validationOptions);
    Assertions.assertEquals(IssueSeverity.WARNING, out.messages().iterator().next().severity());
  }

  @DisplayName(
      "Given content equal to another cached regex, when matching, then the requested regex is used")
  @Test
  void expectRegexCacheLookupUsesPatternRatherThanMessageContent() {
    final var cachedPattern = "cache-key-regex-98731";
    final var firstResult = resultWith(msg(IssueSeverity.ERROR, "E-1", "ordinary content"));
    final var validationOptions1 =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(),
            List.of(new SuppressionRule("R1", cachedPattern, "reason")));
    MessageTransformationApplier.apply(firstResult, validationOptions1);

    final var collisionResult = resultWith(msg(IssueSeverity.ERROR, "E-2", cachedPattern));
    final var validationOptions2 =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(),
            List.of(new SuppressionRule("R2", "different-regex-24680", "reason")));
    final var out = MessageTransformationApplier.apply(collisionResult, validationOptions2);
    Assertions.assertEquals("E-2", out.messages().iterator().next().messageId());
  }

  @DisplayName(
      "Given a transformation whose severityFrom does not match, when applying, then the message is unchanged")
  @Test
  void expectNoRewriteWhenSeverityFromDiffers() {
    final var result = resultWith(msg(IssueSeverity.WARNING, "E-1", "a warning"));
    final var transformation =
        new MessageTransformation(
            IssueSeverity.ERROR.getCode(), IssueSeverity.INFORMATION.getCode(), null, null, "E-1");
    final var validationOptions =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(transformation),
            List.of());
    final var out = MessageTransformationApplier.apply(result, validationOptions);
    Assertions.assertEquals(IssueSeverity.WARNING, out.messages().iterator().next().severity());
  }

  @DisplayName(
      "Given a transformation whose messageId does not match, when applying, then the message is unchanged")
  @Test
  void expectNoRewriteWhenMessageIdDiffers() {
    final var result = resultWith(msg(IssueSeverity.ERROR, "E-1", "content"));
    final var transformation =
        new MessageTransformation(
            IssueSeverity.ERROR.getCode(),
            IssueSeverity.INFORMATION.getCode(),
            null,
            null,
            "OTHER_ID");
    final var validationOptions =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(transformation),
            List.of());
    final var out = MessageTransformationApplier.apply(result, validationOptions);
    Assertions.assertEquals(IssueSeverity.ERROR, out.messages().iterator().next().severity());
  }

  @DisplayName(
      "Given a suppression rule matching the content, when applying, then the message is removed")
  @Test
  void expectSuppressionRemovesMessage() {
    final var result =
        resultWith(
            msg(IssueSeverity.ERROR, "E-1", "keep this"),
            msg(IssueSeverity.WARNING, "W-1", "this is __sorted noise"));
    final var rule = new SuppressionRule("GLOBAL-SORT-001", ".*__sorted.*", "not actionable");
    final var validationOptions =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(),
            List.of(rule));
    final var out = MessageTransformationApplier.apply(result, validationOptions);
    Assertions.assertEquals(1, out.messages().size());
    Assertions.assertEquals("E-1", out.messages().iterator().next().messageId());
  }

  @DisplayName(
      "Given all messages suppressed, when applying, then a synthetic OK message is returned")
  @Test
  void expectAllSuppressedReturnsOk() {
    final var result = resultWith(msg(IssueSeverity.ERROR, "E-1", "noise __sorted"));
    final var rule = new SuppressionRule("R", ".*__sorted.*", "reason");
    final var validationOptions =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(),
            List.of(rule));
    final var out = MessageTransformationApplier.apply(result, validationOptions);
    Assertions.assertEquals(1, out.messages().size());
    Assertions.assertEquals(IssueSeverity.INFORMATION, out.messages().iterator().next().severity());
  }

  @DisplayName(
      "Given a suppression rule that does not match, when applying, then the message is kept")
  @Test
  void expectNonMatchingSuppressionKeepsMessage() {
    final var result = resultWith(msg(IssueSeverity.ERROR, "E-1", "real error"));
    final var rule = new SuppressionRule("R", ".*something-else.*", "reason");
    final var validationOptions =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(),
            List.of(rule));
    final var out = MessageTransformationApplier.apply(result, validationOptions);
    Assertions.assertEquals(1, out.messages().size());
    Assertions.assertEquals(IssueSeverity.ERROR, out.messages().iterator().next().severity());
  }

  @DisplayName("Given a null result, when applying, then a NullPointerException is thrown")
  @Test
  void expectNullResultThrows() {
    final var validationOptions = ValidationOptions.defaultConfiguration();
    Assertions.assertThrows(
        NullPointerException.class,
        () -> MessageTransformationApplier.apply(null, validationOptions));
  }

  @DisplayName(
      "Given a transformation to each severity level, when applying, then the id severity is correct")
  @Test
  void expectAllTargetSeverities() {
    final var result = resultWith(msg(IssueSeverity.INFORMATION, "I", "c"));
    final var validationOptions1 =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(new MessageTransformation("information", "fatal", null, null, "I")),
            List.of());
    Assertions.assertEquals(
        IssueSeverity.FATAL,
        MessageTransformationApplier.apply(result, validationOptions1)
            .messages()
            .iterator()
            .next()
            .severity());

    final var validationOptions2 =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(new MessageTransformation(null, "error", null, null, "I")),
            List.of());
    Assertions.assertEquals(
        IssueSeverity.ERROR,
        MessageTransformationApplier.apply(result, validationOptions2)
            .messages()
            .iterator()
            .next()
            .severity());

    final var validationOptions3 =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(new MessageTransformation(null, "warning", null, null, "I")),
            List.of());
    Assertions.assertEquals(
        IssueSeverity.WARNING,
        MessageTransformationApplier.apply(result, validationOptions3)
            .messages()
            .iterator()
            .next()
            .severity());
  }

  @DisplayName(
      "Given a list of ignored codesystems and valuesets, when applying, they are filtered")
  @Test
  void expectFilterTerminologiesWorks() {
    final var result =
        resultWith(
            msg(
                IssueSeverity.ERROR,
                "I1",
                "Der angegebene Wert ('finished') ist nicht im ValueSet 'Encounter Status ValueSet' (http://fhir.de/ValueSet/EncounterStatusDe|1.5.4), und ein Code aus diesem Valueset ist erforderlich) (error message = Der angegebene Code 'http://hl7.org/fhir/encounter-status#finished' befindet sich nicht im ValueSet 'http://fhir.de/ValueSet/EncounterStatusDe|1.5.4'; Unbekannter Code finished in http://hl7.org/fhir/encounter-status Version 5.0.0) "));
    final var validationOptions =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(),
            List.of(),
            List.of(),
            List.of("http://fhir.de/ValueSet/EncounterStatusDe"));
    Assertions.assertEquals(
        IssueSeverity.INFORMATION,
        MessageTransformationApplier.apply(result, validationOptions)
            .messages()
            .iterator()
            .next()
            .severity());

    final var validationOptions2 =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(),
            List.of(),
            List.of("http://hl7.org/fhir/encounter-status"),
            List.of());
    Assertions.assertEquals(
        IssueSeverity.INFORMATION,
        MessageTransformationApplier.apply(result, validationOptions2)
            .messages()
            .iterator()
            .next()
            .severity());
  }
}
