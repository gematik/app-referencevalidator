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

import de.gematik.refv.lib.config_parser.boundary.YAMLMapperProvider;
import de.gematik.refv.lib.exceptions.LoadModuleException;
import de.gematik.refv.lib.valmodule.control.JarValidationModuleLoader;
import de.gematik.refv.lib.valmodule.entity.ValidationModule;
import de.gematik.refv.valmodule.api.boundary.FhirValidationModule;
import java.nio.file.Path;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Interface, defining the method for returning the Validation Module meta information. Generally, a
 * Validation Module should declare a YAML file at the path defined in {@link #MANIFEST_PATH}, a
 * definition specified at {@link #MODULE_DEFINITION_PATH}.
 */
public interface ModuleLoader {
  String MANIFEST_PATH = "META-INF/config.yaml";
  String PACKAGE_PATH = "package/";
  String MODULE_DEFINITION_PATH =
      "META-INF/services/" + FhirValidationModule.class.getCanonicalName();

  /**
   * Attempts to load a {@link ValidationModule} instance for validation, which corresponds to the
   * name of the module being searched (e.g. {@code erezept, isik}).
   *
   * @param directory the directory where the validation modules are stored
   * @param name the preferred validation module to be used for validation
   * @return an optional {@link ValidationModule} instance
   * @throws LoadModuleException in case of errors while loading the module
   */
  default @NonNull Optional<ValidationModule> forValidation(
      @NonNull Path directory, @NonNull String name) throws LoadModuleException {
    throw new LoadModuleException("Not implemented");
  }

  /**
   * Attempts to load a {@link ValidationModule} instance for snapshot generation, given the path to
   * the module where a YAML Manifest is stored.
   *
   * @param modulePath the path to the module containing a valid manifest.
   * @return an optional {@link ValidationModule} instance
   * @throws LoadModuleException in case of errors while loading the module
   */
  default @NonNull Optional<ValidationModule> forSnapshotGeneration(@NonNull Path modulePath)
      throws LoadModuleException {
    throw new LoadModuleException("Not implemented");
  }

  /**
   * Creates a Loader for importing the information from JAR files.
   *
   * @return a {@link ModuleLoader} implementation for accessing the JAR files
   */
  static ModuleLoader defaultLoader() {
    return new JarValidationModuleLoader(YAMLMapperProvider.getMapper());
  }
}
