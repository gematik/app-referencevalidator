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

import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.exceptions.PackageDownloadException;
import de.gematik.refv.lib.exceptions.PackageLoadFailedException;
import de.gematik.refv.lib.exceptions.PackageParsingFailedException;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.package_resolver.entity.LocalDirectory;
import de.gematik.refv.lib.package_resolver.entity.PackageCategory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.RemotePackage;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs the installation of FHIR Packages into the provided cache path and parses the manifest
 * of the given dependencies.
 */
final class FilesystemPackageSourceProvider implements PackageSourceProvider {

  private static final Logger log = LoggerFactory.getLogger(FilesystemPackageSourceProvider.class);

  private final PackageDownloadConfiguration configuration;
  private final FilesystemPackageCacheManager packageCacheManager;
  private final boolean canAccessRemoteRegistries;

  /**
   * Implements the Source of FHIR Packages based on HL7 FileSystem Cache Manager.
   *
   * @param configuration the configuration for downloading the packages
   */
  public FilesystemPackageSourceProvider(@NonNull PackageDownloadConfiguration configuration) {
    this.configuration =
        Objects.requireNonNull(configuration, "The package configuration must be defined");
    this.canAccessRemoteRegistries =
        configuration
            .remoteDownloadPolicy()
            .equals(PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED);
    try {
      // BUG in HL7: it requires that a folder exists, instead of creating it internally.
      if (!Files.exists(configuration.cachePath())) {
        Files.createDirectories(configuration.cachePath());
      }
      this.packageCacheManager =
          new FilesystemPackageCacheManager.Builder()
              .withCacheFolder(this.configuration.cachePath().toString())
              .build();
    } catch (Exception e) {
      throw new InitializationException("Failed to initialize the package repository", e);
    }
  }

  @Override
  public @NonNull Path location() {
    return configuration.cachePath();
  }

  /**
   * Returns whether a FHIR Package is installed or not.
   *
   * @param packageId the {@link PackageId} instance to check
   * @return true if exists, false otherwise
   */
  @Override
  public boolean packageInstalled(@NonNull PackageId packageId) {
    Objects.requireNonNull(packageId, "The package id must not be null");

    return packageCacheManager.packageInstalled(packageId.name(), packageId.version());
  }

  @Override
  public @NonNull ResolvedPackage loadPackage(@NonNull PackageCategory packageRequirement)
      throws PackageLoadFailedException {
    Objects.requireNonNull(packageRequirement, "The package requirement must not be null");
    if (log.isDebugEnabled()) {
      log.debug("Loading {}", packageRequirement.id().coordinates());
    }
    return switch (packageRequirement) {
      case RemotePackage exact -> loadRemote(exact);
      case LocalDirectory localDirectory -> loadDirectory(localDirectory);
      case LocalArchive localArchive -> loadArchive(localArchive);
    };
  }

  private @NonNull ResolvedPackage loadRemote(@NonNull RemotePackage exact)
      throws PackageLoadFailedException {
    final String packageCoordinates = exact.id().coordinates();

    if (packageInstalled(exact.id())) {
      log.debug("Loading Package {} from cache", packageCoordinates);
      return getInstalledPackage(exact.id());
    }

    try {
      log.info("Loading package {}", packageCoordinates);
      NpmPackage npmPackage;
      if (!canAccessRemoteRegistries) {
        log.warn("Cannot download package {} in offline mode", packageCoordinates);
        npmPackage =
            packageCacheManager.loadPackageFromCacheOnly(exact.id().name(), exact.id().version());
        if (Objects.isNull(npmPackage)) {
          final var latestVersion =
              packageCacheManager.getLatestVersionFromCache(
                  exact.id().name(),
                  exact.id().version().substring(0, exact.id().version().lastIndexOf(".")) + ".x");
          log.debug("Latest version found: {}", latestVersion);
          if (Objects.isNull(latestVersion) || latestVersion.isBlank()) {
            throw new PackageDownloadException(
                String.format("Latest package %s not available in cache", packageCoordinates));
          }
          npmPackage =
              packageCacheManager.loadPackageFromCacheOnly(exact.id().name(), latestVersion);
        }
        if (Objects.isNull(npmPackage)) {
          throw new PackageDownloadException(
              String.format("Package %s not available in cache", packageCoordinates));
        }
      } else { // Imports the package directly in cache
        npmPackage = packageCacheManager.loadPackage(exact.id().name(), exact.id().version());
      }
      return toResolvedPackage(npmPackage);
    } catch (Exception exception) {
      throw new PackageLoadFailedException(
          "Could not load the FHIR package: " + packageCoordinates, exception);
    }
  }

