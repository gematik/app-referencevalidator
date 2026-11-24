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
package de.gematik.refv.lib.fhir_context.control;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ManagedTemporaryDirectoryTest {
  @TempDir Path tempDir;

  @DisplayName(
      "Given shared cache leases, when closed, then storage is removed after the last lease")
  @Test
  void deletesDirectoryOnlyAfterLastLeaseCloses() throws Exception {
    Path directory = Files.createDirectory(tempDir.resolve("owned-cache"));
    Files.writeString(directory.resolve("entry"), "cache");
    var firstLease = ManagedTemporaryDirectory.owned(directory);
    var secondLease = firstLease.retain();

    firstLease.close();
    Assertions.assertTrue(Files.exists(directory));

    secondLease.close();
    Assertions.assertFalse(Files.exists(directory));
  }
}
