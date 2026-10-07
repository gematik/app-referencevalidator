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

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.gematik.refv.lib.exceptions.LoadModuleException;
import de.gematik.refv.lib.valmodule.boundary.ModuleConfigImporter;
import de.gematik.refv.lib.valmodule.boundary.ModuleLoader;
import de.gematik.refv.lib.valmodule.entity.ValidationModule;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/** Implements the logic for loading a Module Manifest for Snapshot Generation. */
public final class SnapshotValidationModuleLoader implements ModuleLoader {
  private final ModuleConfigImporter moduleConfigImporter;

  /**
   * Constructs the class with the provided YAML Wrapper.
   *
   * @param mapper a YAML mapper for parsing the configuration
   */
  public SnapshotValidationModuleLoader(@NonNull YAMLMapper mapper) {
    this.moduleConfigImporter = new ModuleConfigImporter(mapper);
  }

  /**
   * Attempts to load a {@link ValidationModule} instance for snapshot generation, given the path to
   * the module where a YAML Manifest is stored.
   *
   * @param modulePath the path to the module containing a valid manifest.
   * @return an optional {@link ValidationModule} instance
   * @throws LoadModuleException in case of errors while loading the module
   */
  @Override
  public @NonNull Optional<ValidationModule> forSnapshotGeneration(@NonNull Path modulePath)
      throws LoadModuleException {
    Objects.requireNonNull(modulePath, "The 'modulePath' must not be null");
    final var manifestPath = modulePath.resolve(ModuleLoader.MANIFEST_PATH);
    try (var is = new FileInputStream(manifestPath.toFile())) {
      final var moduleConfig = moduleConfigImporter.importFromStream(is);
      if (moduleConfig.isEmpty()) {
        throw new LoadModuleException("Failed to parse the module configuration from file");
      }

      return Optional.of(new ValidationModule(moduleConfig.get(), modulePath));
    } catch (IOException e) {
      throw new LoadModuleException("Failed to read manifest from file", e);
    }
  }
}
