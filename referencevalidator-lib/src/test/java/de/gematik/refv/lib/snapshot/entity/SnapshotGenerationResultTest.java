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
package de.gematik.refv.lib.snapshot.entity;

import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnapshotGenerationResultTest {

  @DisplayName(
      "Given a message, when using forMessage, then a result with a single message is built")
  @Test
  void expectForMessageBuildsSingleMessageResult() {
    final var result =
        SnapshotGenerationResult.forMessage(IssueSeverity.INFORMATION, "ID-1", "done");
    Assertions.assertEquals(1, result.messages().size());
    final var message = result.messages().iterator().next();
    Assertions.assertEquals("done", message.messageContent());
    Assertions.assertEquals(IssueSeverity.INFORMATION, message.severity());
  }

  @DisplayName(
      "Given an empty message collection, when created, then an IllegalStateException is thrown")
  @Test
  void expectEmptyMessagesThrow() {
    final var messages = List.<ResultMessage>of();
    Assertions.assertThrows(
        IllegalStateException.class, () -> new SnapshotGenerationResult(messages));
  }

  @DisplayName(
      "Given a null message collection, when created, then a NullPointerException is thrown")
  @Test
  void expectNullMessagesThrow() {
    Assertions.assertThrows(NullPointerException.class, () -> new SnapshotGenerationResult(null));
  }

  @DisplayName("Given a result, when calling toString, then the messages are rendered")
  @Test
  void expectToStringRendersMessages() {
    final var result =
        new SnapshotGenerationResult(
            List.of(ResultMessage.fromMessage(IssueSeverity.ERROR, "E-1", "boom")));
    Assertions.assertTrue(result.toString().contains("boom"));
  }
}
