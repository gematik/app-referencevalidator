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

import de.gematik.refv.lib.fhir_context.entity.ContextResult;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Defines the Results of the Validation Operation.
 *
 * @param messages the collection (a set, a list) of {@link ResultMessage} containing relevant
 *     information/warnings/errors collected during the validation operation
 */
public record ValidationResult(@NonNull Collection<ResultMessage> messages)
    implements ContextResult {
  public ValidationResult {
    Objects.requireNonNull(messages, "The collection of messages must not be null");
    if (messages.isEmpty()) {
      throw new IllegalStateException("No messages provided");
    }
  }

  @Override
  public @NonNull String toString() {
    return asString();
  }

  /**
   * Creates a valid {@link ValidationResult} from given parameters,
   *
   * @param severity the severity of the message
   * @param messageId the ID of the message
   * @param messageContent the content of the message
   * @return a new instance of {@link ValidationResult}
   */
  public static @NonNull ValidationResult forMessage(
      @NonNull IssueSeverity severity, @NonNull String messageId, @NonNull String messageContent) {
    return new ValidationResult(
        List.of(ResultMessage.fromMessage(severity, messageId, messageContent)));
  }
}
