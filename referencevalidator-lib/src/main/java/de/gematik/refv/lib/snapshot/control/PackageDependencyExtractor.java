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

import de.gematik.refv.lib.config_parser.boundary.JSONMapperProvider;
import de.gematik.refv.lib.exceptions.PackageLoadFailedException;
import de.gematik.refv.lib.exceptions.ParsingException;
import de.gematik.refv.lib.package_resolver.entity.LocalDirectory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Extracts the dependencies of a given package by analyzing its internal package.json file. */
final class PackageDependencyExtractor {
  private static final Logger log = LoggerFactory.getLogger(PackageDependencyExtractor.class);
  public static final String DEPENDENCIES_ENTRY = "dependencies";
  public static final String PACKAGE_JSON_RELATIVE_PATH = "package/package.json";
  private static final String NULL_PACKAGE_MESSAGE = "The package must not be null";
  private final Path cacheFolder;

  PackageDependencyExtractor(@NonNull Path cacheFolder) {
    this.cacheFolder =
        Objects.requireNonNull(cacheFolder, "The cache folder for packages cannot be null");
  }

  @NonNull List<String> fromPackage(@NonNull LocalDirectory fhirPackage) {
    Objects.requireNonNull(fhirPackage, NULL_PACKAGE_MESSAGE);

    final String coordinates = fhirPackage.id().coordinates();
    log.debug("Extracting dependencies from {}", coordinates);
    try {
      final var jsonMapper = JSONMapperProvider.getMapper();
      final var packageJson =
          jsonMapper.readTree(
              fhirPackage.path().resolve(Path.of(PACKAGE_JSON_RELATIVE_PATH)).toFile());

      if (Objects.isNull(packageJson) || packageJson.isNull()) {
        throw new ParsingException("Failed to parse the package.json file");
      }

      final List<String> dependencies = new ArrayList<>();

      if (packageJson.has(DEPENDENCIES_ENTRY)) {
        final var dependencyNode = packageJson.get(DEPENDENCIES_ENTRY);
        dependencyNode.forEachEntry(
            (packageName, jsonNodeVersion) -> {
              final String packageVersion = jsonMapper.convertValue(jsonNodeVersion, String.class);
              dependencies.add(packageName + PackageId.PACKAGE_SEPARATOR + packageVersion);
            });
      }
      log.debug("{} dependencies {}", coordinates, dependencies);
      return dependencies;
    } catch (Exception e) {
      log.debug(e.getLocalizedMessage(), e);
      throw new ParsingException("Failed to extract the dependencies for " + coordinates);
    }
  }

  @NonNull LocalDirectory getMatchingCoordinates(@NonNull String fhirPackageId) {
    Objects.requireNonNull(fhirPackageId, NULL_PACKAGE_MESSAGE);
    final var correctCoordinates = getCoordinates(fhirPackageId);
    return LocalDirectory.parse(correctCoordinates, cacheFolder.resolve(correctCoordinates));
  }

  @NonNull Map<String, String> uniqueDependencies(
      @NonNull Collection<String> dependencies, @NonNull String ownerCoordinates) {
    Map<String, String> unique = new LinkedHashMap<>();
    for (String dependency : dependencies) {
      if (dependency == null || dependency.isBlank()) {
        log.warn("Ignoring blank dependency declared by {}", ownerCoordinates);
      } else {
        String trimmed = dependency.trim();
        if (PackageUtils.sameCoordinates(trimmed, ownerCoordinates)) {
          log.info("Ignoring direct self-dependency {} declared by {}", trimmed, ownerCoordinates);
        } else {
          unique.putIfAbsent(PackageUtils.coordinateKey(trimmed), trimmed);
        }
      }
    }
    return unique;
  }

  private @NonNull String getCoordinates(@NonNull String fhirPackageId) {
    Objects.requireNonNull(fhirPackageId, NULL_PACKAGE_MESSAGE);
    if (fhirPackageId.endsWith("x")) {
      return getWildcardCachedPackageCoordinate(fhirPackageId);
    }
    return fhirPackageId;
  }

  /**
   * Returns the version‑matched file with the highest semantic version for a cached package.
   *
   * @param fhirPackageId the package to locate in the cache
   * @return name of the cached directory of the newest matching package, {@code null} if none found
   * @throws PackageLoadFailedException in case no match has been found
   */
  private @NonNull String getWildcardCachedPackageCoordinate(@NonNull String fhirPackageId)
      throws PackageLoadFailedException {
    final var packageValues = fhirPackageId.split(PackageId.PACKAGE_SEPARATOR);
    final String packageName = packageValues[0];
    final String packageVersion = packageValues[1];
    final File folder = cacheFolder.toFile();
    final File[] files =
        folder.listFiles((_, name) -> name.matches(packageName + "#\\d+\\.\\d+\\.\\d+"));

    String highestVersionFilename = "";
    PackageId highestVersion = null;

    if (files != null) {
      for (File file : files) {
        String filename = file.getName();
        final var filenameParts = filename.split(PackageId.PACKAGE_SEPARATOR);
        if (filenameParts.length != 2) {
          continue;
        }

        String versionString = filenameParts[1];
        PackageId version = new PackageId(packageName, versionString);

        if (matchesWildcard(packageVersion, versionString)
            && (highestVersion == null || version.compareTo(highestVersion) > 0)) {
          highestVersion = version;
          highestVersionFilename = filename;
        }
      }
    }

    if (highestVersionFilename.isBlank()) {
      throw new PackageLoadFailedException(
          "Failed to find a matching version for " + fhirPackageId);
    }
    return highestVersionFilename;
  }

  /**
   * Matches the version of a {@link PackageId} with a provided wildcard string.
   *
   * @param fhirPackageVersion the FHIR package version to compare
   * @param wildcardVersion the wild card version to match
   * @return true if the match is found, false otherwise
   */
  private boolean matchesWildcard(
      @NonNull String fhirPackageVersion, @Nullable String wildcardVersion) {
    if (Objects.isNull(wildcardVersion) || wildcardVersion.isBlank()) {
      return false;
    }
    return fhirPackageVersion.startsWith(
        wildcardVersion.substring(0, wildcardVersion.length() - 1));
  }
}
