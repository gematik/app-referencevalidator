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
package de.gematik.refv.lib.snapshot.entity;

import de.gematik.refv.lib.exceptions.SnapshotGenerationException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SnapshotGenerationOptionsTest {

  @TempDir Path tempDir;

  @DisplayName("Given no patches directory, when using the default constructor, then it is null")
  @Test
  void expectDefaultConstructorHasNullPatchesDirectory() {
    final var options = new SnapshotGenerationOptions();
    Assertions.assertNull(options.patchesDirectory());
  }

  @DisplayName("Given a valid patches directory, when created, then it is accepted")
  @Test
  void expectValidPatchesDirectoryAccepted() {
    final var options = Assertions.assertDoesNotThrow(() -> new SnapshotGenerationOptions(tempDir));
    Assertions.assertEquals(tempDir, options.patchesDirectory());
  }

  @DisplayName("Given a non-existing patches directory, when created, then an exception is thrown")
  @Test
  void expectNonExistingPatchesDirectoryThrows() {
    final var missing = tempDir.resolve("does-not-exist");
    Assertions.assertThrows(
        SnapshotGenerationException.class, () -> new SnapshotGenerationOptions(missing));
  }

  @DisplayName("Given a file instead of a directory, when created, then an exception is thrown")
  @Test
  void expectFileInsteadOfDirectoryThrows() throws Exception {
    final var file = Files.createFile(tempDir.resolve("a-file.txt"));
    Assertions.assertThrows(
        SnapshotGenerationException.class, () -> new SnapshotGenerationOptions(file));
  }
}
