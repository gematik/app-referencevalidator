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
package de.gematik.refv.lib.snapshot.boundary;

import de.gematik.refv.lib.common.ResultPrinter;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.TerminologyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationModuleIndex;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolver;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolverFactory;
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationOptions;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationRequest;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationResult;
import de.gematik.refv.lib.valmodule.boundary.ModuleLoader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("resource")
class SnapshotGeneratorIT {
  @TempDir private Path tempDir;
  private final List<SnapshotGenerator> generators = new ArrayList<>();

  @AfterEach
  void closeGenerators() {
    generators.forEach(SnapshotGenerator::close);
  }

  @DisplayName(
      "Performs the Snapshot Generation for a given Remote Implementation Guide and no custom patches")
  @Test
  void testSnapshotGenerationWorksWithRemoteImplementationGuideAndNoPatches() { // Given a FHIR
    // Release
    final var fhirRelease = FhirRelease.asR4();
    // Given a remote IG
    final var packagePath = Path.of("de.gematik.terminology#1.0.9");
    // and When I generate a snapshot
    final var contextConfiguration =
        new ContextConfiguration(
            fhirRelease,
            "de",
            DisplayBehaviorConfiguration.defaultConfiguration(),
            packageDownloadConfiguration(),
            new TerminologyConfiguration(
                TerminologyConfiguration.RemoteLoadingPolicy.ALLOWED,
                URI.create("https://tx.fhir.org")),
            ValidationPolicyConfiguration.defaultConfiguration());
    final var snapshotGenerator = createSnapshotGenerator(contextConfiguration);
    final var result =
        Assertions.assertDoesNotThrow(
            () ->
                snapshotGenerator.generateSnapshots(
                    new SnapshotGenerationRequest(packagePath, tempDir),
                    new SnapshotGenerationOptions()));
    // Then it is successful
    Assertions.assertFalse(result.messages().isEmpty());
    ResultPrinter.printMessages(result);
    Assertions.assertTrue(
        result.messages().stream()
            .noneMatch(
                resultMessage ->
                    resultMessage
                            .severity()
                            .getCode()
                            .equalsIgnoreCase(IssueSeverity.ERROR.getCode())
                        || resultMessage
                            .severity()
                            .getCode()
                            .equalsIgnoreCase(IssueSeverity.FATAL.getCode())));
  }

  @DisplayName("R1.1 — generates a snapshot for a local Implementation Guide and its dependencies")
  @Test
  void testSnapshotGenerationWorksWithLocalDirectoryImplementationGuide() {
    // Given a FHIR Release
    final var fhirRelease = FhirRelease.asR4();
    // Given a local directory IG
    final var packagePath = Path.of("src/test/resources/packages/minimal.example#1.0.0");
    // and When I generate a snapshot
    final var contextConfiguration =
        new ContextConfiguration(
            fhirRelease,
            "en-GB",
            DisplayBehaviorConfiguration.defaultConfiguration(),
            packageDownloadConfiguration(),
            new TerminologyConfiguration(
                TerminologyConfiguration.RemoteLoadingPolicy.ALLOWED,
                URI.create("https://tx.fhir.org")),
            ValidationPolicyConfiguration.defaultConfiguration());
    final var snapshotGenerator = createSnapshotGenerator(contextConfiguration);
    final var result =
        Assertions.assertDoesNotThrow(
            () ->
                snapshotGenerator.generateSnapshots(
                    new SnapshotGenerationRequest(packagePath, tempDir),
                    new SnapshotGenerationOptions()));
    // Then it is successful
    Assertions.assertFalse(result.messages().isEmpty());
    ResultPrinter.printMessages(result);
    Assertions.assertTrue(
        result.messages().stream()
            .noneMatch(
                resultMessage ->
                    resultMessage
                            .severity()
                            .getCode()
                            .equalsIgnoreCase(IssueSeverity.ERROR.getCode())
                        || resultMessage
                            .severity()
                            .getCode()
                            .equalsIgnoreCase(IssueSeverity.FATAL.getCode())));
    Assertions.assertTrue(Files.isRegularFile(tempDir.resolve("minimal.example-1.0.0.tgz")));
  }

