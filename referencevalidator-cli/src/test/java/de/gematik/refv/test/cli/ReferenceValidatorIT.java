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
package de.gematik.refv.test.cli;

import de.gematik.refv.cli.ReferenceValidator;
import de.gematik.refv.cli.commands.boundary.ConfigCommand;
import de.gematik.refv.cli.commands.boundary.FhirValidatorCommand;
import de.gematik.refv.cli.commands.boundary.SnapshotGeneratorCommand;
import de.gematik.refv.test.cli.helpers.TestAppender;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

class ReferenceValidatorIT {
  private TestAppender appender;

  @BeforeEach
  void beforeEach() {
    appender = TestAppender.createAppender("TestAppender");
    appender.start();
    org.apache.logging.log4j.core.Logger rootLogger =
        (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
    rootLogger.setLevel(Level.ALL);
    rootLogger.addAppender(appender);
  }

  @AfterEach
  void afterEach() {
    org.apache.logging.log4j.core.Logger rootLogger =
        (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
    rootLogger.removeAppender(appender);
  }

  @DisplayName("Tests that help and version arguments work")
  @ParameterizedTest
  @MethodSource("provideInputArguments")
  void expectGeneralHelpAndVersionArgumentsWork(String inputArgs) {
    String[] args = inputArgs.split(" ");

    int exitStatus =
        Assertions.assertDoesNotThrow(
            () -> new CommandLine(new ReferenceValidator()).execute(args));
    Assertions.assertEquals(0, exitStatus);
    Assertions.assertTrue(appender.getLogsFromCurrentThread().isEmpty(), "No log entries expected");
  }

  private static Stream<Arguments> provideInputArguments() {
    return Stream.of(
        Arguments.of("--help"),
        Arguments.of("--version"),
        Arguments.of(ConfigCommand.COMMAND_NAME + "configuration --help"),
        Arguments.of(ConfigCommand.COMMAND_NAME + " --version"),
        Arguments.of(SnapshotGeneratorCommand.COMMAND_NAME + " --help"),
        Arguments.of(SnapshotGeneratorCommand.COMMAND_NAME + " --version"),
        Arguments.of(FhirValidatorCommand.COMMAND_NAME + " --help"),
        Arguments.of(FhirValidatorCommand.COMMAND_NAME + " --version"));
  }
}
