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
package de.gematik.refv.lib.package_resolver.boundary;

import de.gematik.refv.lib.exceptions.PackageLoadFailedException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.package_resolver.entity.PackageCategory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.PackageResolutionRequest;
import de.gematik.refv.lib.package_resolver.entity.RemotePackage;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PackageResolverRequirementsTest {

  private static final Path ARCHIVE_DIRECTORY =
      Path.of("src/test/resources/packages/erezept/minimal");
  private static final Path MINIMAL_PACKAGE_DIRECTORY =
      Path.of("src/test/resources/packages/minimal.example#1.0.0");
  private static final String CORE_COORDINATES = "hl7.fhir.r4.core#4.0.1";

  @TempDir Path temporaryDirectory;

  /// Requirement `R1.1` Requirement `R1.2` Requirement `R1.3`
  @ParameterizedTest(name = "{0}")
  @MethodSource("requestCases")
  void requestResolutionFollowsTransitivityAndDownloadPolicy(
      String requirementLabel,
      String coordinates,
      PackageResolver.TransitiveResolution transitiveResolution,
      boolean seedPackages,
      boolean expectFailure) {
    var resolver = resolver();
    if (seedPackages) {
      resolver.resolvePath(ARCHIVE_DIRECTORY);
    }
    var request =
        new PackageResolutionRequest(List.of(new RemotePackage(PackageId.parse(coordinates))));

    if (expectFailure) {
      Assertions.assertThrows(
          PackageLoadFailedException.class,
          () -> resolver.resolveRequest(request, transitiveResolution));
      return;
    }

    var result = resolver.resolveRequest(request, transitiveResolution);
    var resolvedIds = result.packages().stream().map(pkg -> pkg.id().coordinates()).toList();
    Assertions.assertTrue(resolvedIds.contains(coordinates));
    if (transitiveResolution == PackageResolver.TransitiveResolution.ALLOWED) {
      Assertions.assertTrue(
          result.packages().stream()
              .allMatch(
                  pkg ->
                      pkg.dependencies().stream()
                          .allMatch(
                              dependency ->
                                  resolvedIds.indexOf(dependency.coordinates()) >= 0
                                      && resolvedIds.indexOf(dependency.coordinates())
                                          < resolvedIds.indexOf(pkg.id().coordinates()))),
          "Every resolved dependency must precede its dependent package");
    }
    if (transitiveResolution == PackageResolver.TransitiveResolution.IGNORE) {
      Assertions.assertEquals(List.of(coordinates), resolvedIds);
    } else {
      Assertions.assertTrue(result.packages().size() > 1);
    }
  }

  /// Requirement `R2.1`
  @Test
  @DisplayName("R2.1 resolve-list resolves every supplied local archive")
  void resolveListReturnsEachSuppliedPackage() {
    var resolver = resolver();
    var coreArchive = ARCHIVE_DIRECTORY.resolve("hl7.fhir.r4.core-4.0.1.tgz");
    var baseArchive = ARCHIVE_DIRECTORY.resolve("de.basisprofil.r4-1.5.2.tgz");

    var result = resolver.resolveList(List.of(coreArchive.toString(), baseArchive.toString()));
    var coordinates = result.packages().stream().map(pkg -> pkg.id().coordinates()).toList();

    Assertions.assertEquals(List.of(CORE_COORDINATES, "de.basisprofil.r4#1.5.2"), coordinates);
  }

  /// Requirement `R3.1` Requirement `R3.2` Requirement `R3.3` Requirement `R3.4`
  @ParameterizedTest(name = "{0}")
  @MethodSource("pathCases")
  void pathResolutionRecognizesPackageSourceForms(
      String requirementLabel,
      Path input,
      Preparation preparation,
      String expectedCoordinate,
      int minimumResultSize) {
    var resolver = resolver();
    switch (preparation) {
      case CORE -> resolver.loadCore(FhirRelease.asR4());
      case ARCHIVES -> resolver.resolvePath(ARCHIVE_DIRECTORY);
      case NONE -> { // No resolver state needs to be prepared for this case.
      }
    }

    var result = resolver.resolvePath(input);
    var coordinates = result.packages().stream().map(pkg -> pkg.id().coordinates()).toList();

    Assertions.assertTrue(coordinates.contains(expectedCoordinate));
    Assertions.assertTrue(coordinates.size() >= minimumResultSize);
  }

  /// Requirement `R4.1` Requirement `R4.2`
  @ParameterizedTest(name = "{0}")
  @MethodSource("matchingPackageCases")
  void matchingPackageReturnsOnePackage(String requirementLabel, String coordinate) {
    var resolver = resolver();
    resolver.loadCore(FhirRelease.asR4());

    PackageCategory matchingPackage = resolver.getMatchingPackage(coordinate);

    Assertions.assertEquals(PackageId.parse(CORE_COORDINATES), matchingPackage.id());
  }

  /// Requirement `R5.1`
  @Test
  @DisplayName("R5.1 core packages are available for the requested FHIR release")
  void loadCoreReturnsPackagesForR4AndR5() {
    var resolver = resolver();

    var r4Packages = resolver.loadCore(FhirRelease.asR4());
    var r5Packages = resolver.loadCore(FhirRelease.asR5());

    Assertions.assertTrue(
        FhirRelease.asR4().packages().stream()
            .map(PackageId::parse)
            .allMatch(id -> r4Packages.find(id).isPresent()));
    Assertions.assertTrue(
        FhirRelease.asR5().packages().stream()
            .map(PackageId::parse)
            .allMatch(id -> r5Packages.find(id).isPresent()));
  }

  /// Requirement `R6.1`
  @Test
  @DisplayName("R6.1 load-module-packages makes embedded module packages available offline")
  void loadModulePackagesMakesEmbeddedPackagesAvailable() {
    var resolver = resolver();

    resolver.loadModulePackages(Path.of("src/test/resources/modules/isik.jar"));

    Assertions.assertEquals(
        PackageId.parse("de.gematik.isik#5.1.3"),
        resolver.getMatchingPackage("de.gematik.isik#5.1.3").id());
  }

  /// Requirement `R7.1`
  @Test
  @DisplayName("R7.1 cache-path reports the configured package cache")
  void cachePathIsTheConfiguredPath() {
    var resolver = resolver();

    Assertions.assertEquals(temporaryDirectory, resolver.cachePath());
  }

  private PackageResolver resolver() {
    return PackageResolverFactory.withConfiguration(
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED, temporaryDirectory));
  }

  private enum Preparation {
    NONE,
    CORE,
    ARCHIVES
  }

  private static Stream<Arguments> requestCases() {
    return Stream.of(
        Arguments.of(
            "R1.1 — transitive packages are resolved dependency-first",
            "de.abda.erezeptabgabedaten#1.5.0",
            PackageResolver.TransitiveResolution.ALLOWED,
            true,
            false),
        Arguments.of(
            "R1.2 — only explicit roots are resolved when transitive resolution is ignored",
            "de.basisprofil.r4#1.5.2",
            PackageResolver.TransitiveResolution.IGNORE,
            true,
            false),
        Arguments.of(
            "R1.3 — unavailable packages fail when downloads are disabled",
            "missing.package#1.0.0",
            PackageResolver.TransitiveResolution.IGNORE,
            false,
            true));
  }

  private static Stream<Arguments> matchingPackageCases() {
    return Stream.of(
        Arguments.of("R4.1 exact coordinates return the package", CORE_COORDINATES),
        Arguments.of(
            "R4.2 wildcard coordinates return a matching package", "hl7.fhir.r4.core#4.0.x"));
  }

  private static Stream<Arguments> pathCases() {
    return Stream.of(
        Arguments.of(
            "R3.1 — a local archive resolves as one package",
            ARCHIVE_DIRECTORY.resolve("hl7.fhir.r4.core-4.0.1.tgz"),
            Preparation.NONE,
            CORE_COORDINATES,
            1),
        Arguments.of(
            "R3.2 — a coordinate-named package directory resolves with its dependencies",
            MINIMAL_PACKAGE_DIRECTORY,
            Preparation.CORE,
            "minimal.example#1.0.0",
            2),
        Arguments.of(
            "R3.3 — a directory of archives resolves its directly contained packages",
            ARCHIVE_DIRECTORY,
            Preparation.NONE,
            "de.abda.erezeptabgabedaten#1.5.0",
            4),
        Arguments.of(
            "R3.4 — a non-existing name-version path is treated as a remote coordinate",
            Path.of("de.abda.erezeptabgabedaten#1.5.0"),
            Preparation.ARCHIVES,
            "de.abda.erezeptabgabedaten#1.5.0",
            2));
  }
}
