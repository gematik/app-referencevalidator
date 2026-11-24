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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.refv.cli.commands.entity.ContextArguments;
import de.gematik.refv.cli.commands.entity.ReportArguments;
import de.gematik.refv.cli.commands.entity.SnapshotGenerationModuleArguments;
import de.gematik.refv.cli.commands.entity.SnapshotGeneratorArguments;
import de.gematik.refv.cli.commands.entity.TerminologyArguments;
import de.gematik.refv.cli.config.boundary.ConfigLoader;
import de.gematik.refv.lib.exceptions.InitializationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigBuilderDiagnosticsTest {
  @TempDir Path tempDir;

  @Test
  void rejectsDirectoryAsModuleManifest() {
    var configLoader = new ConfigLoader();
    var reportPath = tempDir.resolve("report.html");
    var arguments = snapshotArguments(tempDir, reportPath);
    var failure =
        assertThrows(
            InitializationException.class,
            () -> configLoader.fromSnapshotGeneratorArguments(arguments));
    assertTrue(failure.getMessage().contains("not a regular file"));
    assertTrue(failure.getMessage().contains(tempDir.toString()));
  }

  @Test
  void reportsUnreadableConfigurationAndModuleManifest() throws IOException {
    Assumptions.assumeTrue(
        tempDir.getFileSystem().supportedFileAttributeViews().contains("posix"),
        "Unreadable-file permissions require a POSIX file system");
    var path = Files.writeString(tempDir.resolve("unreadable.yaml"), "not relevant");
    var permissions = Files.getPosixFilePermissions(path);
    Files.setPosixFilePermissions(path, Set.<PosixFilePermission>of());
    try {
      Assumptions.assumeFalse(Files.isReadable(path), "The current user can still read this file");

      var configLoader = new ConfigLoader();
      var configurationFailure =
          assertThrows(
              InitializationException.class, () -> configLoader.loadValidationConfigFromPath(path));
      assertTrue(configurationFailure.getMessage().contains("not readable"));

      var arguments = snapshotArguments(path, tempDir.resolve("report.html"));
      var manifestFailure =
          assertThrows(
              InitializationException.class,
              () -> configLoader.fromSnapshotGeneratorArguments(arguments));
      assertTrue(manifestFailure.getMessage().contains("not readable"));
    } finally {
      Files.setPosixFilePermissions(path, permissions);
    }
  }

  private static SnapshotGeneratorArguments snapshotArguments(Path manifestPath, Path reportPath) {
    return new SnapshotGeneratorArguments(
        new ContextArguments(
            "R4", "de", null, new TerminologyArguments(false, ""), true, true, false),
        new SnapshotGenerationModuleArguments(manifestPath, null, null),
        new ReportArguments(reportPath, false));
  }
}
