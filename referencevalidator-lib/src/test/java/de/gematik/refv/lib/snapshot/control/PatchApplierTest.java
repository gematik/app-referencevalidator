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
package de.gematik.refv.lib.snapshot.control;

import de.gematik.refv.lib.package_resolver.entity.LocalDirectory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PatchApplierTest {

  @TempDir Path tempDir;

  @DisplayName("Given a non-existing patch directory, when applying, then nothing happens")
  @Test
  void expectNonExistingPatchDirDoesNothing() {
    final var pkg = new LocalDirectory(new PackageId("my.pkg", "1.0.0"), tempDir);
    final var missing = tempDir.resolve("no-patches");
    Assertions.assertDoesNotThrow(
        () -> PatchApplier.applyForPackage(pkg, Path.of("not existing"), missing));
  }

  @DisplayName("Given a patch file, when applying, then it is copied into the package folder")
  @Test
  void expectPatchFileCopied() throws Exception {
    // Given a patches directory with a patch for my.pkg#1.0.0
    final var patchesRoot = Files.createDirectories(tempDir.resolve("patches"));
    final var packagePatchDir =
        Files.createDirectories(patchesRoot.resolve("patches/my.pkg#1.0.0"));
    Files.writeString(packagePatchDir.resolve("fix.json"), "{\"patched\":true}");

    // And a working directory with a package folder
    final var workDir = Files.createDirectory(tempDir.resolve("work"));
    final var packageWorkDir = Files.createDirectory(workDir.resolve("my.pkg#1.0.0"));
    final var packageContentDir = Files.createDirectory(packageWorkDir.resolve("package"));
    Files.writeString(packageContentDir.resolve("fix.json"), "{\"patched\":true}");

    // When applying patches
    final var pkg = new LocalDirectory(new PackageId("my.pkg", "1.0.0"), packageWorkDir);
    PatchApplier.applyForPackage(pkg, pkg.path(), patchesRoot);

    // Then the patch file is copied
    Assertions.assertTrue(Files.exists(packageWorkDir.resolve("package/fix.json")));
  }
}
