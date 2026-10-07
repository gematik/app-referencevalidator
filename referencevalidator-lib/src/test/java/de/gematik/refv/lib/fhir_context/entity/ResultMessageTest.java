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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResultMessageTest {

  @DisplayName("Given valid fields, when creating a message, then the accessors return the values")
  @Test
  void expectCreationWorks() {
    final var message = new ResultMessage("content", "ID-1", IssueSeverity.ERROR);
    Assertions.assertEquals("content", message.messageContent());
    Assertions.assertEquals("ID-1", message.messageId());
    Assertions.assertEquals(IssueSeverity.ERROR, message.severity());
  }

  @DisplayName("Given a null field, when creating a message, then a NullPointerException is thrown")
  @Test
  void expectNullFieldThrows() {
    Assertions.assertThrows(
        NullPointerException.class, () -> new ResultMessage(null, "ID-1", IssueSeverity.ERROR));
    Assertions.assertThrows(
        NullPointerException.class, () -> new ResultMessage("content", null, IssueSeverity.ERROR));
    Assertions.assertThrows(
        NullPointerException.class, () -> new ResultMessage("content", "ID-1", null));
  }

  @DisplayName("Given severity, id and content, when using fromMessage, then a message is built")
  @Test
  void expectFromMessageFactory() {
    final var message = ResultMessage.fromMessage(IssueSeverity.WARNING, "ID-2", "warn");
    Assertions.assertEquals("warn", message.messageContent());
    Assertions.assertEquals("ID-2", message.messageId());
    Assertions.assertEquals(IssueSeverity.WARNING, message.severity());
  }

  @DisplayName(
      "Given a message, when changing severity, then a copy with the new severity is returned")
  @Test
  void expectWithSeverityCreatesCopy() {
    final var original = ResultMessage.fromMessage(IssueSeverity.INFORMATION, "ID-3", "info");
    final var changed = ResultMessage.withSeverity(original, IssueSeverity.FATAL);
    Assertions.assertEquals(IssueSeverity.FATAL, changed.severity());
    Assertions.assertEquals(original.messageContent(), changed.messageContent());
    Assertions.assertEquals(original.messageId(), changed.messageId());
  }

  @DisplayName(
      "Given a null message, when changing severity, then a NullPointerException is thrown")
  @Test
  void expectWithSeverityNullThrows() {
    Assertions.assertThrows(
        NullPointerException.class, () -> ResultMessage.withSeverity(null, IssueSeverity.ERROR));
  }

  @DisplayName(
      "Given messages, when sorting by severity, then FATAL comes first and INFORMATION last")
  @Test
  void expectComparatorBySeverityOrdersDescending() {
    final var messages =
        new ArrayList<>(
            List.of(
                ResultMessage.fromMessage(IssueSeverity.INFORMATION, "i", "info"),
                ResultMessage.fromMessage(IssueSeverity.FATAL, "f", "fatal"),
                ResultMessage.fromMessage(IssueSeverity.WARNING, "w", "warn"),
                ResultMessage.fromMessage(IssueSeverity.ERROR, "e", "error")));
    messages.sort(ResultMessage.comparatorBySeverity());
    Assertions.assertEquals(IssueSeverity.FATAL, messages.get(0).severity());
    Assertions.assertEquals(IssueSeverity.ERROR, messages.get(1).severity());
    Assertions.assertEquals(IssueSeverity.WARNING, messages.get(2).severity());
    Assertions.assertEquals(IssueSeverity.INFORMATION, messages.get(3).severity());
  }
}
