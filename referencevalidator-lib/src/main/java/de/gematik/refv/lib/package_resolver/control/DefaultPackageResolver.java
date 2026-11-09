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
import de.gematik.refv.lib.exceptions.SnapshotGenerationException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolver;
import de.gematik.refv.lib.package_resolver.entity.DependencyGraph;
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.package_resolver.entity.LocalDirectory;
import de.gematik.refv.lib.package_resolver.entity.PackageCategory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.PackageResolution;
import de.gematik.refv.lib.package_resolver.entity.PackageResolutionRequest;
import de.gematik.refv.lib.package_resolver.entity.RemotePackage;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Controls how the FHIR Packages are resolved */
// Resolved-package memoization is scoped to this resolver instance.
public final class DefaultPackageResolver implements PackageResolver {
  private static final Logger log = LoggerFactory.getLogger(DefaultPackageResolver.class);
  private final PackageSourceProvider packageSource;
  private final JarPackageLoader corePackageLoader;
  private final ConcurrentHashMap<String, PackageResolution> cachedResolved =
      new ConcurrentHashMap<>();

  /**
   * Creates a package resolver.
   *
   * <p>Package sources are evaluated in list order. The First source that reports support for a
   * root requirement is user for that requirement.
   */
  public DefaultPackageResolver(
      @NonNull PackageDownloadConfiguration packageDownloadConfiguration) {
    this(packageDownloadConfiguration, new JarPackageLoader());
  }

  DefaultPackageResolver(
      @NonNull PackageDownloadConfiguration packageDownloadConfiguration,
      @NonNull JarPackageLoader corePackageLoader) {
    Objects.requireNonNull(
        packageDownloadConfiguration, "The context configuration cannot be null");

    this.packageSource = new FilesystemPackageSourceProvider(packageDownloadConfiguration);
    this.corePackageLoader = Objects.requireNonNull(corePackageLoader, "corePackageLoader");
  }

  @Override
  public @NonNull Path cachePath() {
    return this.packageSource.location();
  }

  /**
   * Resolves the requested packages and all transitive dependencies, if configured.
   *
   * @param request package-resolution request
   * @return dependency-first package resolution
   * @throws PackageLoadFailedException in case of errors
   */
  public @NonNull PackageResolution resolveRequest(
      @NonNull PackageResolutionRequest request, @NonNull TransitiveResolution transitiveResolution)
      throws PackageLoadFailedException {
    Objects.requireNonNull(request, "The package resolution request cannot be null");
    if (TransitiveResolution.IGNORE.equals(transitiveResolution)) {
      final var resolvedPackageList =
          request.requirements().stream().map(packageSource::loadPackage).distinct().toList();
      log.debug("Resolved Packages: {}", resolvedPackageList.size());
      return new PackageResolution(resolvedPackageList);
    }

    return resolveTransitively(request);
  }

  /**
   * Resolves all supplied roots and their transitive dependencies as one graph.
   *
   * <p>Each concrete package coordinate is returned once in dependency-first order.
   *
   * @param packagesToResolve a collection of Packages (path to local directory or archive, remote
   *     coordinates)
   * @return resolved packages in dependency-first order
   * @throws PackageLoadFailedException if resolution fails
   */
  @Override
  public @NonNull PackageResolution resolveList(@NonNull Collection<String> packagesToResolve)
      throws PackageLoadFailedException {
    Objects.requireNonNull(packagesToResolve, "The list of packages to resolve cannot be null");
    if (packagesToResolve.isEmpty()) {
      return new PackageResolution(List.of());
    }
    List<PackageCategory> requirements = new ArrayList<>();
    for (var packageEntry : packagesToResolve) {
      requirements.addAll(discoverSources(Path.of(packageEntry)).requirements());
    }
    return resolveRequest(new PackageResolutionRequest(requirements), TransitiveResolution.ALLOWED);
  }

