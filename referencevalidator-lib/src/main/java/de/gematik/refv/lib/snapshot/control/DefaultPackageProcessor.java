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

import de.gematik.refv.lib.exceptions.SnapshotGenerationException;
import de.gematik.refv.lib.fhir_context.boundary.SnapshotGenerationContext;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolver;
import de.gematik.refv.lib.package_resolver.entity.LocalDirectory;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates snapshots for FHIR NPM packages in dependency-first order.
 *
 * <p>Resolved package identities are memoized per invocation. Successful archives are also cached
 * across invocations by package contents, generated dependency archives, and cache namespace.
 */
final class DefaultPackageProcessor implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(DefaultPackageProcessor.class);

  private static final String DEFAULT_CACHE_NAMESPACE = "snapshot-generation-v2";
  private static final String SNAPSHOT_TEMP_PREFIX = "snapshot-gen-";
  private static final String ARCHIVE_TEMP_PREFIX = "snapshot-archive-";
  private static final String DEPENDENCIES_DIRECTORY = "deps";
  private static final int HASH_BUFFER_SIZE = 64 * 1024;

  private final SnapshotGenerationContext fhirContext;
  private final PackageDependencyExtractor packageDependencyExtractor;
  private final PackageResolver packageResolver;
  private final SnapshotArchiveCache archiveCache;
  private final String cacheNamespace;

  DefaultPackageProcessor(
      @NonNull SnapshotGenerationContext fhirContext, @NonNull PackageResolver packageResolver) {
    this(
        fhirContext,
        packageResolver,
        fhirContext.cachePath().resolve("generated-snapshot-archives"),
        DEFAULT_CACHE_NAMESPACE);
  }

  DefaultPackageProcessor(
      @NonNull SnapshotGenerationContext fhirContext,
      @NonNull PackageResolver packageResolver,
      @NonNull Path archiveCacheDirectory,
      @NonNull String cacheNamespace) {
    this.fhirContext = Objects.requireNonNull(fhirContext, "The FHIR context must not be null");
    this.packageResolver =
        Objects.requireNonNull(packageResolver, "The package resolver must not be null");
    this.cacheNamespace =
        PackageUtils.requireNonBlank(cacheNamespace, "The cache namespace must not be blank");
    this.archiveCache =
        new SnapshotArchiveCache(
            Objects.requireNonNull(
                archiveCacheDirectory, "The archive cache directory must not be null"));
    this.packageDependencyExtractor = new PackageDependencyExtractor(fhirContext.cachePath());
  }

  synchronized SnapshotGenerationResult process(
      @NonNull Path packagePath,
      @NonNull Path outputDir,
      @Nullable Path patchesDir,
      @NonNull List<String> packagesToProcess) {

    Objects.requireNonNull(packagePath, "The package path must not be null");
    Objects.requireNonNull(outputDir, "The output directory must not be null");
    Objects.requireNonNull(packagesToProcess, "The packages-to-process list must not be null");

    var session = new ProcessingSession();
    var requestedPackages = normalizeRequestedPackages(packagesToProcess);

    try {
      validateInput(packagePath, outputDir, patchesDir);
      var resolution = packageResolver.resolvePath(packagePath);

      if (resolution.packages().isEmpty()) {
        return errorResult(
            packagePath.toString(),
            new SnapshotGenerationException(
                "Failed to resolve packages from the given path: " + packagePath));
      }

      boolean packageSelected = false;
      for (var resolvedPackage : resolution.packages()) {
        String coordinates = resolvedPackage.id().coordinates();
        if (!requestedPackages.isEmpty()
            && !requestedPackages.contains(PackageUtils.coordinateKey(coordinates))) {
          log.debug("Skipping package {}", coordinates);
          continue;
        }

        packageSelected = true;
        LocalDirectory localPackage =
            new LocalDirectory(resolvedPackage.id(), resolvedPackage.packagePath());
        processRootPackage(localPackage, outputDir, patchesDir, session, coordinates);
      }

      if (!packageSelected) {
        return errorResult(
            packagePath.toString(),
            new SnapshotGenerationException(
                "None of the resolved packages matched the requested package coordinates"));
      }
      if (session.results.isEmpty()) {
        return errorResult(
            packagePath.toString(),
            new SnapshotGenerationException(
                "Could not generate snapshots with the given parameters"));
      }
      return aggregate(session.results);
    } catch (Exception e) {
      log.error("Package snapshot processing failed for {}", packagePath, e);
      if (session.results.isEmpty()) {
        return errorResult(packagePath.toString(), e);
      }
      session.results.put(
          processingFailureKey(packagePath), errorResult(packagePath.toString(), e));
      return aggregate(session.results);
    }
  }

  private void processRootPackage(
      LocalDirectory localPackage,
      Path outputDir,
      @Nullable Path patchesDir,
      ProcessingSession session,
      String coordinates) {
    try {
      processRecursively(localPackage, outputDir, patchesDir, session, new ArrayDeque<>());
    } catch (Exception e) {
      session.results.putIfAbsent(coordinates, errorResult(coordinates, e));
      log.error("Failed to process root package {}", coordinates, e);
    }
  }

  private ProcessedPackage processRecursively(
      @NonNull LocalDirectory pkg,
      @NonNull Path outputDir,
      @Nullable Path patchesDir,
      @NonNull ProcessingSession session,
      @NonNull Deque<String> dependencyStack) {
    Objects.requireNonNull(pkg, "The package must not be null");
    Objects.requireNonNull(outputDir, "The output directory must not be null");
    Objects.requireNonNull(session, "The processing session must not be null");
    Objects.requireNonNull(dependencyStack, "The dependency stack must not be null");

    final var coordinates = pkg.id().coordinates();
    final var key = PackageUtils.coordinateKey(coordinates);

    ProcessedPackage completedPackage = session.completedPackages.get(key);
    if (Objects.nonNull(completedPackage)) {
      log.debug("Reusing package {} already processed in this session", coordinates);
      return completedPackage;
    }
    // check if a dependency has completed with failure, so we break the generation for the current
    // package
    detectPreviousSnapshotFailure(session, dependencyStack, key, coordinates);
    // add the current coordinates to the end of the stack
    dependencyStack.addLast(coordinates);
    try {
      return processPackage(pkg, outputDir, patchesDir, session, dependencyStack);
    } catch (SnapshotGenerationException e) {
      recordFailure(session, key, coordinates, e);
      throw e;
    } catch (Exception e) {
      SnapshotGenerationException wrapped =
          new SnapshotGenerationException(
              "Failed to process package " + coordinates + ": " + e.getLocalizedMessage(), e);
      recordFailure(session, key, coordinates, wrapped);
      throw wrapped;
    } finally {
      removeLastOccurrence(dependencyStack, coordinates);
    }
  }

  private ProcessedPackage processPackage(
      LocalDirectory pkg,
      Path outputDir,
      @Nullable Path patchesDir,
      ProcessingSession session,
      Deque<String> dependencyStack)
      throws IOException {
    String coordinates = pkg.id().coordinates();
    String key = PackageUtils.coordinateKey(coordinates);
    log.info("Processing package {}", coordinates);

    Path workDir = Files.createTempDirectory(SNAPSHOT_TEMP_PREFIX);
    try {
      LocalDirectory workingPackage = copyPackageToWorkingDirectory(pkg, workDir);
      applyPatches(workingPackage, patchesDir);

      Map<String, ArchivedPackage> dependencyClosure =
          processDependencies(workingPackage, outputDir, patchesDir, session, dependencyStack);
      String cacheKey = createArchiveCacheKey(workingPackage, dependencyClosure.values());
      Optional<Path> materializedArchive =
          archiveCache.materialize(cacheKey, outputDir, PackageUtils.expectedArchiveFileName(pkg));
      if (materializedArchive.isPresent()) {
        Path cachedPath = materializedArchive.orElseThrow();
        SnapshotGenerationResult cachedResult = cacheHitResult(coordinates, cachedPath);
        ProcessedPackage completed =
            completedPackage(pkg, cachedPath, cachedResult, dependencyClosure);
        session.complete(key, completed);
        log.debug("Reused cached snapshot archive for {}", coordinates);
        return completed;
      }

      List<ResolvedPackage> snapshotDependencies =
          extractDependencies(dependencyClosure.values(), workDir);
      log.info(
          "Generating snapshots for {} using {} dependency packages",
          coordinates,
          snapshotDependencies.size());
      SnapshotGenerationResult generationResult =
          generateSnapshots(workingPackage, snapshotDependencies, coordinates);
      if (hasErrors(generationResult)) {
        SnapshotGenerationException failure =
            new SnapshotGenerationException(
                "Snapshot generation returned errors for " + coordinates);
        session.failures.put(key, failure);
        session.results.put(coordinates, generationResult);
        throw failure;
      }

      Path generatedArchive = createCompressedArchive(workingPackage, outputDir);
      archiveCache.publish(cacheKey, generatedArchive);
      SnapshotGenerationResult successfulResult =
          withMessage(generationResult, "Snapshot stored at " + generatedArchive);
      ProcessedPackage completed =
          completedPackage(pkg, generatedArchive, successfulResult, dependencyClosure);
      session.complete(key, completed);
      log.info(
          "Successfully processed package {} and stored archive at {}",
          coordinates,
          generatedArchive);
      return completed;
    } finally {
      cleanupTempFolder(workDir);
    }
  }

  private SnapshotGenerationResult generateSnapshots(
      LocalDirectory workingPackage,
      List<ResolvedPackage> snapshotDependencies,
      String coordinates) {
    try (var generationContext = fhirContext.cloneContext(snapshotDependencies)) {
      return generationContext.generateSnapshots(workingPackage.path());
    } catch (Exception e) {
      throw new SnapshotGenerationException("Failed to generate snapshots for " + coordinates, e);
    }
  }

  private Map<String, ArchivedPackage> processDependencies(
      LocalDirectory workingPackage,
      Path outputDir,
      @Nullable Path patchesDir,
      ProcessingSession session,
      Deque<String> dependencyStack) {
    String ownerCoordinates = workingPackage.id().coordinates();
    Map<String, String> dependencies =
        packageDependencyExtractor.uniqueDependencies(
            packageDependencyExtractor.fromPackage(workingPackage), ownerCoordinates);
    log.debug("Package {} declares {} unique dependencies", ownerCoordinates, dependencies.size());

    Map<String, ArchivedPackage> dependencyClosure = new LinkedHashMap<>();
    for (String coordinates : dependencies.values()) {
      ProcessedPackage dependency =
          processDependency(coordinates, outputDir, patchesDir, session, dependencyStack);
      mergeDependencyClosure(dependencyClosure, dependency.dependencyClosure());
      dependencyClosure.putIfAbsent(
          PackageUtils.coordinateKey(dependency.coordinates()), dependency.archivedPackage());
    }
    return dependencyClosure;
  }

  private ProcessedPackage completedPackage(
      LocalDirectory pkg,
      Path archive,
      SnapshotGenerationResult result,
      Map<String, ArchivedPackage> dependencyClosure) {
    return new ProcessedPackage(
        new ArchivedPackage(new LocalDirectory(pkg.id(), pkg.path()), archive),
        result,
        dependencyClosure);
  }

  private void recordFailure(
      ProcessingSession session,
      String key,
      String coordinates,
      SnapshotGenerationException failure) {
    session.failures.putIfAbsent(key, failure);
    session.results.putIfAbsent(coordinates, errorResult(coordinates, failure));
  }

  private void detectPreviousSnapshotFailure(
      @NonNull ProcessingSession session,
      @NonNull Deque<String> dependencyStack,
      String key,
      String coordinates) {
    SnapshotGenerationException previousFailure = session.failures.get(key);
    if (Objects.nonNull(previousFailure)) {
      throw new SnapshotGenerationException(
          "Package " + coordinates + " failed during an earlier processing attempt",
          previousFailure);
    }
    if (dependencyStack.stream().anyMatch(item -> PackageUtils.coordinateKey(item).equals(key))) {
      throw cyclicDependencyException(dependencyStack, coordinates);
    }
  }

  private ProcessedPackage processDependency(
      @NonNull String requestedCoordinates,
      @NonNull Path outputDir,
      @Nullable Path patchesDir,
      @NonNull ProcessingSession session,
      @NonNull Deque<String> dependencyStack) {

    final var requestKey = PackageUtils.coordinateKey(requestedCoordinates);
    LocalDirectory dependencyPackage = session.resolvedDependencies.get(requestKey);
    if (dependencyPackage == null) {
      dependencyPackage = resolveDependency(requestedCoordinates);
      session.resolvedDependencies.put(requestKey, dependencyPackage);
    }

    String resolvedCoordinates = dependencyPackage.id().coordinates();
    String resolvedKey = PackageUtils.coordinateKey(resolvedCoordinates);
    session.resolvedDependencies.putIfAbsent(resolvedKey, dependencyPackage);

    if (!PackageUtils.sameCoordinates(requestedCoordinates, resolvedCoordinates)) {
      log.info("Dependency {} resolved to {}", requestedCoordinates, resolvedCoordinates);
    }

    ProcessedPackage completed = session.completedPackages.get(resolvedKey);
    if (completed != null) {
      return completed;
    }

    ProcessedPackage result =
        processRecursively(dependencyPackage, outputDir, patchesDir, session, dependencyStack);
    if (hasErrors(result.result())) {
      throw new SnapshotGenerationException(
          "Failed to process dependency "
              + resolvedCoordinates
              + " requested as "
              + requestedCoordinates);
    }
    return result;
  }

  private LocalDirectory resolveDependency(@NonNull String requestedCoordinates) {
    return (LocalDirectory) packageResolver.getMatchingPackage(requestedCoordinates);
  }

  private String createArchiveCacheKey(
      @NonNull LocalDirectory workingPackage, @NonNull Collection<ArchivedPackage> dependencies) {
    try {
      MessageDigest digest = newDigest();
      updateDigest(digest, cacheNamespace);
      updateDigest(digest, PackageUtils.coordinateKey(workingPackage.id().coordinates()));
      updateDigest(digest, "package-files");
      hashDirectory(digest, workingPackage.path());

      List<ArchivedPackage> orderedDependencies =
          dependencies.stream()
              .sorted(
                  Comparator.comparing(
                      d -> PackageUtils.coordinateKey(d.descriptor().id().coordinates())))
              .toList();
      updateDigest(digest, "dependency-archives");
      updateDigest(digest, orderedDependencies.size());
      for (ArchivedPackage dependency : orderedDependencies) {
        updateDigest(
            digest, PackageUtils.coordinateKey(dependency.descriptor().id().coordinates()));
        hashFile(digest, dependency.archive());
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (IOException e) {
      throw new SnapshotGenerationException(
          "Failed to calculate snapshot archive cache key for " + workingPackage.id().coordinates(),
          e);
    }
  }

  private void hashDirectory(@NonNull MessageDigest digest, @NonNull Path directory)
      throws IOException {
    try (Stream<Path> paths = Files.walk(directory)) {
      List<Path> files =
          paths
              .filter(path -> !path.equals(directory))
              .sorted(Comparator.comparing(path -> directory.relativize(path).toString()))
              .toList();
      updateDigest(digest, files.size());
      for (Path file : files) {
        updateDigest(digest, directory.relativize(file).toString().replace('\\', '/'));
        if (Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) {
          updateDigest(digest, "directory");
        } else if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
          updateDigest(digest, "file");
          hashFile(digest, file);
        } else {
          throw new IOException("Unsupported filesystem entry in package directory: " + file);
        }
      }
    }
  }

  private void hashFile(@NonNull MessageDigest digest, @NonNull Path file) {
    try (InputStream input = Files.newInputStream(file)) {
      updateDigest(digest, Files.size(file));
      byte[] buffer = new byte[HASH_BUFFER_SIZE];
      int read;
      while ((read = input.read(buffer)) >= 0) {
        if (read > 0) {
          digest.update(buffer, 0, read);
        }
      }
    } catch (IOException e) {
      throw new SnapshotGenerationException("Failed to hash file " + file, e);
    }
  }

  private static MessageDigest newDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private static void updateDigest(MessageDigest digest, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    digest.update((byte) (bytes.length >>> 24));
    digest.update((byte) (bytes.length >>> 16));
    digest.update((byte) (bytes.length >>> 8));
    digest.update((byte) bytes.length);
    digest.update(bytes);
  }

  private static void updateDigest(MessageDigest digest, long value) {
    for (int shift = Long.SIZE - Byte.SIZE; shift >= 0; shift -= Byte.SIZE) {
      digest.update((byte) (value >>> shift));
    }
  }

  private LocalDirectory copyPackageToWorkingDirectory(
      @NonNull LocalDirectory sourcePackage, @NonNull Path workDir) throws IOException {
    Path copy =
        PackageUtils.resolveContainedPath(
            workDir, PackageUtils.safeDirectoryName(sourcePackage.id().coordinates()));
    Files.createDirectories(copy);
    FileUtils.copyDirectory(sourcePackage.path().toFile(), copy.toFile(), true);
    return new LocalDirectory(sourcePackage.id(), copy);
  }

  private void applyPatches(@NonNull LocalDirectory workingPackage, @Nullable Path patchesDir) {
    if (Objects.nonNull(patchesDir)) {
      PatchApplier.applyForPackage(workingPackage, workingPackage.path(), patchesDir);
    }
  }

  private List<ResolvedPackage> extractDependencies(
      @NonNull Collection<ArchivedPackage> dependencies, @NonNull Path workDir) {
    if (dependencies.isEmpty()) {
      return List.of();
    }

    Path root = PackageUtils.resolveContainedPath(workDir, DEPENDENCIES_DIRECTORY);
    try {
      Files.createDirectories(root);
    } catch (IOException e) {
      throw new SnapshotGenerationException("Failed to create dependency directory " + root, e);
    }

    List<ResolvedPackage> result = new ArrayList<>(dependencies.size());
    for (ArchivedPackage dependency : dependencies) {
      String coordinates = dependency.descriptor().id().coordinates();
      Path dependencyRoot =
          PackageUtils.resolveContainedPath(root, PackageUtils.safeDirectoryName(coordinates));
      try {
        Files.createDirectories(dependencyRoot);
        PackageArchiveUtils.decompress(dependency.archive().toString(), dependencyRoot.toFile());
        result.add(new ResolvedPackage(dependency.descriptor().id(), dependencyRoot, List.of()));
      } catch (Exception e) {
        throw new SnapshotGenerationException(
            "Failed to materialize dependency " + coordinates + " from " + dependency.archive(), e);
      }
    }
    return List.copyOf(result);
  }

  private void mergeDependencyClosure(
      @NonNull Map<String, ArchivedPackage> target, @NonNull Map<String, ArchivedPackage> source) {
    source.forEach(target::putIfAbsent);
  }

  @SuppressWarnings("java:S5443")
  private Path createCompressedArchive(
      @NonNull LocalDirectory fhirPackage, @NonNull Path outputDirectory) {
    Path stagingDirectory = null;
    try {
      Files.createDirectories(outputDirectory);
      stagingDirectory = Files.createTempDirectory(ARCHIVE_TEMP_PREFIX);
      final var stagedPackage =
          PackageUtils.resolveContainedPath(
              stagingDirectory,
              PackageUtils.safeArchiveRootName(
                  fhirPackage.id().name(), fhirPackage.id().version()));
      FileUtils.copyDirectory(fhirPackage.path().toFile(), stagedPackage.toFile(), true);
      final var archiveName = PackageArchiveUtils.compress(stagedPackage, outputDirectory);
      if (archiveName.isBlank()) {
        throw new SnapshotGenerationException(
            "Archive utility returned an empty archive path for " + fhirPackage.id().coordinates());
      }

      Path archivePath = Path.of(archiveName).toAbsolutePath().normalize();
      if (!Files.exists(archivePath)) {
        Path relative = outputDirectory.resolve(archiveName).toAbsolutePath().normalize();
        if (Files.exists(relative)) {
          archivePath = relative;
        }
      }
      if (!Files.isRegularFile(archivePath)) {
        throw new SnapshotGenerationException("Generated archive does not exist: " + archivePath);
      }
      return archivePath;
    } catch (SnapshotGenerationException e) {
      throw e;
    } catch (Exception e) {
      throw new SnapshotGenerationException(
          "Failed to create compressed archive: " + e.getLocalizedMessage(), e);
    } finally {
      cleanupTempFolder(stagingDirectory);
    }
  }

  private void validateInput(Path packagePath, Path outputDir, @Nullable Path patchesDir)
      throws IOException {
    if (packagePath.toString().isBlank()) {
      throw new SnapshotGenerationException("The package path must not be blank");
    }
    if (patchesDir != null && !Files.isDirectory(patchesDir)) {
      throw new SnapshotGenerationException(
          "The patches directory does not exist or is not a directory: " + patchesDir);
    }
    Files.createDirectories(outputDir);
    if (!Files.isDirectory(outputDir) || !Files.isWritable(outputDir)) {
      throw new SnapshotGenerationException(
          "The output path is not a writable directory: " + outputDir);
    }
  }

  private Set<String> normalizeRequestedPackages(List<String> packagesToProcess) {
    Set<String> normalized = new LinkedHashSet<>();
    for (String coordinates : packagesToProcess) {
      normalized.add(
          PackageUtils.coordinateKey(
              PackageUtils.requireNonBlank(
                  coordinates,
                  "The packages-to-process list must not contain null or blank coordinates")));
    }
    return Set.copyOf(normalized);
  }

  private SnapshotGenerationException cyclicDependencyException(
      Deque<String> dependencyStack, String repeatedCoordinates) {
    List<String> path = new ArrayList<>(dependencyStack);
    int start = -1;
    for (int i = 0; i < path.size(); i++) {
      if (PackageUtils.sameCoordinates(path.get(i), repeatedCoordinates)) {
        start = i;
        break;
      }
    }
    path.add(repeatedCoordinates);
    List<String> cycle = start >= 0 ? path.subList(start, path.size()) : path;
    return new SnapshotGenerationException(
        "Cyclic package dependency detected: " + String.join(" -> ", cycle));
  }

  private void removeLastOccurrence(Deque<String> stack, String coordinates) {
    if (!stack.isEmpty() && PackageUtils.sameCoordinates(coordinates, stack.peekLast())) {
      stack.removeLast();
      return;
    }
    for (var iterator = stack.descendingIterator(); iterator.hasNext(); ) {
      if (PackageUtils.sameCoordinates(coordinates, iterator.next())) {
        iterator.remove();
        return;
      }
    }
  }

  private SnapshotGenerationResult aggregate(Map<String, SnapshotGenerationResult> results) {
    return new SnapshotGenerationResult(
        results.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .filter(Objects::nonNull)
            .map(SnapshotGenerationResult::messages)
            .flatMap(Collection::stream)
            .toList());
  }

  private SnapshotGenerationResult withMessage(SnapshotGenerationResult result, String message) {
    List<ResultMessage> messages = new ArrayList<>(result.messages());
    messages.add(
        ResultMessage.fromMessage(
            IssueSeverity.INFORMATION, MessageId.NO_ERROR.getCode(), message));
    return new SnapshotGenerationResult(List.copyOf(messages));
  }

  private SnapshotGenerationResult cacheHitResult(String coordinates, Path archive) {
    return new SnapshotGenerationResult(
        List.of(
            ResultMessage.fromMessage(
                IssueSeverity.INFORMATION,
                MessageId.NO_ERROR.getCode(),
                "Reused cached snapshot archive for " + coordinates + " at " + archive)));
  }

  private SnapshotGenerationResult errorResult(String coordinates, Throwable error) {
    return new SnapshotGenerationResult(
        List.of(
            ResultMessage.fromMessage(
                IssueSeverity.ERROR,
                MessageId.SNAPSHOT_GENERATION_ERROR.getCode(),
                "Failed to generate snapshots for " + coordinates + ": " + error.getMessage())));
  }

  private boolean hasErrors(SnapshotGenerationResult result) {
    return result.messages().stream()
        .anyMatch(
            message ->
                message.severity() == IssueSeverity.ERROR
                    || message.severity() == IssueSeverity.FATAL);
  }

  private void cleanupTempFolder(@Nullable Path directory) {
    if (directory == null) {
      return;
    }
    try {
      FileUtils.deleteDirectory(directory.toFile());
    } catch (IOException e) {
      log.warn("Failed to delete temporary directory {}", directory, e);
    }
  }

  private String processingFailureKey(Path packagePath) {
    return "__processing_failure__:"
        + UUID.nameUUIDFromBytes(
            packagePath.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
  }

  private static final class ProcessingSession {
    private final Map<String, ProcessedPackage> completedPackages = new LinkedHashMap<>();
    private final Map<String, SnapshotGenerationException> failures = new LinkedHashMap<>();
    private final Map<String, SnapshotGenerationResult> results = new LinkedHashMap<>();
    private final Map<String, LocalDirectory> resolvedDependencies = new LinkedHashMap<>();

    private void complete(String key, ProcessedPackage pkg) {
      completedPackages.put(key, pkg);
      results.put(pkg.coordinates(), pkg.result());
    }
  }

  @Override
  public synchronized void close() {
    try {
      fhirContext.close();
    } catch (Exception e) {
      log.warn("Failed to close snapshot generation context", e);
    }
  }

  private record ArchivedPackage(@NonNull LocalDirectory descriptor, @NonNull Path archive) {
    private ArchivedPackage {
      Objects.requireNonNull(descriptor, "The package descriptor must not be null");
      archive =
          Objects.requireNonNull(archive, "The archive path must not be null")
              .toAbsolutePath()
              .normalize();
    }
  }

  private record ProcessedPackage(
      @NonNull ArchivedPackage archivedPackage,
      @NonNull SnapshotGenerationResult result,
      @NonNull Map<String, ArchivedPackage> dependencyClosure) {
    private ProcessedPackage {
      Objects.requireNonNull(archivedPackage, "The archived package must not be null");
      Objects.requireNonNull(result, "The result must not be null");
      dependencyClosure =
          Collections.unmodifiableMap(
              new LinkedHashMap<>(
                  Objects.requireNonNull(
                      dependencyClosure, "The dependency closure must not be null")));
    }

    private String coordinates() {
      return archivedPackage.descriptor().id().coordinates();
    }
  }
}
