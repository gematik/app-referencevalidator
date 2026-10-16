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

import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnapshotGenerationRequestTest {

  @DisplayName("Given valid paths, when created, then the accessors return the values")
  @Test
  void expectCreationWorks() {
    final var request = new SnapshotGenerationRequest(Path.of("source#1.0.0"), Path.of("out"));
    Assertions.assertEquals(Path.of("source#1.0.0"), request.sourcePackagePath());
    Assertions.assertEquals(Path.of("out"), request.outputDirectoryPath());
  }

  @DisplayName("Given a null source path, when created, then a NullPointerException is thrown")
  @Test
  void expectNullSourceThrows() {
    final var outputPath = Path.of("out");
    Assertions.assertThrows(
        NullPointerException.class, () -> new SnapshotGenerationRequest(null, outputPath));
  }

  @DisplayName("Given a null output path, when created, then a NullPointerException is thrown")
  @Test
  void expectNullOutputThrows() {
    final var sourcePath = Path.of("source");
    Assertions.assertThrows(
        NullPointerException.class, () -> new SnapshotGenerationRequest(sourcePath, null));
  }
}
