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
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class SnapshotGeneratorCommandTest {

  @TempDir Path tempDir;

  @DisplayName(
      "Given a non-existing source, when executing, then the command fails with exit code 1")
  @Test
  void expectNonExistingSourceFails() {
    final int exitCode =
        new CommandLine(new SnapshotGeneratorCommand())
            .execute(
                "--source",
                tempDir.resolve("missing").toString(),
                "--output-dir",
                tempDir.resolve("out").toString(),
                "--fhir-version",
                "R4",
                "--offline-mode");
    Assertions.assertEquals(1, exitCode);
  }

  @DisplayName(
      "Given an invalid config file, when executing, then the command fails with exit code 1")
  @Test
  void expectInvalidConfigFileFails() {
    final int exitCode =
        new CommandLine(new SnapshotGeneratorCommand())
            .execute(
                "--source", tempDir.toString(),
                "--output-dir", tempDir.resolve("out").toString(),
                "--config", tempDir.resolve("missing-config.yaml").toString());
    Assertions.assertEquals(1, exitCode);
  }

  @DisplayName(
      "Expect that the SnapshotGenerator Command fails, since the source folder for packages isn't specified")
  @Test
  void expectNonExistingModuleSourceFails() {
    final int exitCode =
        new CommandLine(new SnapshotGeneratorCommand())
            .execute(
                "--module-manifest",
                tempDir.resolve("missing.yaml").toString(),
                "--output-dir",
                tempDir.resolve("out").toString(),
                "--fhir-version",
                "R4",
                "--locale",
                "de",
                "--report",
                tempDir.resolve("report.html").toString(),
                "--offline-mode");
    Assertions.assertEquals(1, exitCode);
  }
}
