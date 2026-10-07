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

import de.gematik.refv.lib.exceptions.ValidationException;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import java.util.Collection;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/** Returns results with a severity configured through options. */
final class ValidationResultFilter {
  private ValidationResultFilter() {}

  /**
   * Returns messages that are only ERROR or FATAL or WARNING, depending on the option given.
   *
   * @param messagesToFilter messages to filter
   * @param validationMessagesFilterStrategy the strategy to apply
   * @return the filtered result
   */
  static @NonNull ValidationResult apply(
      @NonNull Collection<ResultMessage> messagesToFilter,
      ValidationOptions.@NonNull ValidationMessagesFilterStrategy
          validationMessagesFilterStrategy) {
    Objects.requireNonNull(messagesToFilter, "Cannot filter null entries");
    Objects.requireNonNull(validationMessagesFilterStrategy, "The filter strategy cannot be null");

    final var processedMessages =
        messagesToFilter.stream()
            .map(
                resultMessage ->
                    resultMessage.messageId().startsWith("REFV-")
                        ? resultMessage
                        : ResultMessage.fromMessage(
                            resultMessage.severity(),
                            MessageId.VALIDATION_ERROR.getCode(),
                            resultMessage.messageContent()))
            .toList();

    if (validationMessagesFilterStrategy
        == ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL) {
      return new ValidationResult(processedMessages);
    }

    if (validationMessagesFilterStrategy
        == ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ERRORS_AND_WARNINGS_ONLY) {
      var filteredMessages =
          processedMessages.stream()
              .filter(
                  m ->
                      m.severity() == IssueSeverity.ERROR
                          || m.severity() == IssueSeverity.FATAL
                          || m.severity() == IssueSeverity.WARNING)
              .toList();
      return new ValidationResult(filteredMessages);
    }

    if (validationMessagesFilterStrategy
        == ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ERRORS_ONLY) {
      var filteredMessages =
          processedMessages.stream()
              .filter(
                  m -> m.severity() == IssueSeverity.ERROR || m.severity() == IssueSeverity.FATAL)
              .toList();

      if (filteredMessages.isEmpty()) {
        return ValidationResult.forMessage(
            IssueSeverity.INFORMATION, MessageId.NO_ERROR.getCode(), "No Validation Errors found");
      }
      return new ValidationResult(filteredMessages);
    }

    throw new ValidationException("Filter " + validationMessagesFilterStrategy + " is unsupported");
  }
}
