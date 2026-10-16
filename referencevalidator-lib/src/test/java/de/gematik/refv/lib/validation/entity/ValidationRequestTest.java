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
package de.gematik.refv.lib.validation.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidationRequestTest {

  @DisplayName(
      "Given a resource, when created with the single-argument constructor, then default options are used")
  @Test
  void expectSingleArgumentConstructorUsesDefaults() {
    final var resource = FhirResource.fromJson("{\"resourceType\":\"Patient\"}");
    final var request = new ValidationRequest(resource);
    Assertions.assertEquals(resource, request.resource());
  }

  @DisplayName("Given a null resource, when created, then a NullPointerException is thrown")
  @Test
  void expectNullResourceThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new ValidationRequest(null));
  }
}
