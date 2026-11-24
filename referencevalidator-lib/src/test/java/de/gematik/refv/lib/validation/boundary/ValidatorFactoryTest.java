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
package de.gematik.refv.lib.validation.boundary;

import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidatorFactoryTest {

  @DisplayName("Creates a Validator from an existing initialized context")
  @Test
  void expectCreationOfValidatorFromCoreContextWorks() {
    // Given a Context Provider
    final var contextProvider = ContextProvider.defaultProvider();
    // When a context with the default configuration is generated
    final var coreContext =
        Assertions.assertDoesNotThrow(
            () -> contextProvider.validationContext(ContextConfiguration.defaultConfiguration()));
    // Then the context is valid
    Assertions.assertNotNull(coreContext);
    // and When a Validator is created
    final var validator = Assertions.assertDoesNotThrow(() -> coreContext.cloneContext(List.of()));
    // Then the validator is not null
    Assertions.assertNotNull(validator);
  }
}
