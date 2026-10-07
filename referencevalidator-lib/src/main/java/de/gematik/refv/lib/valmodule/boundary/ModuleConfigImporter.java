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

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Centralizes the import of a Validation Module's YAML Configuration and the import of dependencies
 */
public final class ModuleConfigImporter {
  private final YAMLMapper yamlMapper;

  /**
   * Constructs the class with the provided YAML Wrapper.
   *
   * @param mapper a YAML mapper for parsing the configuration
   */
  public ModuleConfigImporter(@NonNull YAMLMapper mapper) {
    this.yamlMapper = Objects.requireNonNull(mapper, "The YAML Mapper cannot be null");
  }

  /**
   * Imports a Validation Module Configuration from an input stream. The caller is responsible for
   * defining the stream.
   *
   * @param inputStream an opened input stream to a YAML file containing the module configuration.
   * @return an optional found instance of {@link ValidationModuleManifest} or nothing, in case of a
   *     null stream
   * @throws IOException in case of errors
   */
  public @NonNull Optional<ValidationModuleManifest> importFromStream(
      @Nullable InputStream inputStream) throws IOException {
    if (Objects.isNull(inputStream)) {
      return Optional.empty();
    }

    return Optional.of(yamlMapper.readValue(inputStream, ValidationModuleManifest.class));
  }
}
