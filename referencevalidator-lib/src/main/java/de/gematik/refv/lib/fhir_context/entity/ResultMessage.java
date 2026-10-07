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
package de.gematik.refv.lib.fhir_context.entity;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/** Defines a single Validation Message. */
public record ResultMessage(
    @NonNull String messageContent, @NonNull String messageId, @NonNull IssueSeverity severity) {

  public ResultMessage {
    Objects.requireNonNull(messageContent, "The message content must not be null");
    Objects.requireNonNull(messageId, "The message ID must not be null");
    Objects.requireNonNull(severity, "The severity must not be null");
  }

  public static @NonNull ResultMessage fromMessage(
      @NonNull IssueSeverity severity, @NonNull String messageId, @NonNull String messageContent) {
    return new ResultMessage(messageContent, messageId, severity);
  }

  public static @NonNull ResultMessage withSeverity(
      @NonNull ResultMessage other, @NonNull IssueSeverity newSeverity) {
    Objects.requireNonNull(other, "The validation message must not be null");
    return new ResultMessage(other.messageContent, other.messageId, newSeverity);
  }

  /**
   * Defines a Sorting Method for comparing multiple validation messages.
   *
   * <p>Order of Messages is reversed: FATAL messages have the highest priority, then ERROR, then
   * WARNING, etc.
   */
  public static Comparator<ResultMessage> comparatorBySeverity() {
    // anonymous record used internally
    record MessageSorter() implements Comparator<ResultMessage> {

      @Override
      public int compare(ResultMessage vm1, ResultMessage vm2) {
        return vm1.severity.ordinal() - vm2.severity.ordinal();
      }
    }
    return Collections.reverseOrder(new MessageSorter());
  }
}
