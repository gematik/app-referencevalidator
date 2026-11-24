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
package de.gematik.refv.test.cli.config.boundary;

import de.gematik.refv.cli.commands.entity.ContextArguments;
import de.gematik.refv.cli.commands.entity.ReportArguments;
import de.gematik.refv.cli.commands.entity.SnapshotGenerationModuleArguments;
import de.gematik.refv.cli.commands.entity.SnapshotGeneratorArguments;
import de.gematik.refv.cli.commands.entity.TerminologyArguments;
import de.gematik.refv.cli.commands.entity.ValidationArguments;
import de.gematik.refv.cli.commands.entity.ValidationModuleArguments;
import de.gematik.refv.cli.config.boundary.ConfigLoader;
import de.gematik.refv.cli.report.entity.ReportConfiguration;
import de.gematik.refv.lib.exceptions.ConfigurationException;
import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigLoaderTest {

  private final ConfigLoader configLoader = new ConfigLoader();

  @DisplayName("Expect Load of a valid Validation Configuration YAML works successfully")
  @ParameterizedTest
  @MethodSource("provideValidConfigurations")
  void expectLoadValidConfigFromPathSuccessful(String configPath) {
    final var cliConfig =
        Assertions.assertDoesNotThrow(
            () -> configLoader.loadValidationConfigFromPath(Path.of(configPath)));
    Assertions.assertNotNull(cliConfig);
    Assertions.assertNotNull(cliConfig.context());
    Assertions.assertNotNull(cliConfig.context().fhirRelease());
    Assertions.assertNotNull(cliConfig.module().directory());
    Assertions.assertFalse(cliConfig.module().directory().toString().isBlank());
    Assertions.assertNotNull(cliConfig.module().name());
    Assertions.assertFalse(cliConfig.module().name().isBlank());
    Assertions.assertTrue(
        Objects.isNull(cliConfig.validationOptions().profileToValidate())
            || !cliConfig.validationOptions().profileToValidate().value().isBlank());
    Assertions.assertNotNull(cliConfig.validationOptions().validationMessagesFilterStrategy());
    Assertions.assertNotNull(cliConfig.report().filePath());
    Assertions.assertFalse(cliConfig.report().filePath().toString().isBlank());
    Assertions.assertNotNull(cliConfig.report().format());
    Assertions.assertTrue(
        ReportConfiguration.Format.HTML.equals(cliConfig.report().format())
            || ReportConfiguration.Format.JSON.equals(cliConfig.report().format()));
  }

  @DisplayName("Expect Load of an invalid Configuration YAML produces an error")
  @ParameterizedTest
  @MethodSource("provideInvalidConfigurations")
  void expectLoadInvalidConfigFromPathGeneratesError(String configPath) {
    final Path path = Path.of(configPath);
    Assertions.assertThrows(
        ConfigurationException.class, () -> configLoader.loadSnapshotConfigFromPath(path));
  }

  @DisplayName("Expect Load of a Configuration YAML from a wrong path produces an error")
  @ParameterizedTest
  @ValueSource(strings = {"", "/wrong/file/given"})
  void expectLoadFromWrongPathError(String configPath) {
    final Path path = Path.of(configPath);
    Assertions.assertThrows(
        InitializationException.class, () -> configLoader.loadSnapshotConfigFromPath(path));
  }

  @DisplayName("Expect Load of a Configuration YAML from a null path produces an error")
  @ParameterizedTest
  @NullSource
  void expectLoadFromNullPathError(String configPath) {
    final Path path = configPath == null ? null : Path.of(configPath);
    Assertions.assertThrows(
        NullPointerException.class, () -> configLoader.loadSnapshotConfigFromPath(path));
  }

  @DisplayName("Expect that loading configurations for Validation from Arguments works as expected")
  @Test
  void expectLoadFromValidValidationCliArgumentsSuccessful() {
    final var arguments =
        new ValidationArguments(
            new ValidationModuleArguments(Path.of("modules/"), "my-module"),
            new ContextArguments(
                "R4", "en-GB", null, new TerminologyArguments(false, ""), false, false, false),
            null,
            false,
            new ReportArguments(Path.of("output/"), true));
    final var cliConfig =
        Assertions.assertDoesNotThrow(() -> configLoader.fromValidationArguments(arguments));

    Assertions.assertNotNull(cliConfig);
    Assertions.assertNotNull(cliConfig.context());
    Assertions.assertEquals(FhirRelease.asR4(), cliConfig.context().fhirRelease());
    Assertions.assertNotNull(cliConfig.module().directory());
    Assertions.assertEquals(Path.of("modules/"), cliConfig.module().directory());
    Assertions.assertNotNull(cliConfig.module().name());
    Assertions.assertTrue(cliConfig.module().name().contentEquals("my-module"));
    Assertions.assertNull(cliConfig.validationOptions().profileToValidate());
    Assertions.assertEquals(
        ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ERRORS_ONLY,
        cliConfig.validationOptions().validationMessagesFilterStrategy());
    Assertions.assertEquals(Path.of("output/"), cliConfig.report().filePath());
    Assertions.assertNotNull(cliConfig.report().format());
    Assertions.assertEquals(ReportConfiguration.Format.JSON, cliConfig.report().format());
  }

  @DisplayName(
      "Expect that loading configurations for Validation from Arguments with missing important settings produces an error")
  @Test
  void expectLoadFromIncompleteValidationCliArgumentsGeneratesError() {
    final Path modulesDirectory = Path.of("modules-directory/");
    final Path reportDirectory = Path.of("report/");
    final var invalidVersionArguments =
        new ValidationArguments(
            new ValidationModuleArguments(modulesDirectory, "my-module"),
            new ContextArguments(
                "R6", "de-DE", null, new TerminologyArguments(false, ""), false, true, false),
            null,
            false,
            new ReportArguments(reportDirectory, false));
    final var missingReportArguments =
        new ValidationArguments(
            new ValidationModuleArguments(modulesDirectory, "my-module"),
            new ContextArguments(
                "R6", "de-DE", null, new TerminologyArguments(false, ""), false, true, false),
            null,
            false,
            new ReportArguments(null, false));
    final var validArguments =
        new ValidationArguments(
            new ValidationModuleArguments(modulesDirectory, "my-module"),
            new ContextArguments(
                "R5",
                "en-GB",
                null,
                new TerminologyArguments(true, "https://terminologien.bfarm.de"),
                false,
                false,
                false),
            "https://gematik.de/fhir/myprofile",
            false,
            new ReportArguments(reportDirectory, false));
    final var noModuleDirectoryArguments =
        new ValidationArguments(
            new ValidationModuleArguments(null, "my-module"),
            new ContextArguments(
                "R4", "en-GB", null, new TerminologyArguments(false, ""), false, true, false),
            null,
            false,
            new ReportArguments(reportDirectory, false));
    final var noModuleNameArguments =
        new ValidationArguments(
            new ValidationModuleArguments(modulesDirectory, null),
            new ContextArguments(
                "R4", "en-GB", null, new TerminologyArguments(false, ""), false, true, true),
            null,
            false,
            new ReportArguments(reportDirectory, false));
    final var noModuleArguments =
        new ValidationArguments(
            new ValidationModuleArguments(null, null),
            new ContextArguments(
                "R5",
                "en-GB",
                null,
                new TerminologyArguments(true, "https://terminologien.bfarm.de"),
                false,
                true,
                true),
            "https://gematik.de/fhir/myprofile",
            false,
            new ReportArguments(reportDirectory, false));
    // unsupported FhirVersion
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> configLoader.fromValidationArguments(invalidVersionArguments));

    // missing report settings
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> configLoader.fromValidationArguments(missingReportArguments));

    // Test it works with valid args
    Assertions.assertDoesNotThrow(() -> configLoader.fromValidationArguments(validArguments));

    // no module directory
    Assertions.assertDoesNotThrow(
        () -> configLoader.fromValidationArguments(noModuleDirectoryArguments));

    // no module name and directory
    Assertions.assertDoesNotThrow(
        () -> configLoader.fromValidationArguments(noModuleNameArguments));

    // no module name and directory
    Assertions.assertDoesNotThrow(() -> configLoader.fromValidationArguments(noModuleArguments));
  }

  @DisplayName(
      "Expect that loading configurations for Snapshot Generation from Arguments works as expected")
  @Test
  void expectLoadFromValidSnapshotCliArgumentsSuccessful() {
    final String fhirVersion = "R4";
    final String locale = "de";
    final String terminologyServer = "";
    boolean shouldUseTerminologyServer = false;
    boolean shouldUseOfflineMode = false;
    final Path reportPath = Path.of("report/");
    final Path moduleManifest = Path.of("src/test/resources/module/config.valid.yaml");
    final Path cachePath = Path.of("cache/");
    final var reportFormat = ReportConfiguration.Format.JSON;
    final var arguments =
        new SnapshotGeneratorArguments(
            new ContextArguments(
                fhirVersion,
                locale,
                cachePath,
                new TerminologyArguments(shouldUseTerminologyServer, terminologyServer),
                true,
                true,
                shouldUseOfflineMode),
            new SnapshotGenerationModuleArguments(moduleManifest, null, null),
            new ReportArguments(reportPath, true));
    final var cliConfig =
        Assertions.assertDoesNotThrow(() -> configLoader.fromSnapshotGeneratorArguments(arguments));

    Assertions.assertNotNull(cliConfig);
    Assertions.assertNotNull(cliConfig.context());
    Assertions.assertEquals(FhirRelease.asR4(), cliConfig.context().fhirRelease());
    Assertions.assertEquals(reportPath, cliConfig.report().filePath());
    Assertions.assertEquals(reportFormat, cliConfig.report().format());
  }

  @DisplayName("Unknown snapshot FHIR versions use R4 and missing report settings produce an error")
  @Test
  void expectLoadFromUnknownSnapshotVersionAndMissingReportSettings() {
    // Test: Invalid manifest
    final var context =
        new ContextArguments(
            "R6", "de", null, new TerminologyArguments(false, ""), true, true, false);
    final var report = new ReportArguments(Path.of("report/"), false);
    final var moduleManifest = Path.of("module/config.yaml");
    final var moduleConfigInvalid =
        new SnapshotGenerationModuleArguments(moduleManifest, null, null);
    final var notExistingManifest =
        new SnapshotGeneratorArguments(context, moduleConfigInvalid, report);
    Assertions.assertThrows(
        InitializationException.class,
        () -> configLoader.fromSnapshotGeneratorArguments(notExistingManifest));

    // Test: unknown FHIR Version -> R4
    final var manifestFile =
        Assertions.assertDoesNotThrow(() -> Files.createTempFile("manifest", ".yaml"));
    final var moduleConfigValid = new SnapshotGenerationModuleArguments(manifestFile, null, null);
    final var unknownFhirVersion =
        new SnapshotGeneratorArguments(context, moduleConfigValid, report);
    final var cliConfig = configLoader.fromSnapshotGeneratorArguments(unknownFhirVersion);
    Assertions.assertEquals(FhirRelease.asR4(), cliConfig.context().fhirRelease());

    // Test: invalid Report config
    final var validContext =
        new ContextArguments(
            "R4", "de", null, new TerminologyArguments(false, ""), true, true, false);
    final var missingReportArguments =
        new SnapshotGeneratorArguments(
            validContext, moduleConfigValid, new ReportArguments(null, false));
    Assertions.assertThrows(
        NullPointerException.class,
        () -> configLoader.fromSnapshotGeneratorArguments(missingReportArguments));
  }

  private static Stream<Arguments> provideValidConfigurations() {
    return Stream.of(
        Arguments.of("src/test/resources/config/config.valid.minimal.yaml"),
        Arguments.of("src/test/resources/config/config.valid.complete.yaml"));
  }

  private static Stream<Arguments> provideInvalidConfigurations() {
    return Stream.of(
        Arguments.of("src/test/resources/config/config.invalid.missing-required-settings.yaml"));
  }
}
