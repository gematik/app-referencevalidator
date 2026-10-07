/*-
 * #%L
 * Validation Module API
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
package de.gematik.refv.test.valmodule.api.entity;

import de.gematik.refv.valmodule.api.entity.MessageTransformation;
import de.gematik.refv.valmodule.api.entity.SuppressionRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimpleEntitiesTest {

  @DisplayName("Given a MessageTransformation, when created, then all accessors return the values")
  @Test
  void expectMessageTransformationAccessors() {
    final var t = new MessageTransformation("error", "warning", "locator", ".*regex", "id-1");
    Assertions.assertEquals("error", t.severityLevelFrom());
    Assertions.assertEquals("warning", t.severityLevelTo());
    Assertions.assertEquals("locator", t.locatorString());
    Assertions.assertEquals(".*regex", t.messageLocationRegex());
    Assertions.assertEquals("id-1", t.messageId());
  }

  @DisplayName("Given a SuppressionRule, when created, then all accessors return the values")
  @Test
  void expectSuppressionRuleAccessors() {
    final var rule = new SuppressionRule("rule-1", ".*pattern", "justification");
    Assertions.assertEquals("rule-1", rule.ruleId());
    Assertions.assertEquals(".*pattern", rule.messagePattern());
    Assertions.assertEquals("justification", rule.reason());
  }

  @DisplayName(
      "Given two equal MessageTransformations, when compared, then they are equal by value")
  @Test
  void expectMessageTransformationValueEquality() {
    final var a = new MessageTransformation("error", "warning", "l", "r", "i");
    final var b = new MessageTransformation("error", "warning", "l", "r", "i");
    Assertions.assertEquals(a, b);
    Assertions.assertEquals(a.hashCode(), b.hashCode());
  }
}
