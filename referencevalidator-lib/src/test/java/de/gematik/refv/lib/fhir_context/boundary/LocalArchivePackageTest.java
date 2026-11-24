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
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalArchivePackageTest {

  @DisplayName("Given a tgz package, when reading the coordinates, it is correct")
  @Test
  void expectArchiveName() {
    final var pkg =
        new LocalArchive(
            new PackageId("hl7.fhir.r4.core", "4.0.1"),
            Path.of("src/test/resources/packages/erezept/minimal/hl7.fhir.r4.core-4.0.1.tgz"));
    Assertions.assertEquals("hl7.fhir.r4.core#4.0.1", pkg.id().coordinates());
  }

  @DisplayName(
      "Given a valid archive file name, when parsing coordinates, then name and version are extracted")
  @Test
  void expectFromPackageCoordinatesParses() {
    final var pkg =
        LocalArchive.parse(
            "hl7.fhir.r4.core-4.0.1.tgz",
            Path.of("src/test/resources/packages/erezept/minimal/hl7.fhir.r4.core-4.0.1.tgz"));
    Assertions.assertEquals("hl7.fhir.r4.core", pkg.id().name());
    Assertions.assertEquals("4.0.1", pkg.id().version());
  }

  @DisplayName(
      "Given a malformed archive file name, when parsing coordinates, then a PackageParsingFailedException is thrown")
  @Test
  void expectFromPackageCoordinatesMalformedThrows() {
    final Path archivePath = Path.of("/tmp/x.tgz");
    Assertions.assertThrows(
        PackageParsingFailedException.class,
        () -> LocalArchive.parse("noversion.tgz", archivePath));
  }

  @DisplayName("Given null fields, when created, then a NullPointerException is thrown")
  @Test
  void expectNullFieldsThrow() {
    Assertions.assertThrows(NullPointerException.class, () -> new PackageId(null, "1.0"));
    Assertions.assertThrows(NullPointerException.class, () -> new PackageId("n", null));
    final var packageId = new PackageId("n", "1.0");
    Assertions.assertThrows(NullPointerException.class, () -> new LocalArchive(packageId, null));
  }
}
