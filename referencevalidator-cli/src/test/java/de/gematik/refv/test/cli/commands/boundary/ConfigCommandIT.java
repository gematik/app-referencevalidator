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

import de.gematik.refv.cli.ReferenceValidator;
import de.gematik.refv.cli.commands.boundary.ConfigCommand;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class ConfigCommandIT {
  @DisplayName("Generation and Validation of Configuration works without issues")
  @Test
  void expectConfigurationCommandSuccessful() {
    // Given a new configuration file to be created
    final var tempConfigOutputPath =
        Assertions.assertDoesNotThrow(() -> Files.createTempFile("refv-temp-config", ".yaml"));
    tempConfigOutputPath.toFile().deleteOnExit();
    // When the command to generate it is executed
    final String inputArgs =
        ConfigCommand.COMMAND_NAME
            + " --generate --fhir-version R4 --config "
            + tempConfigOutputPath;
    final String[] args = inputArgs.split(" ");
    int exitStatus =
        Assertions.assertDoesNotThrow(
            () -> new CommandLine(new ReferenceValidator()).execute(args));
    // Then the result works as expected
    Assertions.assertEquals(0, exitStatus);
    Assertions.assertTrue(tempConfigOutputPath.toFile().length() > 0);
    final var fileContent =
        Assertions.assertDoesNotThrow(
            () -> Files.readString(tempConfigOutputPath, StandardCharsets.UTF_8));
    Assertions.assertFalse(fileContent.isBlank());
    // And When this output is back parsed
    // When the command to generate it is executed
    final String newInputArgs =
        ConfigCommand.COMMAND_NAME + " --debug --validate --config " + tempConfigOutputPath;
    final String[] newArgs = newInputArgs.split(" ");
    exitStatus =
        Assertions.assertDoesNotThrow(
            () -> new CommandLine(new ReferenceValidator()).execute(newArgs));
    // Then the result works as expected
    Assertions.assertEquals(0, exitStatus);
  }

  @DisplayName("Validation of a wrong Configuration produces error")
  @Test
  void expectConfigurationCommandReturnsErrorWithBrokenConfiguration() {
    // When
    final String newInputArgs =
        ConfigCommand.COMMAND_NAME
            + " --validate --config "
            + Path.of("src/test/resources/config/config.invalid.missing-required-settings.yaml");
    final String[] newArgs = newInputArgs.split(" ");
    final int exitStatus =
        Assertions.assertDoesNotThrow(
            () -> new CommandLine(new ReferenceValidator()).execute(newArgs));
    // Then the program exists in error
    Assertions.assertEquals(1, exitStatus);
  }
}
