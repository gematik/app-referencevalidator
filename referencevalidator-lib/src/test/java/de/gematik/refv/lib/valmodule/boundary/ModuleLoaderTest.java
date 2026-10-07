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
package de.gematik.refv.lib.valmodule.boundary;

import de.gematik.refv.lib.exceptions.LoadModuleException;
import de.gematik.refv.lib.valmodule.control.JarValidationModuleLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModuleLoaderTest {

  @TempDir Path modulesDir;

  @DisplayName(
      "Given the factory, when creating a loader, then a JarValidationModuleLoader is returned")
  @Test
  void expectFactoryCreatesJarLoader() {
    Assertions.assertInstanceOf(JarValidationModuleLoader.class, ModuleLoader.defaultLoader());
  }

  @DisplayName(
      "Given a non-existing modules directory, when loading, then a LoadModuleException is thrown")
  @Test
  void expectNonExistingDirectoryThrows() {
    final var loader = ModuleLoader.defaultLoader();
    final var missing = modulesDir.resolve("does-not-exist");
    Assertions.assertThrows(LoadModuleException.class, () -> loader.forValidation(missing, "erp"));
  }

  @DisplayName(
      "Given a directory without a matching module, when loading, then a LoadModuleException is thrown")
  @Test
  void expectNoMatchingModuleThrows() {
    final var loader = ModuleLoader.defaultLoader();
    Assertions.assertThrows(
        LoadModuleException.class, () -> loader.forValidation(modulesDir, "erp"));
  }

  @DisplayName("Given a null directory, when loading, then a NullPointerException is thrown")
  @Test
  void expectNullDirectoryThrows() {
    final var loader = ModuleLoader.defaultLoader();
    Assertions.assertThrows(NullPointerException.class, () -> loader.forValidation(null, "erp"));
  }

  @DisplayName(
      "Given a null module configuration, when loading, then a NullPointerException is thrown")
  @Test
  void expectNullModuleConfigurationThrows() {
    final var loader = ModuleLoader.defaultLoader();
    Assertions.assertThrows(NullPointerException.class, () -> loader.forValidation(null, null));
  }

  @DisplayName(
      "Given a valid module configuration, when loading a missing module, then a LoadModuleException is thrown")
  @Test
  void expectLoadByModuleConfigurationDelegates() throws Exception {
    Files.createDirectory(modulesDir.resolve("sub"));
    final var loader = ModuleLoader.defaultLoader();
    Assertions.assertThrows(
        LoadModuleException.class, () -> loader.forValidation(modulesDir, "erp"));
  }
}
