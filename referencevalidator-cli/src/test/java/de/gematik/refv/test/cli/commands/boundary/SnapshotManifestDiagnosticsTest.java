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
import de.gematik.refv.test.cli.helpers.TestAppender;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

class SnapshotManifestDiagnosticsTest {
  @TempDir Path tempDir;

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidManifests")
  void reportsManifestPathAndUnderlyingReason(
      String label, UnaryOperator<String> invalidInput, List<String> expectedReasons)
      throws Exception {
    var validManifest =
        Files.readString(Path.of("src/test/resources/module/config.valid.minimal.yaml"));
    var manifest = tempDir.resolve("invalid-manifest.yaml");
    Files.writeString(manifest, invalidInput.apply(validManifest));
    var packages = Files.createDirectory(tempDir.resolve("packages"));
    var appender = TestAppender.createAppender("SnapshotManifestDiagnostics");
    var logger = (Logger) LogManager.getLogger(SnapshotGeneratorCommand.class);
    var previousLevel = logger.getLevel();
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.ERROR);
    try {
      var exitCode =
          new CommandLine(new SnapshotGeneratorCommand())
              .execute(
                  "--module-manifest",
                  manifest.toString(),
                  "--source",
                  packages.toString(),
                  "--output-dir",
                  tempDir.resolve("out").toString(),
                  "--cache-dir",
                  tempDir.resolve("cache").toString(),
                  "--fhir-version",
                  "R4",
                  "--locale",
                  "de",
                  "--report",
                  tempDir.resolve("report.html").toString(),
                  "--offline-mode");
      Assertions.assertEquals(1, exitCode);
      var error =
          appender.getLogsFromCurrentThread().stream()
              .filter(event -> event.getLevel() == Level.ERROR)
              .map(event -> event.getMessage().getFormattedMessage())
              .filter(message -> message.startsWith("Failed to generate snapshots:"))
              .findFirst()
              .orElseThrow();
      Assertions.assertTrue(error.contains("Failed to load configuration"), error);
      Assertions.assertTrue(error.contains(manifest.toString()), error);
      for (var reason : expectedReasons) {
        Assertions.assertTrue(error.contains(reason), error);
      }
    } finally {
      logger.removeAppender(appender);
      logger.setLevel(previousLevel);
      appender.stop();
    }
  }

  static Stream<Arguments> invalidManifests() {
    return Stream.of(
        scenario(
            "unknown manifest field",
            valid ->
                valid
                    + """
                    unexpectedManifestField: true
                    """,
            List.of("Unrecognized field", "unexpectedManifestField")),
        scenario(
            "incompatible manifest value",
            valid ->
                valid
                    + """
                    requireExpansionBeforeValidation: [true, false]
                    """,
            List.of("requireExpansionBeforeValidation", "boolean")),
        scenario(
            "malformed manifest YAML",
            _ -> "configSpecVersion: \"unterminated",
            List.of("quoted scalar", "line", "column")));
  }

  static Arguments scenario(
      String label, UnaryOperator<String> invalidInput, List<String> expectedReasons) {
    return Arguments.of(label, invalidInput, expectedReasons);
  }
}
