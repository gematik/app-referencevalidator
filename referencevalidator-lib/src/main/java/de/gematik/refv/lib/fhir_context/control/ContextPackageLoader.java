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

import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.exceptions.PackageDownloadException;
import de.gematik.refv.lib.exceptions.PackageLoadFailedException;
import de.gematik.refv.lib.exceptions.SnapshotGenerationException;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.validation.ValidationEngine;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads a given set of {@link ResolvedPackage} in an existing Validation Context. */
final class ContextPackageLoader {

  private static final Logger log = LoggerFactory.getLogger(ContextPackageLoader.class);
  private static final String PACKAGE_FOLDER = "package";

  private ContextPackageLoader() {}

  static void loadPackageInContext(
      @NonNull ValidationEngine validationEngine, @NonNull ResolvedPackage fhirPackage) {
    final var npmPackage = loadAsNpmPackage(validationEngine, fhirPackage);
    try {
      final var pkgId = npmPackage.id();
      final var pkgVersion = npmPackage.version();
      if (validationEngine.getContext().hasPackage(pkgId, pkgVersion)) {
        log.debug("Package {}#{} already in context", pkgId, pkgVersion);
        return;
      }
      log.debug("Loading Package {}#{} in FHIR Context", pkgId, pkgVersion);
      validationEngine.getContext().loadFromPackage(npmPackage, null, false);
    } catch (IOException _) {
      throw new InitializationException("Failed to load package " + fhirPackage.id().coordinates());
    }
  }

  static void loadPackagesInContext(
      @NonNull ValidationEngine validationEngine,
      @NonNull Collection<ResolvedPackage> fhirPackages) {
    if (fhirPackages.isEmpty()) {
      log.debug("No package set supplied, skipping import");
      return;
    }
    for (final var resolvedPackage : fhirPackages) {
      loadPackageInContext(validationEngine, resolvedPackage);
    }
    log.debug("Preparing FHIR Context with loaded dependencies");
  }

  static @NonNull Map<StructureDefinition, Path> extractAndLoadResourcesInContext(
      @NonNull ValidationEngine validationEngine, @NonNull NpmPackage npmPackage)
      throws IOException {
    final var structureDefinitionsFromPackage = new HashMap<StructureDefinition, Path>();
    final var allResourcesInPackage = npmPackage.listResourcesInFolder(PACKAGE_FOLDER);
    for (var packageResource : allResourcesInPackage) {
      final Path currentResourceFilePath =
          Path.of(npmPackage.getPath(), PACKAGE_FOLDER, packageResource);
      if (Files.notExists(currentResourceFilePath)) {
        // Skip files from other folders
        continue;
      }
      try (var inputStream = new FileInputStream(currentResourceFilePath.toFile())) {
        final var parsedPackageResource =
            ResourceParser.parseR4ResourceAsR5(inputStream.readAllBytes());
        if (parsedPackageResource instanceof StructureDefinition sd) {
          structureDefinitionsFromPackage.put(sd, currentResourceFilePath);
        }
        // IMPORTANT: load resource in Context Cache
        validationEngine.getContext().cacheResource(parsedPackageResource);
      } catch (Exception _) {
        log.warn("Skipping not parseable resource {}", packageResource);
      }
    }
    return structureDefinitionsFromPackage;
  }

  static @NonNull String extractProfilePrefixFromStructureDefinition(
      Collection<StructureDefinition> structureDefinitionsFromPackage) {
    var referenceStructureDefinition =
        structureDefinitionsFromPackage.stream()
            .findFirst()
            .orElseThrow(() -> new SnapshotGenerationException("No StructureDefinition found"));
    log.debug("Parsed URL: {}", referenceStructureDefinition.getUrl());
    return getParent(referenceStructureDefinition.getUrl());
  }

  private static String getParent(String resourcePath) {
    return resourcePath.replaceAll("StructureDefinition/.*", "StructureDefinition/");
  }

  /** Import dependencies from the local cache. */
  private static NpmPackage loadAsNpmPackage(
      @NonNull ValidationEngine validationEngine, @NonNull ResolvedPackage fhirPackage) {
    final String packageCoordinates = fhirPackage.id().coordinates();
    try {
      // Load the package directly from directory, if it is an extracted package
      if (Files.exists(fhirPackage.packagePath().resolve(PACKAGE_FOLDER))) {
        return NpmPackage.fromFolder(fhirPackage.packagePath().toString(), false);
      }
      final var packageManager = validationEngine.getPcm();
      log.debug("Accessing the cache folder {}", packageManager.getFolder());
      final var npmPackage =
          packageManager.loadPackageFromCacheOnly(
              fhirPackage.id().name(), fhirPackage.id().version());
      if (Objects.isNull(npmPackage)) {
        throw new PackageDownloadException(
            "The given package is not available locally, so it can't be loaded in context");
      }

      return npmPackage;
    } catch (Exception e) {
      log.error(
          "Could not load the package: {} - cause: {}",
          packageCoordinates,
          e.getLocalizedMessage());
      log.debug(e.getMessage(), e);
      throw new PackageLoadFailedException("Failed to load " + packageCoordinates);
    }
  }
}
