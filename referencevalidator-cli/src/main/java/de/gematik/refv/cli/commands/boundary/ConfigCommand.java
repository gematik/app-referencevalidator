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
package de.gematik.refv.cli.commands.boundary;

import de.gematik.refv.cli.BaseCommand;
import de.gematik.refv.cli.commands.VersionProvider;
import de.gematik.refv.cli.config.boundary.ConfigGenerator;
import de.gematik.refv.cli.config.boundary.ConfigLoader;
import de.gematik.refv.cli.config.entity.CliConfig;
import de.gematik.refv.cli.report.boundary.ConfigurationReporter;
import de.gematik.refv.lib.exceptions.ConfigurationException;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(
    name = ConfigCommand.COMMAND_NAME,
    mixinStandardHelpOptions = true,
    version = VersionProvider.PROJECT_VERSION,
    description =
        """

                        Allows to validate an existing configuration file or generate a new one to be used for snapshot generation or for validation of FHIR resources.
                        """)
public class ConfigCommand extends BaseCommand {
  private static final Logger log = LoggerFactory.getLogger(ConfigCommand.class);
  public static final String COMMAND_NAME = "configuration";

  @CommandLine.Option(
      names = {"--config"},
      description =
          "Path to YAML configuration file.\nIn combination with the flag '--generate', the path is used as destination for writing a new configuration",
      required = true)
  private String configPath;

  @CommandLine.Option(
      names = {"--fhir-version"},
      description = "FHIR Version to validate against (e.g. R4, R5, etc. - Defaults to R4)",
      required = false)
  private String fhirVersion;

  @CommandLine.Option(
      names = {"--generate"},
      description =
          "With this flag, the generation of a new configuration will be performed. The result will be stored at the given configuration path",
      required = false)
  private boolean shouldGenerateConfiguration;

  @CommandLine.Option(
      names = {"--validate"},
      description = "With this flag, the given configuration will be validated and printed out",
      required = false)
  private boolean shouldValidateConfiguration;

  @CommandLine.Option(
      names = {"--for-snapshot"},
      description =
          "With this flag, the given configuration will be interpreted as a configuration for the snapshot generator",
      required = false,
      defaultValue = "false")
  private boolean shouldUseSnapshotCli;

  public ConfigCommand() {
    super();
  }

  @Override
  public Integer call() {
    try {
      super.call();
      if (!shouldGenerateConfiguration && !shouldValidateConfiguration) {
        throw new IllegalArgumentException(
            "You need to specify the '--generate' or the '--validate' flag");
      }

      final CliConfig cliConfig;
      if (shouldGenerateConfiguration) {
        cliConfig = generateConfiguration(fhirVersion, Path.of(configPath), shouldUseSnapshotCli);
        log.info("Configuration has been generated successfully at {}", configPath);
      } else {
        cliConfig = validateConfiguration(Path.of(configPath), shouldUseSnapshotCli);
        log.info("Configuration at {} has been validated successfully", configPath);
      }

      final ConfigurationReporter configurationReporter = new ConfigurationReporter();
      configurationReporter.printConfiguration(cliConfig);

      return 0;
    } catch (Exception e) {
      log.error("Failed with the configuration: {}", e.getMessage());
      log.debug("Stack trace:", e);
      return 1;
    }
  }

  private @NonNull CliConfig generateConfiguration(
      @NonNull String fhirVersion, @NonNull Path outputPath, boolean forSnapshot)
      throws ConfigurationException {
    Objects.requireNonNull(
        fhirVersion, "Please specify the FHIR Version with the flag '--fhir-version'");
    Objects.requireNonNull(
        outputPath, "Please specify a valid output path with the flag '--config'");

    log.info("Generating a new configuration for the CLI and storing it in {}", outputPath);
    if (forSnapshot) {
      return ConfigGenerator.generateNewEmptySnapshotConfiguration(fhirVersion, outputPath);
    }

    return ConfigGenerator.generateNewEmptyValidationConfiguration(fhirVersion, outputPath);
  }

  private @NonNull CliConfig validateConfiguration(
      @NonNull Path configurationPath, boolean forSnapshot) {
    Objects.requireNonNull(
        configurationPath, "Please specify a valid configuration path with the flag '--config'");

    ConfigLoader configLoader = new ConfigLoader();
    if (forSnapshot) {
      log.info("Validating snapshot generator configuration from {}", configurationPath);
      return configLoader.loadSnapshotConfigFromPath(configurationPath);
    }
    log.info("Validating validator configuration from {}", configurationPath);
    return configLoader.loadValidationConfigFromPath(configurationPath);
  }
}
