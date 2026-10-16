/*-
 * #%L
 * Validation Module API
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
package de.gematik.refv.lib.valmodule.entity;

import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Contains the loaded configuration and the path to the module itself, containing the packages to
 * be loaded.
 *
 * @param configuration the configuration of the Validation Module, as defined in the (typically
 *     embedded) YAML file
 * @param modulePath the path to the validation module itself
 */
public record ValidationModule(
    @NonNull ValidationModuleManifest configuration, @NonNull Path modulePath) {

  public ValidationModule {
    Objects.requireNonNull(configuration, "The module configuration cannot be null");
    Objects.requireNonNull(modulePath, "The module path cannot be null");
  }
}
