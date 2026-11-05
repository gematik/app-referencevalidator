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
package de.gematik.refv.cli.report.boundary;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.gematik.refv.cli.commands.VersionProvider;
import de.gematik.refv.cli.report.entity.ExpectedValidationResult;
import de.gematik.refv.lib.fhir_context.entity.ContextResult;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CLI-local report presenter to keep output concerns in the boundary module. */
public class ResultReporter {
  private final Logger log = LoggerFactory.getLogger(ResultReporter.class);
  private final ObjectMapper objectMapper;
  private final long startTime = System.currentTimeMillis();

  public ResultReporter() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    this.objectMapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
  }

  public boolean generateReport(@NonNull Map<String, ContextResult> resultMap) {
    return generateReport(resultMap, null);
  }

  public boolean generateReport(
      @NonNull Map<String, ContextResult> resultMap, @Nullable Set<String> expectedInvalidSources) {
    if (Objects.requireNonNull(resultMap, "The result map cannot be empty").isEmpty()) {
      throw new IllegalArgumentException(
          "Could not generate the report: an empty validation result has been supplied");
    }

    long totalErrors = 0;
    long totalWarnings = 0;
    long totalFatalErrors = 0;
    boolean expectationsMatched = true;

    final StringBuilder stringBuilder = new StringBuilder();

    for (var result : resultMap.entrySet()) {
      if (result.getValue() == null) {
        log.debug("Skipping result for {}", result.getKey());
        continue;
      }
      var messages = result.getValue().messages();
      var counts = countMessages(messages);
      final ExpectedValidationResult expectedResult =
          expectedInvalidSources == null
              ? null
              : expectedValidationResult(result.getKey(), messages, counts, expectedInvalidSources);
      appendValidationResult(stringBuilder, result.getKey(), messages, counts, expectedResult);
      if (expectedResult != null) {
        expectationsMatched &= expectedResult.expectationMatched();
      }
      totalErrors += counts.errors();
      totalFatalErrors += counts.fatalErrors();
      totalWarnings += counts.warnings();
    }

    boolean resultSuccessful =
        expectedInvalidSources == null
            ? totalFatalErrors == 0 && totalErrors == 0
            : expectationsMatched;
    String overallStatus =
        expectedInvalidSources == null
            ? resultSuccessful ? "VALID" : "INVALID"
            : resultSuccessful ? "EXPECTATIONS MET" : "EXPECTATION MISMATCH";
    stringBuilder
        .append("\n")
        .append(
            String.format(
                "Overall: %s | %d file(s) | %d fatal(s) | %d error(s) | %d warning(s)%n",
                overallStatus, resultMap.size(), totalFatalErrors, totalErrors, totalWarnings));

    log.info("{}", stringBuilder);

    return resultSuccessful;
  }

  public void writeHtmlReport(
      @NonNull Map<String, ContextResult> results,
      @NonNull Path outputPath,
      @NonNull String fhirVersion)
      throws IOException {
    writeHtmlReport(results, outputPath, fhirVersion, null);
  }

  public void writeHtmlReport(
      @NonNull Map<String, ContextResult> results,
      @NonNull Path outputPath,
      @NonNull String fhirVersion,
      @Nullable Set<String> expectedInvalidSources)
      throws IOException {
    final var totalTime = System.currentTimeMillis() - startTime;
    final var htmlTemplate = importHtmlTemplate();
    if (Objects.requireNonNull(results, "The results cannot be null").isEmpty()) {
      throw new IllegalArgumentException(
          "Could not write the report: an empty validation result has been supplied");
    }
    if (outputPath.getParent() != null) {
      Files.createDirectories(outputPath.getParent());
    }

    long totalAll = 0;
    long totalErrors = 0;
    long totalWarnings = 0;
    long totalFatalErrors = 0;
    boolean expectationsMatched = true;

    final StringBuilder messageCardsBuilder = new StringBuilder();
    for (var result : results.entrySet()) {
      if (Objects.isNull(result.getValue())) {
        log.debug("Skipping result for {}", result.getKey());
        continue;
      }
      var counts =
          appendHtmlMessages(messageCardsBuilder, result.getKey(), result.getValue().messages());
      if (expectedInvalidSources != null) {
        final var expectedResult =
            expectedValidationResult(
                result.getKey(), result.getValue().messages(), counts, expectedInvalidSources);
        messageCardsBuilder.append(generateExpectedResultEntry(result.getKey(), expectedResult));
        expectationsMatched &= expectedResult.expectationMatched();
      }
      totalAll += counts.total();
      totalErrors += counts.errors();
      totalFatalErrors += counts.fatalErrors();
      totalWarnings += counts.warnings();
    }

    final var totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalTime);
    final var expectationStatus =
        expectedInvalidSources == null
            ? "Not enabled"
            : expectationsMatched ? "All expectations matched" : "Expectation mismatch";
    final var totalTimeFormat =
        String.format(
            "%d min, %d sec",
            totalMinutes,
            TimeUnit.MILLISECONDS.toSeconds(totalTime) - TimeUnit.MINUTES.toSeconds(totalMinutes));

    final var htmlReport =
        htmlTemplate
            .replace("%NUM_FATAL%", String.valueOf(totalFatalErrors))
            .replace("%NUM_ERRORS%", String.valueOf(totalErrors))
            .replace("%NUM_WARNINGS%", String.valueOf(totalWarnings))
            .replace("%NUM_TOTAL%", String.valueOf(totalAll))
            .replace("%EXPECTATION_STATUS%", expectationStatus)
            .replace("%FHIR_VERSION%", fhirVersion)
            .replace("%EXECUTION_TIME%", totalTimeFormat)
            .replace("%APPLICATION_VERSION%", VersionProvider.PROJECT_VERSION)
            .replace(
                "%GENERATED%", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .replace("%MESSAGE_ROWS%", messageCardsBuilder.toString());

    try (OutputStream os = new FileOutputStream(outputPath.toString())) {
      os.write(htmlReport.getBytes(StandardCharsets.UTF_8));
      log.info("HTML report written to: {}", outputPath.toUri());
    }
  }

  private MessageCounts countMessages(Iterable<ResultMessage> messages) {
    long total = 0;
    long errors = 0;
    long fatalErrors = 0;
    long warnings = 0;
    for (var message : messages) {
      total++;
      if (IssueSeverity.ERROR.equals(message.severity())) {
        errors++;
      }
      if (IssueSeverity.FATAL.equals(message.severity())) {
        fatalErrors++;
      }
      if (IssueSeverity.WARNING.equals(message.severity())) {
        warnings++;
      }
    }
    return new MessageCounts(total, errors, fatalErrors, warnings);
  }

  private void appendValidationResult(
      StringBuilder stringBuilder,
      String source,
      Iterable<ResultMessage> messages,
      MessageCounts counts,
      @Nullable ExpectedValidationResult expectedResult) {
    stringBuilder
        .append("\n===================================================\nSource:\t")
        .append(source);
    if (expectedResult == null) {
      stringBuilder
          .append("\nValid:\t")
          .append(
              expectedResultValidity(counts) == ExpectedValidationResult.Validity.VALID
                  ? "YES"
                  : "NO");
    } else {
      stringBuilder
          .append("\nExpected:\t")
          .append(expectedResult.expected())
          .append("\nActual:\t")
          .append(expectedResult.actual())
          .append("\nExpectation:\t")
          .append(expectationLabel(expectedResult));
    }
    stringBuilder.append("\n---------------------------------------------------\n");
    for (var message : messages) {
      stringBuilder.append(
          String.format(
              "  \t%s %s %s%n", message.severity(), message.messageId(), message.messageContent()));
    }
    stringBuilder.append("===================================================");
  }

  private ExpectedValidationResult expectedValidationResult(
      String source,
      java.util.Collection<ResultMessage> messages,
      MessageCounts counts,
      Set<String> expectedInvalidSources) {
    final boolean expectedInvalid = expectedInvalidSources.contains(source);
    final boolean actualInvalid = counts.errors() > 0 || counts.fatalErrors() > 0;
    return new ExpectedValidationResult(
        expectedInvalid
            ? ExpectedValidationResult.Validity.INVALID
            : ExpectedValidationResult.Validity.VALID,
        actualInvalid
            ? ExpectedValidationResult.Validity.INVALID
            : ExpectedValidationResult.Validity.VALID,
        expectedInvalid == actualInvalid,
        messages);
  }

  private ExpectedValidationResult.Validity expectedResultValidity(MessageCounts counts) {
    return counts.errors() > 0 || counts.fatalErrors() > 0
        ? ExpectedValidationResult.Validity.INVALID
        : ExpectedValidationResult.Validity.VALID;
  }

  private String expectationLabel(ExpectedValidationResult expectedResult) {
    if (expectedResult.expected() == ExpectedValidationResult.Validity.INVALID) {
      return expectedResult.expectationMatched() ? "EXPECTED ERROR" : "UNEXPECTED VALID";
    }
    return expectedResult.expectationMatched() ? "EXPECTED VALID" : "UNEXPECTED ERROR";
  }

  private String generateExpectedResultEntry(
      String source, ExpectedValidationResult expectedResult) {
    final String messageClass = expectedResult.expectationMatched() ? "information" : "error";
    final String badgeClass = expectedResult.expectationMatched() ? "info" : "error";
    final String status = expectedResult.expectationMatched() ? "MATCH" : "MISMATCH";
    return String.format(
        """
        <div class="message %s">
            <div class="message-header">
                <span class="badge %s">%s</span>
                <span class="message-id">EXPECTATION</span>
                <span class="message-content">%s: expected %s, actual %s (%s)</span>
            </div>
        </div>
        """,
        messageClass,
        badgeClass,
        status,
        escapeHtml(source),
        expectedResult.expected(),
        expectedResult.actual(),
        expectationLabel(expectedResult));
  }

  private MessageCounts appendHtmlMessages(
      StringBuilder messageCardsBuilder, String source, Iterable<ResultMessage> messages) {
    long total = 0;
    long errors = 0;
    long fatalErrors = 0;
    long warnings = 0;
    for (var message : messages) {
      total++;
      if (IssueSeverity.FATAL.equals(message.severity())) {
        fatalErrors++;
      }
      if (IssueSeverity.ERROR.equals(message.severity())) {
        errors++;
      }
      if (IssueSeverity.WARNING.equals(message.severity())) {
        warnings++;
      }
      messageCardsBuilder.append(generateSingleMessageEntry(source, message));
    }
    return new MessageCounts(total, errors, fatalErrors, warnings);
  }

  private @NonNull String generateSingleMessageEntry(
      @NonNull String fileName, @NonNull ResultMessage message) {
    final var severityCode = message.severity();
    return String.format(
        """
                      <div class="message %s">
                                  <div class="message-header">
                                      <span class="badge %s">
                                          %s
                                      </span>
                                      <span class="message-id">
                                          %s
                                      </span>
                                      <span class="message-content">
                                          %s : %s
                                      </span>
                                  </div>
                              </div>
                             \s""",
        severityCode.getCode(),
        severityCode.getCode(),
        severityCode.name(),
        message.messageId(),
        escapeHtml(fileName),
        escapeHtml(message.messageContent()));
  }

  public void writeJsonReport(@NonNull Map<String, ContextResult> results, @NonNull Path outputPath)
      throws IOException {
    writeJsonReport(results, outputPath, null);
  }

  public void writeJsonReport(
      @NonNull Map<String, ContextResult> results,
      @NonNull Path outputPath,
      @Nullable Set<String> expectedInvalidSources)
      throws IOException {
    if (Objects.requireNonNull(results, "The results cannot be null").isEmpty()) {
      throw new IllegalArgumentException(
          "Could not write the report: an empty validation result has been supplied");
    }
    if (outputPath.getParent() != null) {
      Files.createDirectories(outputPath.getParent());
    }
    if (expectedInvalidSources == null) {
      objectMapper.writeValue(outputPath.toFile(), results);
    } else {
      final Map<String, ExpectedValidationResult> expectedResults = new HashMap<>();
      for (var result : results.entrySet()) {
        if (result.getValue() == null) {
          expectedResults.put(result.getKey(), null);
          continue;
        }
        final var messages = result.getValue().messages();
        expectedResults.put(
            result.getKey(),
            expectedValidationResult(
                result.getKey(), messages, countMessages(messages), expectedInvalidSources));
      }
      objectMapper.writeValue(outputPath.toFile(), expectedResults);
    }
    log.info("JSON report written to: {}", outputPath.toUri());
  }

  private @NonNull String importHtmlTemplate() throws IOException {
    try (InputStream templateInputStream =
        getClass().getResourceAsStream("/report/template.html")) {
      if (Objects.isNull(templateInputStream)) {
        throw new IllegalStateException("Template not loaded or missing");
      }

      return new String(templateInputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String escapeHtml(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private record MessageCounts(long total, long errors, long fatalErrors, long warnings) {}
}
