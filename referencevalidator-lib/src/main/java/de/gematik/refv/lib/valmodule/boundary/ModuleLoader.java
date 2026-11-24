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

/// Module loading returns metadata only. Package import belongs to Package Resolver.
public interface ModuleLoader {
  String MANIFEST_PATH = "META-INF/config.yaml";
  String PACKAGE_PATH = "package/";
  String MODULE_DEFINITION_PATH =
      "META-INF/services/" + FhirValidationModule.class.getCanonicalName();

  /// Returns the first valid JAR whose manifest name matches case-insensitively, or an empty result
  /// when no match exists. Invalid candidates are skipped with diagnostics. An invalid or
  /// unreadable directory is a loading failure, not an absent module.
  default @NonNull Optional<ValidationModule> forValidation(
      @NonNull Path directory, @NonNull String name) throws LoadModuleException {
    throw new LoadModuleException("This loader does not support validation-module search");
  }

  /// Reads `META-INF/config.yaml` below a module root, not a direct manifest-file path. A missing
  /// or invalid manifest is a loading failure. The returned module retains the supplied root.
  default @NonNull Optional<ValidationModule> forSnapshotGeneration(@NonNull Path modulePath)
      throws LoadModuleException {
    throw new LoadModuleException("This loader does not support snapshot-module loading");
  }

  /// The default loader supports validation JAR search, not snapshot-module loading.
  static ModuleLoader defaultLoader() {
    return new JarValidationModuleLoader(YAMLMapperProvider.getMapper());
  }
}