  @Override
  public @NonNull PackageResolution resolvePath(@NonNull Path inputPath)
      throws PackageLoadFailedException {
    var sources = discoverSources(inputPath);
    var transitiveResolution =
        sources.requirements().stream().anyMatch(source -> !(source instanceof LocalArchive))
            ? TransitiveResolution.ALLOWED
            : TransitiveResolution.IGNORE;
    return resolveRequest(sources, transitiveResolution);
  }

  @Override
  public @NonNull PackageResolution resolveSources(@NonNull Path inputPath)
      throws PackageLoadFailedException {
    var sources =
        discoverSources(inputPath).requirements().stream()
            .map(source ->
                source instanceof LocalDirectory(PackageId id, Path path)
                    ? new ResolvedPackage(id, path, List.of())
                    : packageSource.loadPackage(source))
            .toList();
    return new PackageResolution(sources);
  }

  @Override
  public @NonNull PackageCategory getMatchingPackage(@NonNull String packageCoordinates)
      throws PackageLoadFailedException {
    if (Objects.requireNonNull(packageCoordinates, "Package coordinates cannot be null")
        .isBlank()) {
      throw new IllegalArgumentException("Package coordinates cannot be blank");
    }
    final String requested = packageCoordinates.trim();
    try {
      var resolution =
          resolveRequest(
              PackageResolutionRequest.of(RemotePackage.parse(requested)),
              PackageResolver.TransitiveResolution.IGNORE);
      if (resolution.packages().isEmpty()) {
        throw new SnapshotGenerationException("Could not resolve dependency " + requested);
      }
      var matches =
          resolution.packages().stream()
              .filter(candidate -> coordinatesMatch(requested, candidate.id().coordinates()))
              .toList();
      if (matches.size() == 1) {
        var resolved = matches.getFirst();
        return new LocalDirectory(resolved.id(), resolved.packagePath());
      }
      if (matches.size() > 1) {
        throw new SnapshotGenerationException(
            "Dependency resolution returned multiple exact matches for " + requested);
      }
      if (resolution.packages().size() == 1) {
        var resolved = resolution.packages().getFirst();
        String resolvedCoordinates = resolved.id().coordinates();
        if (isNewerPatchVersion(requested, resolvedCoordinates)) {
          log.debug(
              "No exact coordinate match for {}. Using compatible cached package {}",
              requested,
              resolvedCoordinates);
          return new LocalDirectory(resolved.id(), resolved.packagePath());
        }
      }

      final var candidates =
          resolution.packages().stream()
              .map(candidate -> candidate.id().coordinates())
              .collect(Collectors.joining(", "));
      throw new SnapshotGenerationException(
          "Dependency "
              + requested
              + " did not resolve to an exact matching package; candidates: "
              + candidates);
    } catch (Exception e) {
      throw new SnapshotGenerationException(
          "Failed to resolve dependency " + requested + ": " + e.getLocalizedMessage(), e);
    }
  }

  @Override
  public @NonNull PackageResolution loadCore(@NonNull FhirRelease fhirRelease)
      throws PackageLoadFailedException {
    Objects.requireNonNull(fhirRelease, "The FHIR Release cannot be null");
    final String fhirAlias = fhirRelease.alias();
    return cachedResolved.computeIfAbsent(
        fhirAlias,
        _ -> {
          final var packagesList = corePackageLoader.importCorePackagesFromRelease(fhirRelease);
          try {
            return resolveRequest(
                new PackageResolutionRequest(packagesList), TransitiveResolution.IGNORE);
          } finally {
            cleanupTemporaryFiles(packagesList);
          }
        });
  }

  @Override
  public void loadModulePackages(@NonNull Path modulePath) {
    Objects.requireNonNull(modulePath, "The module path cannot be null");
    cachedResolved.computeIfAbsent(
        modulePath.toString(),
        _ -> {
          final var packagesList = corePackageLoader.importPackagesFromModule(modulePath);
          try {
            return resolveRequest(
                new PackageResolutionRequest(packagesList), TransitiveResolution.IGNORE);
          } finally {
            cleanupTemporaryFiles(packagesList);
          }
        });
  }

