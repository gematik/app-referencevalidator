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
package de.gematik.refv.test.cli.commands.boundary;

import de.gematik.refv.cli.commands.boundary.ConfigCommand;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class ConfigCommandTest {

  @TempDir Path tempDir;

  @DisplayName(
      "Given no generate/validate flag, when executing, then the command fails with exit code 1")
  @Test
  void expectMissingFlagReturnsError() {
    final var configPath = tempDir.resolve("config.yaml").toString();
    final int exitCode = new CommandLine(new ConfigCommand()).execute("--config", configPath);
    Assertions.assertEquals(1, exitCode);
  }

  @DisplayName(
      "Given the generate flag, when executing, then a configuration is generated and exit code is 0")
  @Test
  void expectGenerateConfigurationSucceeds() {
    final var configPath = tempDir.resolve("generated.yaml").toString();
    final int exitCode =
        new CommandLine(new ConfigCommand())
            .execute("--config", configPath, "--fhir-version", "R4", "--generate");
    Assertions.assertEquals(0, exitCode);
    Assertions.assertTrue(Files.exists(Path.of(configPath)));
  }

  @DisplayName(
      "Given the generate flag without a FHIR version, when executing, then the command fails")
  @Test
  void expectGenerateWithoutFhirVersionFails() {
    final var configPath = tempDir.resolve("generated.yaml").toString();
    final int exitCode =
        new CommandLine(new ConfigCommand()).execute("--config", configPath, "--generate");
    Assertions.assertEquals(1, exitCode);
  }

  @DisplayName("Given the validate flag on a valid config, when executing, then exit code is 0")
  @Test
  void expectValidateValidConfigurationSucceeds() {
    // Given a generated valid configuration
    final var configPath = tempDir.resolve("valid.yaml");
    new CommandLine(new ConfigCommand())
        .execute("--config", configPath.toString(), "--fhir-version", "R4", "--generate");

    // When validating it
    final int exitCode =
        new CommandLine(new ConfigCommand())
            .execute("--config", configPath.toString(), "--validate");

    // Then it succeeds
    Assertions.assertEquals(0, exitCode);
  }

  @DisplayName(
      "Given the validate flag on a missing config, when executing, then the command fails")
  @Test
  void expectValidateMissingConfigurationFails() {
    final var configPath = tempDir.resolve("missing.yaml").toString();
    final int exitCode =
        new CommandLine(new ConfigCommand()).execute("--config", configPath, "--validate");
    Assertions.assertEquals(1, exitCode);
  }
}
