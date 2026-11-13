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
package de.gematik.refv.cli.commands.boundary;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValidationInputDiagnosticsTest {
  @TempDir Path tempDir;

  @Test
  void explainsMissingAndEmptyValidationInputs() throws IOException {
    var missingPath = tempDir.resolve("missing");
    var missingFailure =
        assertThrows(
            IOException.class,
            () -> ValidationInputCollector.collect(List.of(missingPath.toString())));
    assertTrue(missingFailure.getMessage().contains("Validation input does not exist"));
    assertTrue(missingFailure.getMessage().contains(missingPath.toString()));

    var emptyDirectory = Files.createDirectory(tempDir.resolve("empty"));
    var emptyFailure =
        assertThrows(
            IOException.class,
            () -> ValidationInputCollector.collect(List.of(emptyDirectory.toString())));
    assertTrue(emptyFailure.getMessage().contains("No regular FHIR resource files were found"));

    var noSourcesFailure =
        assertThrows(IOException.class, () -> ValidationInputCollector.collect(List.of()));
    assertTrue(noSourcesFailure.getMessage().contains("No FHIR resource files were specified"));
  }

  @Test
  void rejectsUnreadableInputsWhenThePlatformEnforcesPosixPermissions() throws IOException {
    Assumptions.assumeTrue(
        tempDir.getFileSystem().supportedFileAttributeViews().contains("posix"),
        "Unreadable-file permissions require a POSIX file system");
    var file = Files.writeString(tempDir.resolve("unreadable.json"), "{}");
    var permissions = Files.getPosixFilePermissions(file);
    Files.setPosixFilePermissions(file, Set.<PosixFilePermission>of());
    try {
      Assumptions.assumeFalse(Files.isReadable(file), "The current user can still read this file");
      var failure =
          assertThrows(
              IOException.class,
              () -> ValidationInputCollector.collect(List.of(file.toString())));
      assertTrue(failure.getMessage().contains("Validation input is not readable"));
    } finally {
      Files.setPosixFilePermissions(file, permissions);
    }
  }

  @Test
  void rejectsNonRegularFilesystemEntries() {
    var device = Path.of("/dev/null");
    Assumptions.assumeTrue(
        Files.exists(device) && !Files.isRegularFile(device) && Files.isReadable(device),
        "This test requires a readable non-regular filesystem entry");
    var failure =
        assertThrows(
            IOException.class, () -> ValidationInputCollector.collect(List.of(device.toString())));
    assertTrue(failure.getMessage().contains("not a regular file or directory"));
  }
}