  private PackageResolution resolveTransitively(PackageResolutionRequest request) {
    log.debug("Resolving packages transitively");
    var all = new LinkedHashMap<PackageId, ResolvedPackage>();
    var resolvedRequirements = new LinkedHashMap<PackageId, ResolvedPackage>();
    var pending = new ArrayDeque<ResolvedPackage>();
    var graph = new DependencyGraph();
    for (var requirement : request.requirements()) {
      final var resolved = packageSource.loadPackage(requirement);
      resolvedRequirements.putIfAbsent(requirement.id(), resolved);
      addToProcessingQueue(resolved, all, pending);
      graph.addNode(resolved.id());
    }
    while (!pending.isEmpty()) {
      var dependency = pending.remove();
      for (var transitive : dependency.dependencies()) {
        var resolved = all.get(transitive);
        if (resolved == null) {
          resolved =
              resolvedRequirements.computeIfAbsent(
                  transitive, id -> packageSource.loadPackage(new RemotePackage(id)));
          addToProcessingQueue(resolved, all, pending);
        }
        graph.addDependency(resolved.id(), dependency.id());
      }
    }

    var ordered =
        graph.dependencyFirstOrder().stream()
            .map(all::get)
            .map(resolvedPackage ->
                new ResolvedPackage(
                    resolvedPackage.id(),
                    resolvedPackage.packagePath(),
                    resolvedPackage.dependencies()))
            .toList();
    log.debug("Found resolve packages {}", ordered.size());
    return new PackageResolution(ordered);
  }

  private static void addToProcessingQueue(
      ResolvedPackage resolvedPackage,
      Map<PackageId, ResolvedPackage> all,
      Deque<ResolvedPackage> packageDeque) {
    if (Objects.isNull(all.putIfAbsent(resolvedPackage.id(), resolvedPackage))) {
      packageDeque.add(resolvedPackage);
    }
  }

  /**
   * Resolves the given input into a set of {@link PackageCategory} instances.
   *
   * <ul>
   *   <li>a directory is expanded into one {@link LocalDirectory} for a single folder or one {@link
   *       LocalArchive} per {@code .tgz} file directly contained in it
   *   <li>a path pointing to a single {@code .tgz} file yields exactly one {@link LocalArchive}
   *   <li>a "id#packageVersion" coordinate (not an existing file/directory) yields exactly one
   *       {@link RemotePackage}
   * </ul>
   *
   * @param inputPath the raw input to classify
   * @return the resolved, explicit set of {@link ResolvedPackage} instances (possibly empty, if a
   *     directory does not contain any package)
   */
  private @NonNull PackageResolutionRequest discoverSources(@NonNull Path inputPath) {
    Objects.requireNonNull(inputPath, "The path cannot be null");
    final var pathAsFile = inputPath.toFile();
    final var pathFileName = pathAsFile.getName();
    if (!pathAsFile.exists()) {
      if (pathFileName.contains(PackageId.PACKAGE_SEPARATOR)
          && !pathFileName.endsWith(LocalArchive.ARCHIVE_PACKAGE_EXTENSION)) {
        log.debug("Detected remote package coordinate format");
        return PackageResolutionRequest.of(
            new RemotePackage(PackageId.parse(inputPath.toString())));
      }
      throw new IllegalArgumentException("The given path does not exist: " + inputPath);
    }

    if (pathAsFile.isDirectory()) {
      if (pathFileName.contains(PackageId.PACKAGE_SEPARATOR)) {
        log.debug("Detected Directory Source for {}", pathAsFile);
        final var fhirPackage =
            new LocalDirectory(PackageId.parse(pathAsFile.getName()), inputPath);
        return PackageResolutionRequest.of(fhirPackage);
      } else {
        log.debug("Extracting all the archives from Directory {}", pathAsFile);
        return resolveDirectory(pathAsFile);
      }
    }

    if (inputPath.getFileName().toString().endsWith(LocalArchive.ARCHIVE_PACKAGE_EXTENSION)) {
      log.debug("Detected TGZ Package for {}", pathAsFile);
      final var fhirPackage = LocalArchive.parse(pathAsFile.getName(), inputPath);
      return PackageResolutionRequest.of(fhirPackage);
    }

    log.warn("Could not detect package(s) from the given input {}", inputPath);
    return PackageResolutionRequest.of();
  }

