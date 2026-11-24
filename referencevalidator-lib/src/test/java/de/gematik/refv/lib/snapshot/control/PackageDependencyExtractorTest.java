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

import de.gematik.refv.lib.exceptions.PackageLoadFailedException;
import de.gematik.refv.lib.exceptions.ParsingException;
import de.gematik.refv.lib.package_resolver.entity.LocalDirectory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageDependencyExtractorTest {

  private PackageDependencyExtractor extractor;

  @TempDir Path tempDir;

  @BeforeEach
  void beforeEach() {
    extractor = new PackageDependencyExtractor(tempDir);
  }

  private LocalDirectory packageWithPackageJson(String coordinates, String packageJson)
      throws Exception {
    final var pkgDir = Files.createDirectory(tempDir.resolve(coordinates));
    final var packageFolder = Files.createDirectory(pkgDir.resolve("package"));
    Files.writeString(packageFolder.resolve("package.json"), packageJson);
    return LocalDirectory.parse(coordinates, pkgDir);
  }

  private LocalDirectory packageWithPackageJson(String packageJson) throws Exception {
    final var pkgDir = Files.createDirectory(tempDir.resolve("my.pkg#1.0.0"));
    final var packageFolder = Files.createDirectory(pkgDir.resolve("package"));
    Files.writeString(packageFolder.resolve("package.json"), packageJson);
    return new LocalDirectory(new PackageId("my.pkg", "1.0.0"), pkgDir);
  }

  @DisplayName(
      "Given a package.json with dependencies, when extracting, then the dependencies are returned")
  @Test
  void expectDependenciesExtracted() throws Exception {
    final var pkg =
        packageWithPackageJson(
            "{\"name\":\"my.pkg\",\"version\":\"1.0.0\",\"dependencies\":{\"hl7.fhir.r4.core\":\"4.0.1\"}}");
    final var dependencies = extractor.fromPackage(pkg);
    Assertions.assertEquals(1, dependencies.size());
    Assertions.assertEquals("hl7.fhir.r4.core#4.0.1", dependencies.get(0));
  }

  @DisplayName(
      "Given a package.json without dependencies, when extracting, then an empty list is returned")
  @Test
  void expectNoDependenciesReturnsEmpty() throws Exception {
    final var pkg = packageWithPackageJson("{\"name\":\"my.pkg\",\"version\":\"1.0.0\"}");
    Assertions.assertTrue(extractor.fromPackage(pkg).isEmpty());
  }

  @DisplayName("Given a missing package.json, when extracting, then a ParsingException is thrown")
  @Test
  void expectMissingPackageJsonThrows() throws Exception {
    final var pkgDir = Files.createDirectory(tempDir.resolve("empty.pkg-1.0.0"));
    final var pkg = new LocalDirectory(new PackageId("empty.pkg", "1.0.0"), pkgDir);
    Assertions.assertThrows(ParsingException.class, () -> extractor.fromPackage(pkg));
  }

  @DisplayName("Given a null package, when extracting, then a NullPointerException is thrown")
  @Test
  void expectNullPackageThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> extractor.fromPackage(null));
  }

  @DisplayName(
      "Given multiple packages with versions, when searching with wildcard, return the expected results")
  @Test
  void expectMatchingDependenciesFound() throws Exception {
    packageWithPackageJson(
        "my.pkg#1.0.1",
        "{\"name\":\"my.pkg\",\"version\":\"1.0.1\",\"dependencies\":{\"hl7.fhir.r4.core\":\"4.0.1\"}}");
    packageWithPackageJson(
        "my.pkg#1.1.2",
        "{\"name\":\"my.pkg\",\"version\":\"1.1.2\",\"dependencies\":{\"hl7.fhir.r4.core\":\"4.0.1\"}}");
    packageWithPackageJson(
        "my.pkg#1.1.3",
        "{\"name\":\"my.pkg\",\"version\":\"1.1.3\",\"dependencies\":{\"hl7.fhir.r4.core\":\"4.0.1\"}}");
    packageWithPackageJson(
        "my.pkg#1.2.0",
        "{\"name\":\"my.pkg\",\"version\":\"1.2.0\",\"dependencies\":{\"hl7.fhir.r4.core\":\"4.0.1\"}}");

    final var matchingCoordinates =
        Assertions.assertDoesNotThrow(() -> extractor.getMatchingCoordinates("my.pkg#1.1.x"));
    Assertions.assertEquals("my.pkg#1.1.3", matchingCoordinates.id().coordinates());
    // Expect exceptions for not matching packages
    Assertions.assertThrows(
        PackageLoadFailedException.class, () -> extractor.getMatchingCoordinates("my.pkg#1.3.x"));
    Assertions.assertThrows(
        PackageLoadFailedException.class, () -> extractor.getMatchingCoordinates("my.pkg#1.x.1"));
    Assertions.assertThrows(
        PackageLoadFailedException.class, () -> extractor.getMatchingCoordinates("my.pkg#1.0.0"));
  }
}
