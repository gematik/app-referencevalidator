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
package de.gematik.refv.cli.config.entity;

import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Defines the location of a validation module, used for loading it.
 *
 * @param directory the directory where the validation modules are stored
 * @param name the preferred validation module to be used for validation
 */
public record ValidationModuleConfiguration(@NonNull Path directory, @NonNull String name) {
  public ValidationModuleConfiguration {
    Objects.requireNonNull(directory, "The Modules Directory has not been set");
    Objects.requireNonNull(name, "The Validation Module name has not been set");
  }
}