  private @NonNull ResolvedPackage loadDirectory(@NonNull LocalDirectory localDirectory)
      throws PackageLoadFailedException {
    final String packageCoordinates = localDirectory.id().coordinates();

    if (packageInstalled(localDirectory.id())) {
      log.debug("Loading local directory package {} from cache", packageCoordinates);
      return getInstalledPackage(localDirectory.id());
    }

    try {
      log.debug("Importing extracted package {}", packageCoordinates);
      final var npmPackage = NpmPackage.fromFolder(localDirectory.path().toString());
      if (Objects.isNull(npmPackage)) {
        throw new PackageParsingFailedException("Failed to parse the directory as NPM package");
      }

      final var finalCachePath = this.configuration.cachePath().resolve(packageCoordinates);
      log.debug("Moving {} to final path {}", localDirectory.path(), finalCachePath);
      FileUtils.copyDirectory(localDirectory.path().toFile(), finalCachePath.toFile(), true);
      return toResolvedPackage(npmPackage);
    } catch (Exception exception) {
      throw new PackageLoadFailedException(
          "Failed to download FHIR package " + packageCoordinates, exception);
    }
  }

  private @NonNull ResolvedPackage loadArchive(@NonNull LocalArchive localArchive)
      throws PackageLoadFailedException {
    final String packageCoordinates = localArchive.id().coordinates();
    if (packageInstalled(localArchive.id())) {
      log.debug("Loading archived package {} from cache", packageCoordinates);
      return getInstalledPackage(localArchive.id());
    }

    try (var inputStream = new FileInputStream(localArchive.path().toFile())) {
      log.debug(
          "Importing archived package {}#{}",
          localArchive.id().name(),
          localArchive.id().version());
      // Use the package-cache manager to preserve its package metadata and cache handling.
      final var npmPackage =
          packageCacheManager.addPackageToCache(
              localArchive.id().name(),
              localArchive.id().version(),
              inputStream,
              localArchive.id().coordinates());
      return toResolvedPackage(npmPackage);
    } catch (Exception exception) {
      throw new PackageLoadFailedException(
          "Failed to import FHIR package " + packageCoordinates, exception);
    }
  }

  private @NonNull ResolvedPackage getInstalledPackage(@NonNull PackageId packageId) {
    try {
      final NpmPackage npmPackage =
          packageCacheManager.loadPackageFromCacheOnly(packageId.name(), packageId.version());
      return toResolvedPackage(npmPackage);
    } catch (IOException _) {
      throw new PackageLoadFailedException("Failed to resolve package " + packageId.coordinates());
    }
  }

  private @NonNull ResolvedPackage toResolvedPackage(@NonNull NpmPackage npmPackage) {
    Objects.requireNonNull(npmPackage, "The NPM Package cannot be null");

    final var packageId = new PackageId(npmPackage.name(), npmPackage.version());
    final Path canonicalCacheDirectory =
        this.configuration.cachePath().resolve(packageId.coordinates());

    final List<PackageId> directDependencies = new ArrayList<>();
    for (var dependency : npmPackage.dependencies()) {
      directDependencies.add(PackageId.parse(dependency));
    }
    log.debug("Imported dependencies: {}", directDependencies.size());

    return new ResolvedPackage(packageId, canonicalCacheDirectory, directDependencies);
  }
}
