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
package de.gematik.refv.test.cli.report.boundary;

import de.gematik.refv.cli.config.entity.SnapshotCliConfig;
import de.gematik.refv.cli.config.entity.SnapshotModuleConfiguration;
import de.gematik.refv.cli.config.entity.ValidationCliConfig;
import de.gematik.refv.cli.config.entity.ValidationModuleConfiguration;
import de.gematik.refv.cli.report.boundary.ConfigurationReporter;
import de.gematik.refv.cli.report.entity.ReportConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfigurationReporterTest {

  private final ConfigurationReporter configurationReporter = new ConfigurationReporter();

  @DisplayName("Verify that the default configuration can be correctly printed out")
  @Test
  void expectPrintDefaultConfigurationSuccessful() {
    final SnapshotCliConfig cliConfig =
        new SnapshotCliConfig(
            ContextConfiguration.defaultConfiguration(),
            ReportConfiguration.defaultConfiguration());
    Assertions.assertDoesNotThrow(() -> configurationReporter.printConfiguration(cliConfig));
  }

  @DisplayName("Verify that a complete Validator configuration can be correctly printed out")
  @Test
  void expectPrintExtendedValidatorConfigurationSuccessful() {
    final ValidationCliConfig cliConfig =
        new ValidationCliConfig(
            ContextConfiguration.defaultConfiguration(),
            new ValidationModuleConfiguration(Path.of("modules/"), "test-module"),
            new ValidationOptions(
                ProfileCanonical.fromCanonical("https://gematik.de/fhir/my/ig/6.0.0/MyProfile"),
                Pattern.compile("^.*MyProfile$"),
                ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
                ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE),
            ReportConfiguration.defaultConfiguration());
    Assertions.assertDoesNotThrow(() -> configurationReporter.printConfiguration(cliConfig));
  }

  @DisplayName(
      "Verify that a complete Snapshot Generator configuration can be correctly printed out")
  @Test
  void expectPrintExtendedSnapshotGeneratorConfigurationSuccessful() {
    final SnapshotCliConfig cliConfig =
        new SnapshotCliConfig(
            ContextConfiguration.defaultConfiguration(),
            new SnapshotModuleConfiguration(
                Path.of("modules/my-module/config.yaml"),
                Path.of("modules/my-module/packages"),
                Path.of("modules/my-module/patches")),
            ReportConfiguration.defaultConfiguration());
    Assertions.assertDoesNotThrow(() -> configurationReporter.printConfiguration(cliConfig));
  }

  @DisplayName("Verify that a null configuration does not trigger an error")
  @Test
  void expectPrintDoesNotFailsOnNullConfiguration() {
    Assertions.assertDoesNotThrow(() -> configurationReporter.printConfiguration(null));
  }
}
