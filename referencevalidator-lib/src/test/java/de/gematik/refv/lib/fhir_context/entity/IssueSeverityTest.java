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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssueSeverityTest {

  @DisplayName("Given the severities, when reading the code, then the FHIR code is returned")
  @Test
  void expectSeverityCodes() {
    Assertions.assertEquals("information", IssueSeverity.INFORMATION.getCode());
    Assertions.assertEquals("warning", IssueSeverity.WARNING.getCode());
    Assertions.assertEquals("error", IssueSeverity.ERROR.getCode());
    Assertions.assertEquals("fatal", IssueSeverity.FATAL.getCode());
  }

  @DisplayName("Given the severities, when counting, then all four levels exist")
  @Test
  void expectAllSeverityLevelsPresent() {
    Assertions.assertEquals(4, IssueSeverity.values().length);
  }
}
