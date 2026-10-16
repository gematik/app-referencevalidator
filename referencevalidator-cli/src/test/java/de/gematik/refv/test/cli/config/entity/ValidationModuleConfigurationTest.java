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
package de.gematik.refv.test.cli.config.entity;

import de.gematik.refv.cli.config.entity.ValidationModuleConfiguration;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValidationModuleConfigurationTest {

  @TempDir private Path tempDir;

  @DisplayName("Given a directory and a name, when created, then the accessors return the values")
  @Test
  void expectCreationWorks() {
    final var modulesDir = tempDir.resolve("modules");
    final var config = new ValidationModuleConfiguration(modulesDir, "erp");
    Assertions.assertEquals(modulesDir, config.directory());
    Assertions.assertEquals("erp", config.name());
  }

  @DisplayName("Given a null directory, when created, then a NullPointerException is thrown")
  @Test
  void expectNullDirectoryThrows() {
    Assertions.assertThrows(
        NullPointerException.class, () -> new ValidationModuleConfiguration(null, "erp"));
  }

  @DisplayName("Given a null name, when created, then a NullPointerException is thrown")
  @Test
  void expectNullNameThrows() {
    final Path modulesDirectory = Path.of("modules/");
    Assertions.assertThrows(
        NullPointerException.class,
        () -> new ValidationModuleConfiguration(modulesDirectory, null));
  }
}
