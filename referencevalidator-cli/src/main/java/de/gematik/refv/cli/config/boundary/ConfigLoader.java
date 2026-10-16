/*-
 * #%L
 * referencevalidator-cli
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
package de.gematik.refv.cli.config.boundary;

import de.gematik.refv.cli.commands.entity.SnapshotGeneratorArguments;
import de.gematik.refv.cli.commands.entity.ValidationArguments;
import de.gematik.refv.cli.config.control.ConfigBuilder;
import de.gematik.refv.cli.config.entity.SnapshotCliConfig;
import de.gematik.refv.cli.config.entity.ValidationCliConfig;
import de.gematik.refv.lib.exceptions.ConfigurationException;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;

/** Helper to load the Configuration for the FHIR Engine. */
public final class ConfigLoader {
  /**
   * Attempts to load the configuration of the snapshot generator from a valid file path.
   *
   * @param configPath a valid file path pointing to a configuration in YAML format
   * @return an instance of {@link SnapshotCliConfig}, otherwise throws an exception in case of
   *     errors
   * @throws ConfigurationException in case of I/O Errors
   */
  public SnapshotCliConfig loadSnapshotConfigFromPath(@NonNull final Path configPath)
      throws ConfigurationException {
    return ConfigBuilder.loadSnapshotConfigFromPath(configPath);
  }

  /**
   * Attempts to load the configuration of the validator from a valid file path.
   *
   * @param configPath a valid file path pointing to a configuration in YAML format
   * @return an instance of {@link ValidationCliConfig}, otherwise throws an exception in case of
   *     errors
   * @throws ConfigurationException in case of I/O Errors
   */
  public ValidationCliConfig loadValidationConfigFromPath(@NonNull final Path configPath)
      throws ConfigurationException {
    return ConfigBuilder.loadValidationConfigFromPath(configPath);
  }

  /**
   * Attempts to load the configuration of the snapshot generator from a set of command line
   * parameters
   *
   * @return an instance of {@link SnapshotCliConfig}, otherwise throws an exception in case of
   *     errors
   */
  public SnapshotCliConfig fromSnapshotGeneratorArguments(
      @NonNull SnapshotGeneratorArguments arguments) {
    return ConfigBuilder.fromSnapshotGeneratorArguments(arguments);
  }

  /**
   * Attempts to load the configuration of the validator from a set of command line parameters
   *
   * @return an instance of {@link ValidationCliConfig}, otherwise throws an exception in case of
   *     errors
   */
  public ValidationCliConfig fromValidationArguments(@NonNull ValidationArguments arguments) {
    return ConfigBuilder.fromValidationArguments(arguments);
  }
}
