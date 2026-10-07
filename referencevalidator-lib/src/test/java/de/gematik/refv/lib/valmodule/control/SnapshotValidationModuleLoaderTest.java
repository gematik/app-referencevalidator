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
package de.gematik.refv.lib.valmodule.control;

import de.gematik.refv.lib.config_parser.boundary.YAMLMapperProvider;
import de.gematik.refv.lib.exceptions.LoadModuleException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnapshotValidationModuleLoaderTest {

  private final SnapshotValidationModuleLoader loader =
      new SnapshotValidationModuleLoader(YAMLMapperProvider.getMapper());

  @DisplayName("Expect that the load does not implement the method 'forValidation'")
  @Test
  void expectMethodNotImplemented() {
    final Path notExisting = Path.of("notExisting");
    Assertions.assertThrows(
        LoadModuleException.class, () -> loader.forValidation(notExisting, "notExisting"));
  }

  @DisplayName("Expect that a valid configuration can be imported")
  @Test
  void expectConfigurationImported() {
    // Given the path to a valid module directory, containing a valid manifest file
    final var modulePath = Path.of("src/test/resources/modules/isik/");
    final var module =
        Assertions.assertDoesNotThrow(() -> loader.forSnapshotGeneration(modulePath));
    Assertions.assertTrue(module.isPresent());
  }
}
