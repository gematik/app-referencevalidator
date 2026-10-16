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
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.validation.entity.ValidationOptions.ValidationMessagesFilterStrategy;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidationResultFilterTest {

  private static ResultMessage msg(IssueSeverity severity, String id) {
    return ResultMessage.fromMessage(severity, id, "msg-" + id);
  }

  private static List<ResultMessage> allSeverities() {
    return List.of(
        msg(IssueSeverity.INFORMATION, "i"),
        msg(IssueSeverity.WARNING, "w"),
        msg(IssueSeverity.ERROR, "e"),
        msg(IssueSeverity.FATAL, "f"));
  }

  @DisplayName("Given KEEP_ALL, when filtering, then all messages are kept")
  @Test
  void expectKeepAllRetainsEverything() {
    final var result =
        ValidationResultFilter.apply(allSeverities(), ValidationMessagesFilterStrategy.KEEP_ALL);
    Assertions.assertEquals(4, result.messages().size());
  }

  @DisplayName("Given KEEP_ERRORS_AND_WARNINGS_ONLY, when filtering, then INFORMATION is removed")
  @Test
  void expectKeepErrorsAndWarningsRemovesInformation() {
    final var result =
        ValidationResultFilter.apply(
            allSeverities(), ValidationMessagesFilterStrategy.KEEP_ERRORS_AND_WARNINGS_ONLY);
    Assertions.assertEquals(3, result.messages().size());
    Assertions.assertTrue(
        result.messages().stream().noneMatch(m -> m.severity() == IssueSeverity.INFORMATION));
  }

  @DisplayName(
      "Given KEEP_ERRORS_ONLY with errors present, when filtering, then only errors and fatals remain")
  @Test
  void expectKeepErrorsOnlyRetainsErrorsAndFatals() {
    final var result =
        ValidationResultFilter.apply(
            allSeverities(), ValidationMessagesFilterStrategy.KEEP_ERRORS_ONLY);
    Assertions.assertEquals(2, result.messages().size());
    Assertions.assertTrue(
        result.messages().stream()
            .allMatch(
                m -> m.severity() == IssueSeverity.ERROR || m.severity() == IssueSeverity.FATAL));
  }

  @DisplayName(
      "Given KEEP_ERRORS_ONLY with no errors, when filtering, then a synthetic OK message is returned")
  @Test
  void expectKeepErrorsOnlyWithoutErrorsReturnsNoError() {
    final var result =
        ValidationResultFilter.apply(
            List.of(msg(IssueSeverity.INFORMATION, "i"), msg(IssueSeverity.WARNING, "w")),
            ValidationMessagesFilterStrategy.KEEP_ERRORS_ONLY);
    Assertions.assertEquals(1, result.messages().size());
    Assertions.assertEquals(
        IssueSeverity.INFORMATION, result.messages().iterator().next().severity());
    Assertions.assertEquals(
        MessageId.NO_ERROR.getCode(), result.messages().iterator().next().messageId());
  }

  @DisplayName(
      "Given a null message collection, when filtering, then a NullPointerException is thrown")
  @Test
  void expectNullMessagesThrow() {
    Assertions.assertThrows(
        NullPointerException.class,
        () -> ValidationResultFilter.apply(null, ValidationMessagesFilterStrategy.KEEP_ALL));
  }

  @DisplayName("Given a null strategy, when filtering, then a NullPointerException is thrown")
  @Test
  void expectNullStrategyThrows() {
    final var messages = allSeverities();
    Assertions.assertThrows(
        NullPointerException.class, () -> ValidationResultFilter.apply(messages, null));
  }
}