  private @NonNull PackageResolutionRequest resolveDirectory(@NonNull File directory) {
    final File[] tgzFiles =
        directory.listFiles(file ->
            (file.isFile() && file.getName().endsWith(LocalArchive.ARCHIVE_PACKAGE_EXTENSION))
                || (file.isDirectory() && file.getName().contains(PackageId.PACKAGE_SEPARATOR)));

    if (tgzFiles == null || tgzFiles.length == 0) {
      return PackageResolutionRequest.of();
    }

    final var packageList =
        Arrays.stream(tgzFiles)
            .sorted(java.util.Comparator.comparing(File::getName))
            .map(file ->
                file.isDirectory()
                    ? (PackageCategory)
                        new LocalDirectory(PackageId.parse(file.getName()), file.toPath())
                    : LocalArchive.parse(file.getName(), file.toPath()))
            .toList();
    log.debug("Found {} dependencies in directory {}", packageList.size(), directory);
    return new PackageResolutionRequest(packageList);
  }

  private static void cleanupTemporaryFiles(@NonNull List<PackageCategory> packagesList) {
    for (var pkgEntry : packagesList) {
      try {
        Files.deleteIfExists(((LocalArchive) pkgEntry).path());
      } catch (IOException _) {
        log.debug("Failed to delete temporary file: {}", pkgEntry);
      }
    }
  }

  private static boolean coordinatesMatch(@NonNull String requested, @NonNull String candidate) {
    String[] requestedParts = requested.trim().split(PackageId.PACKAGE_SEPARATOR, 2);
    String[] candidateParts = candidate.trim().split(PackageId.PACKAGE_SEPARATOR, 2);

    if (requestedParts.length != 2 || candidateParts.length != 2) {
      return requested.equalsIgnoreCase(candidate);
    }

    if (!requestedParts[0].equalsIgnoreCase(candidateParts[0])) {
      return false;
    }

    String[] requestedVersion = requestedParts[1].split("\\.");
    String[] candidateVersion = candidateParts[1].split("\\.");

    for (int i = 0; i < requestedVersion.length; i++) {
      String requestedPart = requestedVersion[i];

      /*
       * A wildcard matches this component and all remaining components.
       *
       * Examples:
       * 1.x   matches 1.1.2
       * 1.1.x matches 1.1.2
       * x     matches every version
       */
      if ("x".equalsIgnoreCase(requestedPart) || "*".equals(requestedPart)) {
        return true;
      }

      if (i >= candidateVersion.length || !requestedPart.equalsIgnoreCase(candidateVersion[i])) {
        return false;
      }
    }

    return requestedVersion.length == candidateVersion.length;
  }

  private static boolean isNewerPatchVersion(@NonNull String requested, @NonNull String candidate) {
    String[] requestedParts = requested.trim().split(PackageId.PACKAGE_SEPARATOR, 2);
    String[] candidateParts = candidate.trim().split(PackageId.PACKAGE_SEPARATOR, 2);
    if (requestedParts.length != 2
        || candidateParts.length != 2
        || !requestedParts[0].equalsIgnoreCase(candidateParts[0])) {
      return false;
    }

    String requestedVersion = requestedParts[1];
    String candidateVersion = candidateParts[1];
    if (!requestedVersion.matches("\\d+\\.\\d+\\.\\d+")
        || !candidateVersion.matches("\\d+\\.\\d+\\.\\d+")) {
      return false;
    }

    String[] requestedNumbers = requestedVersion.split("\\.");
    String[] candidateNumbers = candidateVersion.split("\\.");
    return requestedNumbers[0].equals(candidateNumbers[0])
        && requestedNumbers[1].equals(candidateNumbers[1])
        && SemanticVersion.compare(candidateVersion, requestedVersion) > 0;
  }
}
