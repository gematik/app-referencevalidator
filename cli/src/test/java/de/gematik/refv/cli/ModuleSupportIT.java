/*-
 * #%L
 * referencevalidator-cli
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
package de.gematik.refv.cli;

import de.gematik.refv.cli.support.TestAppender;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModuleSupportIT {

  private static final Logger ROOT_LOGGER = (Logger) LogManager.getRootLogger();
  private static final Logger REFV_LOGGER = (Logger) LogManager.getLogger("de.gematik.refv");
  private static final TestAppender APPENDER = new TestAppender();

  @BeforeAll
  static void beforeAll() {
    APPENDER.start();
    ROOT_LOGGER.addAppender(APPENDER);
    REFV_LOGGER.addAppender(APPENDER);
  }

  @AfterAll
  static void afterAll() {
    ROOT_LOGGER.removeAppender(APPENDER);
    REFV_LOGGER.removeAppender(APPENDER);
    APPENDER.stop();
  }

  @SneakyThrows
  @ParameterizedTest
  @ValueSource(
      strings = {
        "erp src/test/resources/erp-test.xml",
        "eau src/test/resources/eau-test.xml",
        "core src/test/resources/core-test.xml --profile http://fhir.org/StructureDefinition/Parameters",
        "erpta7 src/test/resources/erpta7-test.xml"
      })
  @Execution(ExecutionMode.CONCURRENT)
  void testValidCliInput(String input) {
    int startIndex = APPENDER.size();
    String invocationThreadName = Thread.currentThread().getName();

    String[] args = input.split(" ");
    ReferenceValidator.main(args);

    boolean isValid = false;
    for (LogEvent event : APPENDER.getLogsFrom(startIndex)) {
      if (!invocationThreadName.equals(event.getThreadName())) {
        continue;
      }
      if (event.getMessage().getFormattedMessage().contains("Valid: true")) {
        isValid = true;
        break;
      }
    }
    Assertions.assertTrue(isValid);
  }
}
