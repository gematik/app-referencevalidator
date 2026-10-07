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
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.package_resolver.entity.LocalDirectory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.PackageResolution;
import de.gematik.refv.lib.package_resolver.entity.PackageResolutionRequest;
import de.gematik.refv.lib.package_resolver.entity.RemotePackage;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PackageResolverTest {

  private static final Logger log = LoggerFactory.getLogger(PackageResolverTest.class);
  @TempDir private Path tempDir;

  @DisplayName("Checks that a path containing a remote package can be loaded with online policy")
  @Test
  void checksThatPathWithRemotePackageCanBeLoaded() {
    final var packageId = new PackageId("de.gematik.epa.medication", "1.3.4");
    final var packageConfiguration =
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED, tempDir);
    final PackageResolver resolver = PackageResolverFactory.withConfiguration(packageConfiguration);
    final var result =
        Assertions.assertDoesNotThrow(() -> resolver.resolvePath(Path.of(packageId.coordinates())));
    printResolution(result);
  }

  @DisplayName("Checks that a remote package cannot be loaded with the offline policy")
  @Test
  void checksThatARemotePackageCannotBeLoadedWhenOffline() {
    final var packageConfiguration =
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED, tempDir);
    final PackageResolver resolver = PackageResolverFactory.withConfiguration(packageConfiguration);

    final PackageResolutionRequest request =
        new PackageResolutionRequest(
            List.of(new RemotePackage(new PackageId("de.basisprofil.r4", "1.6.0"))));

    Assertions.assertThrows(
        PackageLoadFailedException.class,
        () -> resolver.resolveRequest(request, PackageResolver.TransitiveResolution.IGNORE));
  }

  @DisplayName("Checks that a remote package is loaded with all the transitive dependencies")
  @Test
  void checksThatARemotePackageIsLoadedWithAllTheTransitiveDependencies() {
    final var packageConfiguration =
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED, tempDir);

    final PackageResolver resolver = PackageResolverFactory.withConfiguration(packageConfiguration);
    final var packageId = new PackageId("de.basisprofil.r4", "1.6.0");
    final PackageResolutionRequest request =
        new PackageResolutionRequest(List.of(new RemotePackage(packageId)));

    final var result =
        Assertions.assertDoesNotThrow(
            () -> resolver.resolveRequest(request, PackageResolver.TransitiveResolution.ALLOWED));

    printResolution(result);
    Assertions.assertTrue(result.find(packageId).isPresent());
  }

  @DisplayName("Checks that a remote package is loaded only with its direct dependencies")
  @Test
  void checksThatARemotePackageIsLoadedOnlyWithDirectDependencies() {
    final var packageConfiguration =
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED, tempDir);

    final PackageResolver resolver = PackageResolverFactory.withConfiguration(packageConfiguration);
    final var packageId = new PackageId("de.basisprofil.r4", "1.6.0");
    final PackageResolutionRequest request =
        new PackageResolutionRequest(
            List.of(new RemotePackage(new PackageId("de.basisprofil.r4", "1.6.0"))));

    final var result =
        Assertions.assertDoesNotThrow(
            () -> resolver.resolveRequest(request, PackageResolver.TransitiveResolution.IGNORE));

    printResolution(result);
    Assertions.assertTrue(result.find(packageId).isPresent());
  }

  @DisplayName(
      "Checks that a TGZ package is loaded with all the direct dependencies in offline mode")
  @Test
  void checksThatATGZPackageIsLoadedWithAllTheTransitiveDependenciesWhenOffline() {
    final var packageConfiguration =
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED, tempDir);

    final PackageResolver resolver = PackageResolverFactory.withConfiguration(packageConfiguration);

    final var packageId = new PackageId("de.abda.erezeptabgabedaten", "1.5.0");
    final PackageResolutionRequest request =
        new PackageResolutionRequest(
            List.of(
                new LocalArchive(
                    packageId,
                    Path.of(
                        "src/test/resources/packages/erezept/minimal/de.abda.erezeptabgabedaten-1.5.0.tgz")),
                new LocalArchive(
                    new PackageId("de.abda.erezeptabgabedaten", "1.5.0"),
                    Path.of(
                        "src/test/resources/packages/erezept/minimal/de.abda.erezeptabgabedatenbasis-1.5.0.tgz")),
                new LocalArchive(
                    new PackageId("de.basisprofil.r4", "1.5.2"),
                    Path.of(
                        "src/test/resources/packages/erezept/minimal/de.basisprofil.r4-1.5.2.tgz")),
                new LocalArchive(
                    new PackageId("hl7.fhir.r4.core", "4.0.1"),
                    Path.of(
                        "src/test/resources/packages/erezept/minimal/hl7.fhir.r4.core-4.0.1.tgz"))));

    final var result =
        Assertions.assertDoesNotThrow(
            () -> resolver.resolveRequest(request, PackageResolver.TransitiveResolution.IGNORE));

    printResolution(result);
    Assertions.assertTrue(result.find(packageId).isPresent());
  }

  @DisplayName(
      "Checks that a Directory package is loaded with all the transitive direct dependencies")
  @Test
  void checksThatADirectoryPackageIsLoadedWithAllTheTransitiveDependencies() {
    final var packageConfiguration =
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED, tempDir);

    final PackageResolver resolver = PackageResolverFactory.withConfiguration(packageConfiguration);

    final var packageId = new PackageId("minimal.example", "1.0.0");
    final PackageResolutionRequest request =
        new PackageResolutionRequest(
            List.of(
                new LocalDirectory(
                    packageId, Path.of("src/test/resources/packages/minimal.example#1.0.0"))));

    final var result =
        Assertions.assertDoesNotThrow(
            () -> resolver.resolveRequest(request, PackageResolver.TransitiveResolution.IGNORE));

    printResolution(result);
    Assertions.assertTrue(result.find(packageId).isPresent());
  }

  @DisplayName("Checks that core packages can be loaded for the given FHIR Version")
  @Test
  void checksThatCorePackagesCanBeLoaded() {
    final var packageConfiguration =
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED, tempDir);

    final PackageResolver resolver = PackageResolverFactory.withConfiguration(packageConfiguration);
    final var resultR4 = Assertions.assertDoesNotThrow(() -> resolver.loadCore(FhirRelease.asR4()));
    printResolution(resultR4);

    final var resultR5 = Assertions.assertDoesNotThrow(() -> resolver.loadCore(FhirRelease.asR5()));
    printResolution(resultR5);
  }

  private void printResolution(@NonNull PackageResolution resolution) {
    Assertions.assertNotNull(resolution);
    Assertions.assertFalse(resolution.packages().isEmpty());
    for (var entry : resolution.packages()) {
      log.info(
          "### {} : {} - {}", entry.id().coordinates(), entry.dependencies(), entry.packagePath());
    }
  }
}
