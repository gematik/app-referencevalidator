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

import de.gematik.refv.cli.report.boundary.ResultReporter;
import de.gematik.refv.lib.fhir_context.entity.ContextResult;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResultReporterTest {
  private final ResultReporter resultReporter = new ResultReporter();

  @DisplayName("Generating a report with missing/broken information leads to error")
  @Test
  void expertGenerateReportIncompleteWithErrors() {
    Map<String, ContextResult> validationResultMap = new HashMap<>();
    // null map
    Assertions.assertThrows(NullPointerException.class, () -> resultReporter.generateReport(null));
    // empty map
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> resultReporter.generateReport(validationResultMap));
  }

  @DisplayName("Generate a validation report with correct information")
  @Test
  void expertGenerateReportSuccessful() {
    Map<String, ContextResult> validationResultMap = new HashMap<>();
    validationResultMap.put(
        "File1.json",
        createValidSingleResult(
            IssueSeverity.INFORMATION,
            "unknown code",
            "Code is not known, skipping",
            "code.coding.code"));
    validationResultMap.put(
        "File2.json",
        createValidSingleResult(IssueSeverity.ERROR, "parsing error", "Malformed content", ""));
    final var result =
        Assertions.assertDoesNotThrow(() -> resultReporter.generateReport(validationResultMap));
    // Result should be invalid
    Assertions.assertEquals(false, result);
  }

  @DisplayName(
      "Generate a validation report for a single file, where INFORMATION and ERROR Severity Levels lead to invalid result")
  @Test
  void expertGenerateSingleReportSuccessfulWithErrorLevel() {
    Map<String, ContextResult> validationResultMap = new HashMap<>();
    validationResultMap.put(
        "File1.json",
        new ValidationResult(
            List.of(
                createValidSingleMessage(
                    IssueSeverity.INFORMATION,
                    "unknown code",
                    "Code is not known, skipping",
                    "code.coding.code"),
                createValidSingleMessage(
                    IssueSeverity.ERROR, "parsing error", "Malformed content", ""))));
    final var result =
        Assertions.assertDoesNotThrow(() -> resultReporter.generateReport(validationResultMap));
    // Result should be invalid
    Assertions.assertEquals(false, result);
  }

  @DisplayName(
      "Generate a validation report for a single file, where INFORMATION and WARNING Severity Levels lead to valid result")
  @Test
  void expertGenerateSingleReportSuccessfulWithWarningLevel() {
    Map<String, ContextResult> validationResultMap = new HashMap<>();
    validationResultMap.put(
        "File1.json",
        new ValidationResult(
            List.of(
                createValidSingleMessage(
                    IssueSeverity.INFORMATION,
                    "unknown code",
                    "Code is not known, skipping",
                    "code.coding.code"),
                createValidSingleMessage(
                    IssueSeverity.WARNING,
                    "the reference was incomplete",
                    "Broken reference",
                    "subject.reference"))));
    final var result =
        Assertions.assertDoesNotThrow(() -> resultReporter.generateReport(validationResultMap));
    // Result should be valid
    Assertions.assertEquals(true, result);
  }

  @Test
  void writeJsonTriggersErrorOnBadInput() {
    final var reportPath = Path.of("/tmp/report.json");
    // null values
    final Map<String, ContextResult> nullResults = null;
    Assertions.assertThrows(
        NullPointerException.class, () -> resultReporter.writeJsonReport(nullResults, reportPath));
    // empty values
    final Map<String, ContextResult> emptyResults = Map.of();
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> resultReporter.writeJsonReport(emptyResults, reportPath));
    // bad Path
    final Map<String, ContextResult> validationResultMap =
        Map.of(
            "File1.json",
            createValidSingleResult(
                IssueSeverity.INFORMATION,
                "unknown code",
                "Code is not known, skipping",
                "code.coding.code"));

    final var notExistingReport = Path.of("/this/does/not/exist/report.json");
    Assertions.assertThrows(
        AccessDeniedException.class,
        () -> resultReporter.writeJsonReport(validationResultMap, notExistingReport));
  }

  @Test
  void writeJsonWorksWithGoodArguments() {
    final var tempFilePath =
        Assertions.assertDoesNotThrow(() -> Files.createTempFile("refv-report-test", ".json"));
    tempFilePath.toFile().deleteOnExit();
    final Map<String, ContextResult> validationResultMap = new HashMap<>();
    validationResultMap.put(
        "File1.json",
        new ValidationResult(
            List.of(
                createValidSingleMessage(
                    IssueSeverity.INFORMATION,
                    "unknown code",
                    "Code is not known, skipping",
                    "code.coding.code"),
                createValidSingleMessage(
                    IssueSeverity.WARNING,
                    "the reference was incomplete",
                    "Broken reference",
                    "subject.reference"))));

    Assertions.assertDoesNotThrow(
        () -> resultReporter.writeJsonReport(validationResultMap, tempFilePath));
  }

  private static @NonNull ValidationResult createValidSingleResult(
      IssueSeverity severity, String messageContent, String messageId, String location) {
    return new ValidationResult(
        List.of(createValidSingleMessage(severity, messageContent, messageId, location)));
  }

  private static @NonNull ResultMessage createValidSingleMessage(
      IssueSeverity severity, String messageContent, String messageId, String location) {
    return new ResultMessage(messageContent, messageId, severity);
  }
}
