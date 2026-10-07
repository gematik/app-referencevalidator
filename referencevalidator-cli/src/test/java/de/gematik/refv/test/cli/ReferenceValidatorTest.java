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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class ReferenceValidatorTest {

  @DisplayName("Given no subcommand, when executing, then usage is printed and exit code is 0")
  @Test
  void expectNoSubcommandReturnsZero() {
    final int exitCode = new CommandLine(new ReferenceValidator()).execute();
    Assertions.assertEquals(0, exitCode);
  }

  @DisplayName("Given the help flag, when executing, then exit code is 0")
  @Test
  void expectHelpFlagReturnsZero() {
    final int exitCode = new CommandLine(new ReferenceValidator()).execute("--help");
    Assertions.assertEquals(0, exitCode);
  }

  @DisplayName("Given the version flag, when executing, then exit code is 0")
  @Test
  void expectVersionFlagReturnsZero() {
    final int exitCode = new CommandLine(new ReferenceValidator()).execute("--version");
    Assertions.assertEquals(0, exitCode);
  }

  @DisplayName("Given an unknown option, when executing, then a non-zero exit code is returned")
  @Test
  void expectUnknownOptionReturnsError() {
    final int exitCode = new CommandLine(new ReferenceValidator()).execute("--no-such-option");
    Assertions.assertNotEquals(0, exitCode);
  }
}
