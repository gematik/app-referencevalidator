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

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContextResultTest {

  /** Minimal test implementation of the interface under test. */
  private record TestContextResult(Collection<ResultMessage> messages) implements ContextResult {}

  @DisplayName(
      "Given a result with messages, when calling asString, then all messages are rendered")
  @Test
  void expectAsStringRendersMessages() {
    final var result =
        new TestContextResult(
            List.of(
                ResultMessage.fromMessage(IssueSeverity.ERROR, "E-1", "an error"),
                ResultMessage.fromMessage(IssueSeverity.WARNING, "W-1", "a warning")));
    final String rendered = result.asString();
    Assertions.assertTrue(rendered.contains("an error"));
    Assertions.assertTrue(rendered.contains("a warning"));
    Assertions.assertTrue(rendered.contains("E-1"));
    Assertions.assertTrue(rendered.startsWith("Result ["));
  }

  @DisplayName(
      "Given a result without messages, when calling asString, then an empty result is rendered")
  @Test
  void expectAsStringOnEmptyMessages() {
    final var result = new TestContextResult(List.of());
    final String rendered = result.asString();
    Assertions.assertTrue(rendered.contains("Result ["));
  }
}
