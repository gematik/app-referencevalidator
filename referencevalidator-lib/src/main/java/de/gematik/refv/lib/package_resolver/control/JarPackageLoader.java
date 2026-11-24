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

import de.gematik.refv.lib.exceptions.PackageLoadFailedException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.package_resolver.entity.PackageCategory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.valmodule.boundary.ModuleLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads Core Packages from the embedded reference validator library or modules */
public final class JarPackageLoader {
  private static final Logger log = LoggerFactory.getLogger(JarPackageLoader.class);
  private final Path temporaryDirectory;

  public JarPackageLoader() {
    this(Path.of(System.getProperty("java.io.tmpdir")));
  }

  JarPackageLoader(Path temporaryDirectory) {
    this.temporaryDirectory = Objects.requireNonNull(temporaryDirectory, "temporaryDirectory");
  }

  @NonNull List<PackageCategory> importCorePackagesFromRelease(@NonNull FhirRelease fhirRelease)
      throws PackageLoadFailedException {
    List<PackageCategory> fhirPackages = new ArrayList<>();
    var imported = false;
    try {
      for (var packageEntry : fhirRelease.packages()) {
        final String packageEntryFileName =
            packageEntry.replace(PackageId.PACKAGE_SEPARATOR, LocalArchive.PACKAGE_SEPARATOR)
                + LocalArchive.ARCHIVE_PACKAGE_EXTENSION;
        try (var is = FhirRelease.class.getResourceAsStream("/packages/" + packageEntryFileName)) {
          final LocalArchive tgzFhirPackage = importArchive(is, packageEntryFileName);
          fhirPackages.add(tgzFhirPackage);
        }
      }
      log.debug("Extracted FHIR Packages: {}", fhirPackages.size());
      imported = true;
      return fhirPackages;
    } catch (Exception e) {
      log.error(e.getLocalizedMessage());
      log.debug(e.getLocalizedMessage(), e);
      throw new PackageLoadFailedException("Failed to get Packages Information from Module");
    } finally {
      if (!imported) {
        cleanupTemporaryFiles(fhirPackages);
      }
    }
  }

  @NonNull List<PackageCategory> importPackagesFromModule(@NonNull Path modulePath)
      throws PackageLoadFailedException {
    List<PackageCategory> fhirPackages = new ArrayList<>();
    var imported = false;
    try (JarFile jar = new JarFile(modulePath.toFile())) {
      final var packageEntries =
          jar.stream()
              .filter(entry ->
                  !entry.isDirectory()
                      && entry.getName().startsWith(ModuleLoader.PACKAGE_PATH)
                      && entry.getName().endsWith(LocalArchive.ARCHIVE_PACKAGE_EXTENSION))
              .toList();

      for (var entry : packageEntries) {
        log.debug("Importing package {}", entry.getName());
        try (var is = jar.getInputStream(entry)) {
          final LocalArchive tgzFhirPackage =
              importArchive(is, entry.getName().replace(ModuleLoader.PACKAGE_PATH, ""));
          fhirPackages.add(tgzFhirPackage);
        }
      }

      imported = true;
      return fhirPackages;
    } catch (Exception e) {
      throw new PackageLoadFailedException("Failed to load all the module packages in cache", e);
    } finally {
      if (!imported) {
        cleanupTemporaryFiles(fhirPackages);
      }
    }
  }

  private @NonNull LocalArchive importArchive(InputStream is, String packageEntryFileName)
      throws IOException {
    if (Objects.isNull(is)) {
      throw new PackageLoadFailedException("Failed to import " + packageEntryFileName);
    }
    final var tempFile = Files.createTempFile(temporaryDirectory, "refv-", ".tmp");
    var imported = false;
    try {
      try (var out = Files.newOutputStream(tempFile)) {
        is.transferTo(out);
      }
      final var archive = new LocalArchive(LocalArchive.parse(packageEntryFileName), tempFile);
      imported = true;
      return archive;
    } finally {
      if (!imported) {
        cleanupTemporaryFile(tempFile);
      }
    }
  }

  private static void cleanupTemporaryFiles(List<PackageCategory> packages) {
    packages.stream()
        .filter(LocalArchive.class::isInstance)
        .map(LocalArchive.class::cast)
        .map(LocalArchive::path)
        .forEach(JarPackageLoader::cleanupTemporaryFile);
  }

  private static void cleanupTemporaryFile(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      log.debug("Failed to delete temporary archive {}", path, e);
    }
  }
}