  @DisplayName("Performs the Snapshot Generation using a group of TGZ Archives")
  @Test
  void testSnapshotGenerationWorksWithLocalTgzArchives() {
    // Given a FHIR Release
    final var fhirRelease = FhirRelease.asR4();
    // Given a local directory containing many IG in TGZ format
    final var packagePath = Path.of("src/test/resources/packages/erezept/minimal");
    // and When I generate a snapshot
    final var contextConfiguration =
        new ContextConfiguration(
            fhirRelease,
            "en-US",
            DisplayBehaviorConfiguration.defaultConfiguration(),
            packageDownloadConfiguration(),
            new TerminologyConfiguration(
                TerminologyConfiguration.RemoteLoadingPolicy.ALLOWED,
                URI.create("https://tx.fhir.org")),
            ValidationPolicyConfiguration.defaultConfiguration());
    final var snapshotGenerator = createSnapshotGenerator(contextConfiguration);
    final var result =
        Assertions.assertDoesNotThrow(
            () ->
                snapshotGenerator.generateSnapshots(
                    new SnapshotGenerationRequest(
                        packagePath, tempDir, List.of("de.abda.erezeptabgabedaten#1.5.0")),
                    new SnapshotGenerationOptions()));
    // Then it is successful
    Assertions.assertFalse(result.messages().isEmpty());
    ResultPrinter.printMessages(result);
    Assertions.assertTrue(
        result.messages().stream()
            .noneMatch(
                resultMessage ->
                    resultMessage
                            .severity()
                            .getCode()
                            .equalsIgnoreCase(IssueSeverity.ERROR.getCode())
                        || resultMessage
                            .severity()
                            .getCode()
                            .equalsIgnoreCase(IssueSeverity.FATAL.getCode())));
  }

  @DisplayName("R1.7 — applies the matching package patch before snapshot generation")
  @Test
  void testSnapshotGenerationAppliesPackagePatch() {
    // Given a FHIR Release
    final var fhirRelease = FhirRelease.asR4();
    // Given a local IG with a matching package patch
    final var packagePath = Path.of("src/test/resources/packages/minimal.example#1.0.0");
    // and When I generate a snapshot
    final var contextConfiguration =
        new ContextConfiguration(
            fhirRelease,
            "en-US",
            DisplayBehaviorConfiguration.defaultConfiguration(),
            packageDownloadConfiguration(),
            new TerminologyConfiguration(
                TerminologyConfiguration.RemoteLoadingPolicy.ALLOWED,
                URI.create("https://tx.fhir.org")),
            ValidationPolicyConfiguration.defaultConfiguration());
    // When I resolve the package
    final var snapshotGenerator = createSnapshotGenerator(contextConfiguration);
    final var result =
        Assertions.assertDoesNotThrow(
            () ->
                snapshotGenerator.generateSnapshots(
                    new SnapshotGenerationRequest(packagePath, tempDir),
                    new SnapshotGenerationOptions(
                        Path.of("src/test/resources/packages/erezept/withpatch/patches"))));
    // Then it is successful
    Assertions.assertFalse(result.messages().isEmpty());
    ResultPrinter.printMessages(result);
    Assertions.assertTrue(
        result.messages().stream()
            .noneMatch(
                resultMessage ->
                    resultMessage
                            .severity()
                            .getCode()
                            .equalsIgnoreCase(IssueSeverity.ERROR.getCode())
                        || resultMessage
                            .severity()
                            .getCode()
                            .equalsIgnoreCase(IssueSeverity.FATAL.getCode())));
    Assertions.assertTrue(Files.isRegularFile(tempDir.resolve("minimal.example-1.0.0.tgz")));
  }

  @DisplayName("Performs the Snapshot Generation using a patch that overrides packages.json")
  @Test
  void testSnapshotGenerationWorksWithPatchOverridingPackagesJson() {
    // Given a FHIR Release
    final var fhirRelease = FhirRelease.asR4();
    // Given a local directory IG
    final var packagePath = Path.of("src/test/resources/packages/minimal.example#1.0.0");
    // and When I generate a snapshot
    final var contextConfiguration =
        new ContextConfiguration(
            fhirRelease,
            "en-US",
            DisplayBehaviorConfiguration.defaultConfiguration(),
            packageDownloadConfiguration(),
            new TerminologyConfiguration(
                TerminologyConfiguration.RemoteLoadingPolicy.ALLOWED,
                URI.create("https://tx.fhir.org")),
            ValidationPolicyConfiguration.defaultConfiguration());
    // When I resolve the package
    final var snapshotGenerator = createSnapshotGenerator(contextConfiguration);
    final var result =
        Assertions.assertDoesNotThrow(
            () ->
                snapshotGenerator.generateSnapshots(
                    new SnapshotGenerationRequest(packagePath, tempDir),
                    new SnapshotGenerationOptions(
                        Path.of("src/test/resources/packages/erezept/withpatch/patches"))));
    // Then it is successful
    Assertions.assertFalse(result.messages().isEmpty());
    ResultPrinter.printMessages(result);
    Assertions.assertTrue(
        result.messages().stream()
            .noneMatch(
                resultMessage ->
                    resultMessage
                            .severity()
                            .getCode()
                            .equalsIgnoreCase(IssueSeverity.ERROR.getCode())
                        || resultMessage
                            .severity()
                            .getCode()
                            .equalsIgnoreCase(IssueSeverity.FATAL.getCode())));
  }

