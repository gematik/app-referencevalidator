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
package de.gematik.refv.test.cli.report.entity;

import de.gematik.refv.cli.report.entity.ReportConfiguration;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportConfigurationTest {

  @DisplayName(
      "Given the default configuration, when created, then JSON format and report.json are used")
  @Test
  void expectDefaultConfiguration() {
    final var config = ReportConfiguration.defaultConfiguration();
    Assertions.assertEquals(Path.of("./report.json"), config.filePath());
    Assertions.assertEquals(ReportConfiguration.Format.JSON, config.format());
  }

  @DisplayName("Given a null file path, when created, then a NullPointerException is thrown")
  @Test
  void expectNullFilePathThrows() {
    Assertions.assertThrows(
        NullPointerException.class,
        () -> new ReportConfiguration(null, ReportConfiguration.Format.JSON));
  }

  @DisplayName("Given a null format, when created, then a NullPointerException is thrown")
  @Test
  void expectNullFormatThrows() {
    final Path reportPath = Path.of("r.json");
    Assertions.assertThrows(
        NullPointerException.class, () -> new ReportConfiguration(reportPath, null));
  }

  @DisplayName("Given the Format enum, when listing values, then HTML and JSON exist")
  @Test
  void expectFormatValues() {
    Assertions.assertEquals(2, ReportConfiguration.Format.values().length);
    Assertions.assertNotNull(ReportConfiguration.Format.valueOf("HTML"));
    Assertions.assertNotNull(ReportConfiguration.Format.valueOf("JSON"));
  }
}
