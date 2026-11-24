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

class MessageIdTest {

  @DisplayName("Given the message ids, when reading the code, then it carries the REFV prefix")
  @Test
  void expectCodesHaveRefvPrefix() {
    for (MessageId id : MessageId.values()) {
      Assertions.assertTrue(id.getCode().startsWith("REFV-"), id.name() + " misses REFV- prefix");
    }
  }

  @DisplayName("Given specific message ids, when reading the code, then the exact code is returned")
  @Test
  void expectSpecificCodes() {
    Assertions.assertEquals("REFV-000", MessageId.NO_ERROR.getCode());
    Assertions.assertEquals("REFV-002", MessageId.VALIDATION_ERROR.getCode());
    Assertions.assertEquals("REFV-014", MessageId.SNAPSHOT_GENERATION_ERROR.getCode());
  }
}