  @DisplayName("Performs the Snapshot Generation using the Configuration as specified in a Module")
  @Test
  void testSnapshotGenerationWorksWithConfigurationInModule() {
    // Given a Module
    Path validationModulePath = Path.of("src/test/resources/modules/isik.jar");
    final var validationModule =
        ModuleLoader.defaultLoader().forValidation(validationModulePath.getParent(), "isik5");
    Assertions.assertTrue(validationModule.isPresent());
    final var validationModuleIndex = new ValidationModuleIndex(validationModule.get());
    final var moduleConfiguration = validationModuleIndex.getValidationModuleConfiguration();
    // Given a FHIR Release
    final var fhirRelease = FhirRelease.asR4();
    // When I generate a snapshot
    final var contextConfiguration =
        new ContextConfiguration(
            fhirRelease,
            "en-US",
            DisplayBehaviorConfiguration.defaultConfiguration(),
            packageDownloadConfiguration(),
            new TerminologyConfiguration(
                TerminologyConfiguration.RemoteLoadingPolicy.ALLOWED,
                URI.create("https://tx.fhir.org")),
            new ValidationPolicyConfiguration(
                ValidationPolicyConfiguration.ValidationLevelPolicy.SHOW_WARNINGS_AND_ERRORS,
                ValidationPolicyConfiguration.UnknownCodeSystemsPolicy.DISALLOWED,
                ValidationPolicyConfiguration.ExampleCodeSystemsUsagePolicy.ALLOWED,
                ValidationPolicyConfiguration.ExampleUrlsUsagePolicy.DISALLOWED,
                ValidationPolicyConfiguration.ExampleRestReferencesPolicy.ALLOWED,
                ValidationPolicyConfiguration.RecursiveModePolicy.DISALLOWED,
                moduleConfiguration.anyExtensionsAllowed()
                    ? ValidationPolicyConfiguration.AnyExtensionPolicy.ALLOWED
                    : ValidationPolicyConfiguration.AnyExtensionPolicy.DISALLOWED));
    // When I resolve the package
    final var packagesToGenerate = new ArrayList<String>();
    for (var groupValue : moduleConfiguration.packageGroups().values()) {
      final var foundPackage = groupValue.packages().getFirst();
      packagesToGenerate.add(
          foundPackage.contains(LocalArchive.PACKAGE_SEPARATOR)
                  && foundPackage.endsWith(LocalArchive.ARCHIVE_PACKAGE_EXTENSION)
              ? LocalArchive.parse(foundPackage).coordinates()
              : foundPackage);
    }
    PackageResolver packageResolver =
        PackageResolverFactory.withConfiguration(contextConfiguration.packageLoading());
    packageResolver.loadModulePackages(validationModulePath);
    // and I generate the snapshots
    final var snapshotGenerator = createSnapshotGenerator(contextConfiguration);
    final var resultMap = new HashMap<String, SnapshotGenerationResult>();
    for (var packageName : packagesToGenerate) {
      final var result =
          Assertions.assertDoesNotThrow(
              () ->
                  snapshotGenerator.generateSnapshots(
                      new SnapshotGenerationRequest(
                          contextConfiguration.packageLoading().cachePath().resolve(packageName),
                          tempDir,
                          packagesToGenerate),
                      new SnapshotGenerationOptions()));
      resultMap.put(packageName, result);
    }

    // Then it is successful
    Assertions.assertFalse(resultMap.isEmpty());
    for (var result : resultMap.values()) {
      ResultPrinter.printMessages(result);
      Assertions.assertTrue(
          result.messages().stream()
              .noneMatch(
                  resultMessage ->
                      resultMessage
                              .severity()
                              .getCode()
                              .equalsIgnoreCase(IssueSeverity.ERROR.getCode())
                          || resultMessage
                              .severity()
                              .getCode()
                              .equalsIgnoreCase(IssueSeverity.FATAL.getCode())));
    }
  }

  private SnapshotGenerator createSnapshotGenerator(ContextConfiguration configuration) {
    return Assertions.assertDoesNotThrow(
        () -> {
          var generator = SnapshotGeneratorFactory.fromConfiguration(configuration);
          generators.add(generator);
          return generator;
        });
  }

  private PackageDownloadConfiguration packageDownloadConfiguration() {
    return new PackageDownloadConfiguration(
        PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED,
        tempDir.resolve("package-cache"));
  }
}
