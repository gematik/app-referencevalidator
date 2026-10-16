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

import de.gematik.refv.cli.BaseCommand;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseCommandTest {

  /** Minimal concrete command to exercise the base behavior. */
  private static class TestCommand extends BaseCommand {
    @Override
    public Integer call() throws Exception {
      return super.call();
    }

    @Override
    public String showInfo() {
      return super.showInfo();
    }
  }

  @DisplayName("Given a command, when calling it, then zero is returned and info is shown")
  @Test
  void expectCallReturnsZero() {
    final var command = new TestCommand();
    final var exitCode = Assertions.assertDoesNotThrow(command::call);
    Assertions.assertEquals(0, exitCode);
  }

  @DisplayName("Given a command, when showing info, then version, java and locale are included")
  @Test
  void expectShowInfoContainsSystemDetails() {
    final var command = new TestCommand();
    final String info = command.showInfo();
    Assertions.assertTrue(info.contains("gematik Reference Validator"));
    Assertions.assertTrue(info.contains("Java:"));
    Assertions.assertTrue(info.contains("Locale:"));
    Assertions.assertTrue(info.contains("Timezone:"));
  }
}
