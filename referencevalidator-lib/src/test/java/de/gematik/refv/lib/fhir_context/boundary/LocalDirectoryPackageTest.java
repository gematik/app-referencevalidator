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
package de.gematik.refv.lib.fhir_context.boundary;

import de.gematik.refv.lib.exceptions.PackageParsingFailedException;
import de.gematik.refv.lib.package_resolver.entity.LocalDirectory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalDirectoryPackageTest {

  @DisplayName(
      "Given valid coordinates and a path, when parsing, then name and version are extracted")
  @Test
  void expectFromPackageCoordinatesParses() {
    final var tempPath =
        Assertions.assertDoesNotThrow(() -> Files.createTempDirectory("localpackage"));
    tempPath.toFile().deleteOnExit();
    final var pkg = LocalDirectory.parse("minimal.example#1.0.0", tempPath);
    Assertions.assertEquals("minimal.example", pkg.id().name());
    Assertions.assertEquals("1.0.0", pkg.id().version());
    Assertions.assertEquals(tempPath, pkg.path());
  }

  @DisplayName(
      "Given malformed coordinates, when parsing, then a PackageParsingFailedException is thrown")
  @Test
  void expectMalformedCoordinatesThrow() {
    final var pkgPath = Path.of("/tmp/x");
    Assertions.assertThrows(
        PackageParsingFailedException.class, () -> LocalDirectory.parse("noversion", pkgPath));
  }

  @DisplayName("Given null fields, when created, then a NullPointerException is thrown")
  @Test
  void expectNullFieldsThrow() {
    var pkgId = new PackageId("n", "1.0");
    Assertions.assertThrows(NullPointerException.class, () -> new LocalDirectory(pkgId, null));
  }
}
