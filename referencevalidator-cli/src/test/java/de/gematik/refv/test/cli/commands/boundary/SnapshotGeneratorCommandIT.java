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
package de.gematik.refv.test.cli.commands.boundary;

import de.gematik.refv.cli.commands.boundary.SnapshotGeneratorCommand;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class SnapshotGeneratorCommandIT {

  private Path reportOutputHtml;
  private Path reportOutputJson;
  private Path cachePath;
  private static final String PACKAGE_NAME = "minimal.example";
  private static final String PACKAGE_VERSION = "1.0.0";
  private static final String PACKAGE_COORDINATES = PACKAGE_NAME + "#" + PACKAGE_VERSION;
  private static final String PROFILE_FILE_NAME = "StructureDefinition-TestPatient.json";
  private static final String GENERATED_ARCHIVE_NAME =
      PACKAGE_NAME + "-" + PACKAGE_VERSION + ".tgz";

  @TempDir Path tempDir;

  @BeforeEach
  void beforeEach() {
    reportOutputHtml = tempDir.resolve("output.html");
    reportOutputJson = tempDir.resolve("output.json");
    cachePath = tempDir.resolve("cache");
  }

  @DisplayName(
      "Given a package source directory, when generating snapshots, then an archive containing"
          + " the snapshots is created")
  @Test
  void expectSnapshotGenerationFromPackageDirectoryWorks() throws IOException {
    // Given a minimal FHIR package as source directory
    final Path sourceDir = writeSourcePackage();
    final Path outputDir = tempDir.resolve("out");

    // When the snapshot generation is executed
    final int exitCode =
        executeCommand(
            "--source",
            sourceDir.toString(),
            "--output-dir",
            outputDir.toString(),
            "--cache-dir",
            cachePath.toString(),
            "--report",
            reportOutputHtml.toString());

    // Then the command succeeds and the generated archive contains the snapshot
    Assertions.assertEquals(0, exitCode);
    final Path generatedArchive = outputDir.resolve(GENERATED_ARCHIVE_NAME);
    Assertions.assertTrue(
        Files.exists(generatedArchive), "Expected the generated package archive to exist");
    Assertions.assertTrue(Files.size(generatedArchive) > 0);
    final String profileContent = readEntryFromArchive(generatedArchive);
    Assertions.assertNotNull(
        profileContent, "Expected the profile to be part of the generated archive");
    Assertions.assertTrue(
        profileContent.contains("\"snapshot\""),
        "Expected the profile to contain a generated snapshot");
  }

  @DisplayName(
      "Given a patches directory, when generating snapshots, then the patches are applied before"
          + " the generation")
  @Test
  void expectSnapshotGenerationAppliesPatches() throws IOException {
    // Given a minimal FHIR package and a patch for its profile
    final Path sourceDir = writeSourcePackage();
    final Path patchesDir = Files.createDirectories(tempDir.resolve("patches-input"));
    final Path packagePatchesDir =
        Files.createDirectories(patchesDir.resolve(Path.of("patches", PACKAGE_COORDINATES)));
    Files.writeString(
        packagePatchesDir.resolve(PROFILE_FILE_NAME), structureDefinition("Patched Title Marker"));
    final Path outputDir = tempDir.resolve("out-patched");

    // When the snapshot generation is executed with the patches directory
    final int exitCode =
        executeCommand(
            "--source",
            sourceDir.toString(),
            "--output-dir",
            outputDir.toString(),
            "--patches-dir",
            patchesDir.toString(),
            "--cache-dir",
            cachePath.toString(),
            "--report",
            reportOutputJson.toString(),
            "--json");

    // Then the command succeeds and the patch has been applied to the generated archive
    Assertions.assertEquals(0, exitCode);
    final Path generatedArchive = outputDir.resolve(GENERATED_ARCHIVE_NAME);
    Assertions.assertTrue(
        Files.exists(generatedArchive), "Expected the generated package archive to exist");
    Assertions.assertTrue(Files.size(generatedArchive) > 0);
    final String profileContent = readEntryFromArchive(generatedArchive);
    Assertions.assertNotNull(profileContent);
    Assertions.assertTrue(
        profileContent.contains("Patched Title Marker"),
        "Expected the patch to be applied to the profile");
  }

  @DisplayName(
      "Given a matching package selection, when generating snapshots, then the package is"
          + " processed")
  @Test
  void expectMatchingPackageSelectionGeneratesSnapshots() throws IOException {
    // Given a minimal FHIR package as source directory
    final Path sourceDir = writeSourcePackage();
    final Path outputDir = tempDir.resolve("out-selected");

    // When the snapshot generation is executed with a matching package selection
    final int exitCode =
        executeCommand(
            "--source",
            sourceDir.toString(),
            "--output-dir",
            outputDir.toString(),
            "--cache-dir",
            cachePath.toString(),
            "--packages",
            PACKAGE_NAME + "#" + PACKAGE_VERSION,
            "--report",
            reportOutputHtml.toString());

    // Then the command succeeds and the package has been processed
    Assertions.assertEquals(0, exitCode);
    Assertions.assertTrue(
        Files.exists(outputDir.resolve(GENERATED_ARCHIVE_NAME)),
        "Expected the selected package to be processed");
  }

  @DisplayName(
      "Given a non-matching package selection, when generating snapshots, then the package is"
          + " skipped and the command fails")
  @Test
  void expectNonMatchingPackageSelectionSkipsGeneration() throws IOException {
    // Given a minimal FHIR package as source directory
    final Path sourceDir = writeSourcePackage();
    final Path outputDir = tempDir.resolve("out-skipped");

    // When the snapshot generation is executed with a non-matching package selection
    final int exitCode =
        executeCommand(
            "--source",
            sourceDir.toString(),
            "--output-dir",
            outputDir.toString(),
            "--packages",
            "other.package,9.9.9",
            "--report",
            reportOutputHtml.toString(),
            "--json");

    // Then the package is skipped, no archive is created and the command fails
    Assertions.assertEquals(1, exitCode);
    Assertions.assertFalse(
        Files.exists(outputDir.resolve(GENERATED_ARCHIVE_NAME)),
        "Expected no archive for a skipped package");
  }

  @DisplayName("Execute the generation of snapshot for a remote package")
  @Test
  void expectGenerationForRemotePackageWorks() {
    // Given a minimal FHIR package as source directory
    final String sourcePackage = "de.basisprofil.r4#1.5.4";
    final Path outputDir = tempDir.resolve("out-selected");

    // When the snapshot generation is executed with a matching package selection
    final int exitCode =
        executeCommand(
            "--source",
            sourcePackage,
            "--output-dir",
            outputDir.toString(),
            "--cache-dir",
            cachePath.toString(),
            "--report",
            reportOutputHtml.toString());

    // Then the command succeeds and the package has been processed
    Assertions.assertEquals(0, exitCode);
    Assertions.assertTrue(
        Files.exists(outputDir.resolve("de.basisprofil.r4-1.5.4.tgz")),
        "Expected the selected package to be processed");
  }

  @DisplayName("Generate Snapshots for the passed validation module configuration")
  @Test
  void expectGenerationOfModulesPackagesWorks() {
    final Path moduleConfigPath = Path.of("src/test/resources/module/config.valid.minimal.yaml");
    final Path outputDir = tempDir.resolve("out-selected");
    // When the snapshot generation is executed with a matching package selection
    final int exitCode =
        executeCommand(
            "--module-config",
            moduleConfigPath.toString(),
            "--output-dir",
            outputDir.toString(),
            "--cache-dir",
            cachePath.toString(),
            "--report",
            reportOutputHtml.toString());

    // Then the command succeeds and the package has been processed
    Assertions.assertEquals(0, exitCode);
    Assertions.assertTrue(
        Files.exists(outputDir.resolve("de.basisprofil.r4-1.6.0.tgz")),
        "Expected the selected package to be processed");
  }

  private int executeCommand(String... args) {
    final String[] baseArgs = {"--fhir-version", "R4", "--locale", "de"};
    final String[] allArgs = Arrays.copyOf(args, args.length + baseArgs.length);
    System.arraycopy(baseArgs, 0, allArgs, args.length, baseArgs.length);
    return new CommandLine(new SnapshotGeneratorCommand()).execute(allArgs);
  }

  private Path writeSourcePackage() throws IOException {
    final Path packageContentDir =
        Files.createDirectories(tempDir.resolve(PACKAGE_COORDINATES).resolve("package"));
    Files.writeString(
        packageContentDir.resolve("package.json"),
        """
        {
          "name": "minimal.example",
          "version": "1.0.0",
          "description": "Minimal package for snapshot generation integration tests",
          "fhirVersions": ["4.0.1"],
          "dependencies": {"hl7.fhir.r4.core": "4.0.1"}
        }
        """);
    Files.writeString(
        packageContentDir.resolve(PROFILE_FILE_NAME), structureDefinition("Test Patient"));
    return packageContentDir.getParent();
  }

  private static String structureDefinition(String title) {
    return """
        {
          "resourceType": "StructureDefinition",
          "id": "test-patient",
          "url": "http://example.org/fhir/StructureDefinition/test-patient",
          "version": "1.0.0",
          "name": "TestPatient",
          "title": "%s",
          "status": "active",
          "fhirVersion": "4.0.1",
          "kind": "resource",
          "abstract": false,
          "type": "Patient",
          "baseDefinition": "http://hl7.org/fhir/StructureDefinition/Patient",
          "derivation": "constraint",
          "differential": {
            "element": [
              { "id": "Patient", "path": "Patient" },
              { "id": "Patient.name", "path": "Patient.name", "min": 1 }
            ]
          }
        }
        """
        .formatted(title);
  }

  private static String readEntryFromArchive(@NonNull Path archive) throws IOException {
    try (var tarInput =
        new TarArchiveInputStream(new GzipCompressorInputStream(Files.newInputStream(archive)))) {
      TarArchiveEntry entry;
      while ((entry = tarInput.getNextEntry()) != null) {
        if (entry.getName().endsWith(SnapshotGeneratorCommandIT.PROFILE_FILE_NAME)) {
          return new String(tarInput.readAllBytes(), StandardCharsets.UTF_8);
        }
      }
    }
    return null;
  }
}
