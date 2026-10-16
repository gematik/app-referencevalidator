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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NonNull;
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
    if (Objects.requireNonNull(resultMap, "The result map cannot be empty").isEmpty()) {
      throw new IllegalArgumentException(
          "Could not generate the report: an empty validation result has been supplied");
    }

    long totalErrors = 0;
    long totalWarnings = 0;
    long totalFatalErrors = 0;

    final StringBuilder stringBuilder = new StringBuilder();

    for (var result : resultMap.entrySet()) {
      if (result.getValue() == null) {
        log.debug("Skipping result for {}", result.getKey());
        continue;
      }
      var messages = result.getValue().messages();
      var counts = countMessages(messages);
      appendValidationResult(stringBuilder, result.getKey(), messages, counts);
      totalErrors += counts.errors();
      totalFatalErrors += counts.fatalErrors();
      totalWarnings += counts.warnings();
    }

    boolean overallValid = totalFatalErrors == 0 && totalErrors == 0;
    stringBuilder
        .append("\n")
        .append(
            String.format(
                "Overall: %s | %d file(s) | %d fatal(s) | %d error(s) | %d warning(s)%n",
                overallValid ? "VALID" : "INVALID",
                resultMap.size(),
                totalFatalErrors,
                totalErrors,
                totalWarnings));

    log.info("{}", stringBuilder);

    return overallValid;
  }

  public void writeHtmlReport(
      @NonNull Map<String, ContextResult> results,
      @NonNull Path outputPath,
      @NonNull String fhirVersion)
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

    final StringBuilder messageCardsBuilder = new StringBuilder();
    for (var result : results.entrySet()) {
      if (Objects.isNull(result.getValue())) {
        log.debug("Skipping result for {}", result.getKey());
        continue;
      }
      var counts =
          appendHtmlMessages(messageCardsBuilder, result.getKey(), result.getValue().messages());
      totalAll += counts.total();
      totalErrors += counts.errors();
      totalFatalErrors += counts.fatalErrors();
      totalWarnings += counts.warnings();
    }

    final var totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalTime);
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
      MessageCounts counts) {
    stringBuilder
        .append("\n")
        .append(
            String.format(
                """
                  ===================================================
                  Source:\t%s
                  Valid:\t%s
                  ---------------------------------------------------
                  """,
                source, counts.errors() == 0 && counts.fatalErrors() == 0 ? "YES" : "NO"));
    for (var message : messages) {
      stringBuilder.append(
          String.format(
              "  \t%s %s %s%n", message.severity(), message.messageId(), message.messageContent()));
    }
    stringBuilder.append("===================================================");
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
    if (Objects.requireNonNull(results, "The results cannot be null").isEmpty()) {
      throw new IllegalArgumentException(
          "Could not write the report: an empty validation result has been supplied");
    }
    if (outputPath.getParent() != null) {
      Files.createDirectories(outputPath.getParent());
    }
    objectMapper.writeValue(outputPath.toFile(), results);
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
