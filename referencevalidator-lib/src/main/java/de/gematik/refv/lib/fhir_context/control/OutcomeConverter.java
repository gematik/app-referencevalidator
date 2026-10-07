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
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.jspecify.annotations.NonNull;

/**
 * Converts HL7 {@link OperationOutcome} Data Structure into a {@link ValidationResult} instance.
 */
final class OutcomeConverter {
  private static final String SOURCE_MESSAGE = "source.msg";

  private OutcomeConverter() {}

  /**
   * Translates the HL7 {@link OperationOutcome} instance into a {@link ValidationResult}.
   *
   * @param operationOutcome the entity to process
   * @return a new {@link ValidationResult} instance
   */
  static ValidationResult toValidationResult(@NonNull OperationOutcome operationOutcome) {
    Objects.requireNonNull(operationOutcome, "OperationOutcome cannot be null");
    List<ResultMessage> outcomeMessages = new ArrayList<>();
    var issues =
        operationOutcome.getIssue().stream().map(OutcomeConverter::fromOutcomeComponent).toList();

    if (!issues.isEmpty()) {
      outcomeMessages.addAll(issues);
    }

    var valMessages =
        operationOutcome.getValidationMessages().stream()
            .map(OutcomeConverter::fromValidationMessage)
            .toList();

    if (!valMessages.isEmpty()) {
      outcomeMessages.addAll(valMessages);
    }

    if (outcomeMessages.isEmpty()) {
      outcomeMessages.add(
          ResultMessage.fromMessage(
              IssueSeverity.INFORMATION, MessageId.NO_ERROR.getCode(), "Validation succeeded"));
    }

    // sort by severity
    outcomeMessages.sort(ResultMessage.comparatorBySeverity());
    return new ValidationResult(outcomeMessages);
  }

  private static IssueSeverity toSeverity(
      org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity level) {
    return switch (level) {
      case INFORMATION, NULL -> IssueSeverity.INFORMATION;
      case WARNING -> IssueSeverity.WARNING;
      case ERROR -> IssueSeverity.ERROR;
      case FATAL -> IssueSeverity.FATAL;
    };
  }

  private static IssueSeverity toSeverity(OperationOutcome.IssueSeverity level) {
    return switch (level) {
      case SUCCESS, INFORMATION, NULL -> IssueSeverity.INFORMATION;
      case WARNING -> IssueSeverity.WARNING;
      case ERROR -> IssueSeverity.ERROR;
      case FATAL -> IssueSeverity.FATAL;
    };
  }

  private static @NonNull ResultMessage fromOutcomeComponent(
      OperationOutcome.@NonNull OperationOutcomeIssueComponent component) {
    final String messageId;
    if (component.hasUserData(SOURCE_MESSAGE)
        && component.getUserData(SOURCE_MESSAGE) instanceof ValidationMessage validationMessage) {
      messageId =
          Objects.requireNonNullElse(
              validationMessage.getMessageId(), MessageId.INTERNAL_CONTEXT.getCode());
    } else {
      messageId =
          Objects.requireNonNullElse(component.getId(), MessageId.INTERNAL_CONTEXT.getCode());
    }
    return ResultMessage.fromMessage(
        toSeverity(component.getSeverity()), messageId, component.getText());
  }

  private static @NonNull ResultMessage fromValidationMessage(
      @NonNull ValidationMessage validationMessage) {
    return ResultMessage.fromMessage(
        toSeverity(validationMessage.getLevel()),
        Objects.requireNonNullElse(validationMessage.getMessageId(), "UNDEFINED"),
        validationMessage.getMessage());
  }
}
