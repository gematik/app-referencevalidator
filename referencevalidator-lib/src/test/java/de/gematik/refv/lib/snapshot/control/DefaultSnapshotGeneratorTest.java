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
package de.gematik.refv.lib.snapshot.control;

import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.TerminologyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.snapshot.boundary.SnapshotGenerator;
import de.gematik.refv.lib.snapshot.boundary.SnapshotGeneratorFactory;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationOptions;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Execution(ExecutionMode.SAME_THREAD)
class DefaultSnapshotGeneratorTest {

  private static final Logger log = LoggerFactory.getLogger(DefaultSnapshotGeneratorTest.class);
  @TempDir private static Path tempDir;

  private static SnapshotGenerator generator;

  @BeforeAll
  static void beforeAll() {
    log.debug("#### Cache path: {}", tempDir.resolve("cache"));
    final var offlineMode =
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED, tempDir.resolve("cache"));

    generator =
        SnapshotGeneratorFactory.fromConfiguration(
            new ContextConfiguration(
                FhirRelease.asR4(),
                "de",
                DisplayBehaviorConfiguration.defaultConfiguration(),
                offlineMode,
                TerminologyConfiguration.defaultConfiguration(),
                ValidationPolicyConfiguration.defaultConfiguration()));
  }

  @AfterAll
  static void closeGenerator() {
    generator.close();
  }

  @DisplayName(
      "Given a null context or context configuration, when created, then a NullPointerException is thrown")
  @Test
  void expectNullContextThrows() {
    Assertions.assertThrows(
        NullPointerException.class,
        () -> new DefaultSnapshotGenerator((ContextConfiguration) null));
  }

  @DisplayName("R1.10 — an invalid source path returns an error result")
  @Test
  void expectNonExistingSourceThrows() {
    final var request =
        new SnapshotGenerationRequest(tempDir.resolve("missing"), tempDir.resolve("out"));
    final var result =
        Assertions.assertDoesNotThrow(
            () -> generator.generateSnapshots(request, new SnapshotGenerationOptions()));
    Assertions.assertTrue(
        result.messages().stream()
            .anyMatch(resultMessage -> resultMessage.severity().equals(IssueSeverity.ERROR)));
  }

  @DisplayName(
      "Given a source path that is a file, when generating, then a Snapshot Generation Error is generated")
  @Test
  void expectFileSourceThrows() throws Exception {
    final var file = Files.createFile(tempDir.resolve("a-file.txt"));
    final var request = new SnapshotGenerationRequest(file, tempDir.resolve("out"));
    final var result =
        Assertions.assertDoesNotThrow(
            () -> generator.generateSnapshots(request, new SnapshotGenerationOptions()));
    Assertions.assertTrue(
        result.messages().stream()
            .anyMatch(resultMessage -> resultMessage.severity().equals(IssueSeverity.ERROR)));
  }

  @DisplayName(
      "Given a null request, when generating, then a Snapshot Generation Error is generated")
  @Test
  void expectNullRequestThrows() {
    final var result =
        Assertions.assertDoesNotThrow(
            () -> generator.generateSnapshots(null, new SnapshotGenerationOptions()));
    Assertions.assertTrue(
        result.messages().stream()
            .anyMatch(resultMessage -> resultMessage.severity().equals(IssueSeverity.ERROR)));
    Assertions.assertTrue(
        result.messages().stream()
            .anyMatch(
                resultMessage ->
                    resultMessage.messageContent().contains("The request cannot be null")));
  }

  @DisplayName("Given null options, when generating, then a Snapshot Generation Error is generated")
  @Test
  void expectNullOptionsThrows() {
    final var request =
        new SnapshotGenerationRequest(tempDir.resolve("src"), tempDir.resolve("out"));
    final var result =
        Assertions.assertDoesNotThrow(() -> generator.generateSnapshots(request, null));
    Assertions.assertTrue(
        result.messages().stream()
            .anyMatch(resultMessage -> resultMessage.severity().equals(IssueSeverity.ERROR)));
    Assertions.assertTrue(
        result.messages().stream()
            .anyMatch(
                resultMessage ->
                    resultMessage.messageContent().contains("The options cannot be null")));
  }
}
