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

import de.gematik.refv.cli.config.boundary.ConfigGenerator;
import de.gematik.refv.cli.config.entity.ValidationCliConfig;
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import java.nio.file.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfigGeneratorTest {

  @DisplayName("Generate a valid Validator configuration in a valid temporary file")
  @Test
  void expectGenerateNewEmptyValidatorConfigurationWithValidParametersSuccessful() {
    final var tempDir =
        Assertions.assertDoesNotThrow(() -> Files.createTempDirectory("refv-temp-config"));
    tempDir.toFile().deleteOnExit();
    final var tempFile =
        Assertions.assertDoesNotThrow(() -> Files.createTempFile(tempDir, "valid-config", ".yaml"));
    tempFile.toFile().deleteOnExit();

    final var cliConfig =
        Assertions.assertDoesNotThrow(
            () -> ConfigGenerator.generateNewEmptyValidationConfiguration("R4", tempFile));

    assertValidValidationConfiguration(cliConfig);
  }

  private static void assertValidValidationConfiguration(ValidationCliConfig cliConfig) {
    Assertions.assertNotNull(cliConfig);
    Assertions.assertNotNull(cliConfig.context());
    Assertions.assertNotNull(cliConfig.context().fhirRelease());
    Assertions.assertNotNull(cliConfig.context().locale());
    Assertions.assertTrue(cliConfig.context().locale().contentEquals("de"));
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayBestPracticeMessageLevel.SHOW_WARNINGS,
        cliConfig.context().displayBehavior().displayBestPracticeMessageLevel());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.ExampleCodeSystemsUsagePolicy.DISALLOWED,
        cliConfig.context().validationPolicy().exampleCodeSystemsUsagePolicy());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.ExampleRestReferencesPolicy.ALLOWED,
        cliConfig.context().validationPolicy().exampleRestReferencesPolicy());
    Assertions.assertNotNull(cliConfig.context().validationPolicy().exampleUrlsUsagePolicy());
    Assertions.assertNotNull(cliConfig.context().validationPolicy().unknownCodeSystemsPolicy());
    Assertions.assertNotNull(cliConfig.context().validationPolicy().recursiveModePolicy());
    Assertions.assertNotNull(cliConfig.context().displayBehavior().displayWarnings());
    Assertions.assertNotNull(cliConfig.context().displayBehavior().displayMessagesFromReferences());
    Assertions.assertNotNull(cliConfig.context().displayBehavior().displayMessageIds());
    Assertions.assertNotNull(cliConfig.context().displayBehavior().displayInvariantsInMessage());
    Assertions.assertNotNull(
        cliConfig.context().displayBehavior().displayHintAboutNonMustSupport());
    Assertions.assertNotNull(cliConfig.context().packageLoading().remoteDownloadPolicy());
    Assertions.assertNotNull(cliConfig.context().terminology().remoteLoadingPolicy());
    Assertions.assertNull(cliConfig.context().terminology().serverUri());
    Assertions.assertNotNull(cliConfig.module());
    Assertions.assertNotNull(cliConfig.module().name());
    Assertions.assertNotNull(cliConfig.module().directory());
    Assertions.assertNotNull(cliConfig.validationOptions().validationMessagesFilterStrategy());
  }

  @DisplayName("Generate a valid Snapshot Generator configuration in a valid temporary file")
  @Test
  void expectGenerateNewEmptySnapshotGeneratorConfigurationWithValidParametersSuccessful() {
    final var tempDir =
        Assertions.assertDoesNotThrow(() -> Files.createTempDirectory("refv-temp-config"));
    tempDir.toFile().deleteOnExit();
    final var tempFile =
        Assertions.assertDoesNotThrow(() -> Files.createTempFile(tempDir, "valid-config", ".yaml"));
    tempFile.toFile().deleteOnExit();

    final var cliConfig =
        Assertions.assertDoesNotThrow(
            () -> ConfigGenerator.generateNewEmptySnapshotConfiguration("R4", tempFile));

    Assertions.assertNotNull(cliConfig);
    Assertions.assertNotNull(cliConfig.context());
    Assertions.assertNotNull(cliConfig.context().fhirRelease());
    Assertions.assertNotNull(cliConfig.context().locale());
    Assertions.assertTrue(cliConfig.context().locale().contentEquals("de"));
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayBestPracticeMessageLevel.SHOW_WARNINGS,
        cliConfig.context().displayBehavior().displayBestPracticeMessageLevel());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.ExampleCodeSystemsUsagePolicy.DISALLOWED,
        cliConfig.context().validationPolicy().exampleCodeSystemsUsagePolicy());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.ExampleRestReferencesPolicy.ALLOWED,
        cliConfig.context().validationPolicy().exampleRestReferencesPolicy());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.ExampleUrlsUsagePolicy.ALLOWED,
        cliConfig.context().validationPolicy().exampleUrlsUsagePolicy());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.UnknownCodeSystemsPolicy.ALLOWED,
        cliConfig.context().validationPolicy().unknownCodeSystemsPolicy());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.RecursiveModePolicy.DISALLOWED,
        cliConfig.context().validationPolicy().recursiveModePolicy());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayWarnings.ENABLED,
        cliConfig.context().displayBehavior().displayWarnings());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayMessagesFromReferences.ENABLED,
        cliConfig.context().displayBehavior().displayMessagesFromReferences());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayMessageIds.ENABLED,
        cliConfig.context().displayBehavior().displayMessageIds());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayInvariantsInMessage.ENABLED,
        cliConfig.context().displayBehavior().displayInvariantsInMessage());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayHintAboutNonMustSupport.DISABLED,
        cliConfig.context().displayBehavior().displayHintAboutNonMustSupport());
    Assertions.assertEquals(
        PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED,
        cliConfig.context().packageLoading().remoteDownloadPolicy());
    Assertions.assertNotNull(cliConfig.context().terminology().remoteLoadingPolicy());
    Assertions.assertNull(cliConfig.context().terminology().serverUri());
  }

  @DisplayName("Passing invalid parameters triggers an error in the generation of configuration")
  @Test
  void expectGenerateNewEmptyConfigurationWithInvalidParametersFails() {
    final var tempDir =
        Assertions.assertDoesNotThrow(() -> Files.createTempDirectory("refv-temp-config"));
    tempDir.toFile().deleteOnExit();
    final var tempFile =
        Assertions.assertDoesNotThrow(() -> Files.createTempFile(tempDir, "valid-config", ".yaml"));
    tempFile.toFile().deleteOnExit();

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> ConfigGenerator.generateNewEmptySnapshotConfiguration("", tempFile));
  }
}
