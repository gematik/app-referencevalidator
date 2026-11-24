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

import de.gematik.refv.cli.config.entity.SnapshotCliConfig;
import de.gematik.refv.cli.config.entity.SnapshotModuleConfiguration;
import de.gematik.refv.cli.config.entity.ValidationCliConfig;
import de.gematik.refv.cli.config.entity.ValidationModuleConfiguration;
import de.gematik.refv.cli.report.entity.ReportConfiguration;
import de.gematik.refv.lib.config_parser.boundary.YAMLMapperProvider;
import de.gematik.refv.lib.exceptions.ConfigurationException;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;

public final class ConfigGenerator {
  private ConfigGenerator() {}

  public static @NonNull SnapshotCliConfig generateNewEmptySnapshotConfiguration(
      @NonNull String fhirVersion, @NonNull Path outputPath) throws ConfigurationException {
    final SnapshotCliConfig cliConfig =
        new SnapshotCliConfig(
            ContextConfiguration.defaultConfiguration(FhirRelease.ofVersion(fhirVersion)),
            new SnapshotModuleConfiguration(
                Path.of("modules/my-module/config.yaml"),
                Path.of("my-packages-dir/"),
                Path.of("my-patches-dir/")),
            ReportConfiguration.defaultConfiguration());

    writeConfigOnDisk(outputPath, cliConfig);
    return cliConfig;
  }

  public static @NonNull ValidationCliConfig generateNewEmptyValidationConfiguration(
      @NonNull String fhirVersion, @NonNull Path outputPath) throws ConfigurationException {
    final ValidationCliConfig cliConfig =
        new ValidationCliConfig(
            ContextConfiguration.defaultConfiguration(FhirRelease.ofVersion(fhirVersion)),
            new ValidationModuleConfiguration(Path.of("modules/"), "example-module"),
            ValidationOptions.defaultConfiguration(),
            ReportConfiguration.defaultConfiguration());

    writeConfigOnDisk(outputPath, cliConfig);
    return cliConfig;
  }

  private static <T> void writeConfigOnDisk(@NonNull Path outputPath, T cliConfig) {
    try {
      if (Files.exists(outputPath)) {
        Files.deleteIfExists(outputPath);
      }
    } catch (IOException _) {
      throw new ConfigurationException("Failed to delete existing configuration at " + outputPath);
    }
    try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
      YAMLMapperProvider.getMapper().writeValue(outputStream, cliConfig);
    } catch (IOException e) {
      throw new ConfigurationException("Could not write the config file: " + outputPath, e);
    }
  }
}
