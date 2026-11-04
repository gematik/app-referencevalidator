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
package de.gematik.refv.lib.package_resolver.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.gematik.refv.lib.exceptions.PackageLoadFailedException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.valmodule.boundary.ModuleLoader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageImportLifecycleTest {

  @TempDir Path temporaryDirectory;

  @Test
  void repeatedAndConcurrentImportsExtractOnceAndCleanSuccessfulTemporaryArchives()
      throws Exception {
    var extractionDirectory = Files.createDirectory(temporaryDirectory.resolve("extracted"));
    var modulePath = moduleJar("minimal.example-1.0.0.tgz", packageArchive());
    var loader = spy(new JarPackageLoader(extractionDirectory));
    var resolver = new DefaultPackageResolver(configuration("cache"), loader);

    runConcurrentModuleImports(resolver, modulePath, 6);
    resolver.loadModulePackages(modulePath);
    assertDirectoryEmpty(extractionDirectory);

    resolver.loadCore(FhirRelease.asR4());
    resolver.loadCore(FhirRelease.asR4());

    verify(loader, times(1)).importPackagesFromModule(modulePath);
    verify(loader, times(1)).importCorePackagesFromRelease(FhirRelease.asR4());
    assertDirectoryEmpty(extractionDirectory);
  }

  @Test
  void failedCoreAndModuleResolutionsDeleteImportedArchives() throws Exception {
    var temporaryArchives = Files.createDirectory(temporaryDirectory.resolve("archives"));
    var coreArchive = invalidArchive(temporaryArchives.resolve("core.tgz"));
    var moduleArchive = invalidArchive(temporaryArchives.resolve("module.tgz"));
    var release = FhirRelease.asR4();
    var modulePath = Path.of("module.jar");
    var loader = mock(JarPackageLoader.class);
    var resolver = new DefaultPackageResolver(configuration("cache"), loader);
    org.mockito.Mockito.when(loader.importCorePackagesFromRelease(release))
        .thenReturn(List.of(coreArchive));
    org.mockito.Mockito.when(loader.importPackagesFromModule(modulePath))
        .thenReturn(List.of(moduleArchive));

    assertThrows(PackageLoadFailedException.class, () -> resolver.loadCore(release));
    assertFalse(Files.exists(coreArchive.path()));

    assertThrows(PackageLoadFailedException.class, () -> resolver.loadModulePackages(modulePath));
    assertFalse(Files.exists(moduleArchive.path()));
  }

  @Test
  void partialModuleExtractionDeletesEveryTemporaryArchive() throws Exception {
    var extractionDirectory = Files.createDirectory(temporaryDirectory.resolve("partial"));
    var modulePath = moduleJarWithEntries();
    var loader = new JarPackageLoader(extractionDirectory);

    assertThrows(
        PackageLoadFailedException.class, () -> loader.importPackagesFromModule(modulePath));

    assertDirectoryEmpty(extractionDirectory);
  }

  private void runConcurrentModuleImports(
      DefaultPackageResolver resolver, Path modulePath, int parallelism) throws Exception {
    var ready = new CountDownLatch(parallelism);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(parallelism)) {
      var futures = new ArrayList<Future<?>>();
      for (int index = 0; index < parallelism; index++) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start module import");
                  }
                  resolver.loadModulePackages(modulePath);
                  return null;
                }));
      }
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      futures.forEach(PackageImportLifecycleTest::await);
    }
  }

  private Path moduleJar(String packageFilename, byte[] packageArchive) throws IOException {
    var modulePath = temporaryDirectory.resolve("module.jar");
    try (var output = new JarOutputStream(Files.newOutputStream(modulePath))) {
      output.putNextEntry(new JarEntry(ModuleLoader.PACKAGE_PATH + packageFilename));
      output.write(packageArchive);
      output.closeEntry();
    }
    return modulePath;
  }

  private Path moduleJarWithEntries() throws IOException {
    var modulePath = temporaryDirectory.resolve("partial-module.jar");
    try (var output = new JarOutputStream(Files.newOutputStream(modulePath))) {
      output.putNextEntry(new JarEntry(ModuleLoader.PACKAGE_PATH + "valid-1.0.0.tgz"));
      output.write(new byte[] {1});
      output.closeEntry();
      output.putNextEntry(new JarEntry(ModuleLoader.PACKAGE_PATH + "invalid.tgz"));
      output.write(new byte[] {2});
      output.closeEntry();
    }
    return modulePath;
  }

  private byte[] packageArchive() throws IOException {
    var manifest =
        """
        {"name":"minimal.example","version":"1.0.0","fhirVersions":["4.0.1"],"dependencies":{}}
        """
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var bytes = new ByteArrayOutputStream();
    try (var gzip = new GzipCompressorOutputStream(bytes);
        var archive = new TarArchiveOutputStream(gzip)) {
      var entry = new TarArchiveEntry("package/package.json");
      entry.setSize(manifest.length);
      archive.putArchiveEntry(entry);
      archive.write(manifest);
      archive.closeArchiveEntry();
    }
    return bytes.toByteArray();
  }

  private LocalArchive invalidArchive(Path path) throws IOException {
    Files.write(path, new byte[] {1, 2, 3});
    return new LocalArchive(new PackageId("invalid", "1.0.0"), path);
  }

  private PackageDownloadConfiguration configuration(String cacheDirectory) {
    return new PackageDownloadConfiguration(
        PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED,
        temporaryDirectory.resolve(cacheDirectory));
  }

  private void assertDirectoryEmpty(Path directory) throws IOException {
    try (var files = Files.list(directory)) {
      assertEquals(0, files.count());
    }
  }

  private static void await(Future<?> future) {
    try {
      future.get(10, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new AssertionError("Concurrent package import failed", e);
    }
  }
}
