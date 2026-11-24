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

import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import org.hl7.fhir.model.core.CodeableConcept;
import org.hl7.fhir.model.core.OperationOutcome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutcomeConverterTest {

  @DisplayName("Given a null outcome, when converting, then a NullPointerException is thrown")
  @Test
  void expectNullOutcomeThrows() {
    Assertions.assertThrows(
        NullPointerException.class, () -> OutcomeConverter.toValidationResult(null));
  }

  @DisplayName("Given an empty then NO_ERROR is returned")
  @Test
  void expectNoErrorOnEmptyOutcome() {
    final var outcome = new OperationOutcome();
    final var result = OutcomeConverter.toValidationResult(outcome);
    Assertions.assertEquals(
        MessageId.NO_ERROR.getCode(), result.messages().iterator().next().messageId());
  }

  @DisplayName(
      "Given an outcome with issues, when converting, then the messages are mapped with severity")
  @Test
  void expectIssuesMappedToMessages() {
    final var outcome = new OperationOutcome();
    issue(outcome, OperationOutcome.IssueSeverity.ERROR, "err-1", "an error");
    issue(outcome, OperationOutcome.IssueSeverity.WARNING, "warn-1", "a warning");

    final var result = OutcomeConverter.toValidationResult(outcome);

    Assertions.assertEquals(2, result.messages().size());
    // sorted by severity descending -> ERROR first
    final var first = result.messages().iterator().next();
    Assertions.assertEquals(IssueSeverity.ERROR, first.severity());
    Assertions.assertEquals("err-1", first.messageId());
    Assertions.assertEquals("an error", first.messageContent());
  }

  @DisplayName("Given an outcome with a null issue id, when converting, then UNDEFINED is used")
  @Test
  void expectNullIssueIdBecomesUndefined() {
    final var outcome = new OperationOutcome();
    issue(outcome, OperationOutcome.IssueSeverity.INFORMATION, null, "info");
    final var result = OutcomeConverter.toValidationResult(outcome);
    Assertions.assertEquals(
        MessageId.INTERNAL_CONTEXT.getCode(), result.messages().iterator().next().messageId());
  }

  @DisplayName(
      "Given an outcome with FATAL and INFORMATION, when converting, then FATAL sorts first")
  @Test
  void expectFatalSortsFirst() {
    final var outcome = new OperationOutcome();
    issue(outcome, OperationOutcome.IssueSeverity.INFORMATION, "i", "info");
    issue(outcome, OperationOutcome.IssueSeverity.FATAL, "f", "fatal");
    final var result = OutcomeConverter.toValidationResult(outcome);
    Assertions.assertEquals(IssueSeverity.FATAL, result.messages().iterator().next().severity());
  }

  @DisplayName(
      "Given an outcome with validation messages, when converting, then they are mapped with severity")
  @Test
  void expectValidationMessagesMapped() {
    final var outcome = new OperationOutcome();
    final var vm =
        new OperationOutcome.OperationOutcomeIssueComponent()
            .setSeverity(OperationOutcome.IssueSeverity.ERROR)
            .setCode(OperationOutcome.IssueType.INVALID)
            .setDiagnostics("a validation message");
    outcome.addIssue(vm);
    final var result = OutcomeConverter.toValidationResult(outcome);
    Assertions.assertEquals(1, result.messages().size());
    final var first = result.messages().iterator().next();
    Assertions.assertEquals(IssueSeverity.ERROR, first.severity());
    Assertions.assertEquals(MessageId.INTERNAL_CONTEXT.getCode(), first.messageId());
    Assertions.assertEquals("a validation message", first.messageContent());
  }

  @DisplayName("Given a validation message with a null id, when converting, then UNDEFINED is used")
  @Test
  void expectValidationMessageNullIdBecomesUndefined() {
    final var outcome = new OperationOutcome();
    final var vm =
        new OperationOutcome.OperationOutcomeIssueComponent()
            .setSeverity(OperationOutcome.IssueSeverity.WARNING)
            .setDiagnostics("warn");
    outcome.addIssue(vm);
    final var result = OutcomeConverter.toValidationResult(outcome);
    Assertions.assertEquals(
        MessageId.INTERNAL_CONTEXT.getCode(), result.messages().iterator().next().messageId());
  }

  @DisplayName(
      "Given both issues and validation messages, when converting, then both are merged and sorted")
  @Test
  void expectIssuesAndValidationMessagesMerged() {
    final var outcome = new OperationOutcome();
    issue(outcome, OperationOutcome.IssueSeverity.WARNING, "w-1", "a warning");
    final var vm =
        new OperationOutcome.OperationOutcomeIssueComponent()
            .setSeverity(OperationOutcome.IssueSeverity.FATAL)
            .setCode(OperationOutcome.IssueType.INVALID)
            .setDiagnostics("a fatal");
    outcome.addIssue(vm);
    final var result = OutcomeConverter.toValidationResult(outcome);
    Assertions.assertEquals(2, result.messages().size());
    Assertions.assertEquals(IssueSeverity.FATAL, result.messages().iterator().next().severity());
  }

  @DisplayName("Given an issue with SUCCESS severity, when converting, then it maps to INFORMATION")
  @Test
  void expectSuccessSeverityMapsToInformation() {
    final var outcome = new OperationOutcome();
    issue(outcome, OperationOutcome.IssueSeverity.SUCCESS, "s-1", "ok");
    final var result = OutcomeConverter.toValidationResult(outcome);
    Assertions.assertEquals(
        IssueSeverity.INFORMATION, result.messages().iterator().next().severity());
  }

  private static void issue(
      OperationOutcome outcome, OperationOutcome.IssueSeverity severity, String id, String text) {
    final var issue = outcome.addIssue();
    issue.setSeverity(severity);
    if (id != null) {
      issue.setId(id);
    }
    if (text != null) {
      issue.setDetails(new CodeableConcept().setText(text));
    }
  }
}
