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
package de.gematik.refv.test.cli.config.entity;

import de.gematik.refv.cli.config.entity.SnapshotCliConfig;
import de.gematik.refv.cli.config.entity.ValidationCliConfig;
import de.gematik.refv.cli.config.entity.ValidationModuleConfiguration;
import de.gematik.refv.cli.report.entity.ReportConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CliConfigRecordsTest {

  @DisplayName("Given a SnapshotCliConfig, when created, then context and report are returned")
  @Test
  void expectSnapshotCliConfigAccessors() {
    final var config =
        new SnapshotCliConfig(
            ContextConfiguration.defaultConfiguration(),
            ReportConfiguration.defaultConfiguration());
    Assertions.assertNotNull(config.context());
    Assertions.assertNotNull(config.report());
  }

  @DisplayName(
      "Given a SnapshotCliConfig with null context, when created, then a NullPointerException is thrown")
  @Test
  void expectSnapshotCliConfigNullContextThrows() {
    final var report = ReportConfiguration.defaultConfiguration();
    Assertions.assertThrows(NullPointerException.class, () -> new SnapshotCliConfig(null, report));
  }

  @DisplayName(
      "Given a SnapshotCliConfig with null report, when created, then a NullPointerException is thrown")
  @Test
  void expectSnapshotCliConfigNullReportThrows() {
    final var context = ContextConfiguration.defaultConfiguration();
    Assertions.assertThrows(NullPointerException.class, () -> new SnapshotCliConfig(context, null));
  }

  @DisplayName("Given a ValidationCliConfig, when created, then all accessors return the values")
  @Test
  void expectValidationCliConfigAccessors() {
    final var config =
        new ValidationCliConfig(
            ContextConfiguration.defaultConfiguration(),
            new ValidationModuleConfiguration(Path.of("modules/"), "erp"),
            ValidationOptions.defaultConfiguration(),
            ReportConfiguration.defaultConfiguration());
    Assertions.assertNotNull(config.context());
    Assertions.assertEquals("erp", config.module().name());
    Assertions.assertNotNull(config.validationOptions());
    Assertions.assertNotNull(config.report());
  }

  @DisplayName(
      "Given a ValidationCliConfig with a null field, when created, then a NullPointerException is thrown")
  @Test
  void expectValidationCliConfigNullFieldThrows() {
    final var context = ContextConfiguration.defaultConfiguration();
    final var module = new ValidationModuleConfiguration(Path.of("modules/"), "erp");
    final var options = ValidationOptions.defaultConfiguration();
    final var report = ReportConfiguration.defaultConfiguration();

    Assertions.assertThrows(
        NullPointerException.class, () -> new ValidationCliConfig(null, module, options, report));
    Assertions.assertDoesNotThrow(() -> new ValidationCliConfig(context, null, options, report));
    Assertions.assertThrows(
        NullPointerException.class, () -> new ValidationCliConfig(context, module, null, report));
    Assertions.assertThrows(
        NullPointerException.class, () -> new ValidationCliConfig(context, module, options, null));
  }
}
