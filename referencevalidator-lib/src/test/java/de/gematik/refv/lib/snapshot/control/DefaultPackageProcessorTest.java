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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.SnapshotGenerationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.TerminologyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolver;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolverFactory;
import de.gematik.refv.lib.package_resolver.entity.LocalDirectory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.PackageResolution;
import de.gematik.refv.lib.package_resolver.entity.PackageResolutionRequest;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the process() flow of {@link DefaultPackageProcessor} using a local package and FHIR
 * context.
 */
class DefaultPackageProcessorTest {

  @TempDir Path tempDir;
  private final List<DefaultPackageProcessor> processors = new ArrayList<>();

  @AfterEach
  void closeProcessors() {
    processors.forEach(DefaultPackageProcessor::close);
  }

  @DisplayName("Given a null FHIR context, when created, then a NullPointerException is thrown")
  @Test
  void expectNullContextThrows() {
    final var contextConfiguration = contextConfiguration();
    final var packageResolver =
        PackageResolverFactory.withConfiguration(contextConfiguration.packageLoading());

    Assertions.assertThrows(
        NullPointerException.class, () -> new DefaultPackageProcessor(null, packageResolver));
  }

  @DisplayName("Given a null package, when processing, then a NullPointerException is thrown")
  @Test
  void expectNullPackageThrows() {
    final var contextConfiguration = contextConfiguration();
    final var context =
        ContextProvider.defaultProvider().snapshotGenerationContext(contextConfiguration);
    final var packageResolver =
        PackageResolverFactory.withConfiguration(contextConfiguration.packageLoading());

    final var processor = track(new DefaultPackageProcessor(context, packageResolver));
    final var outputPath = tempDir.resolve("out");
    Assertions.assertThrows(
        NullPointerException.class, () -> processor.process(null, outputPath, null, null));
  }

  @DisplayName(
      "Given a null output directory, when processing, then a NullPointerException is thrown")
  @Test
  void expectNullOutputDirectoryThrows() {
    final var contextConfiguration = contextConfiguration();
    final var context =
        ContextProvider.defaultProvider().snapshotGenerationContext(contextConfiguration);
    final var packageResolver =
        PackageResolverFactory.withConfiguration(contextConfiguration.packageLoading());

    final var processor = track(new DefaultPackageProcessor(context, packageResolver));
    final var packagePath = tempDir.resolve("my.pkg#1.0.0");
    Assertions.assertDoesNotThrow(() -> Files.createDirectory(packagePath).toFile().deleteOnExit());
    final var pkg = new LocalDirectory(new PackageId("my.pkg", "1.0.0"), packagePath);
    final List<String> packagesToProcess = List.of();
    final var sourcePath = pkg.path();
    Assertions.assertThrows(
        NullPointerException.class,
        () -> processor.process(sourcePath, null, null, packagesToProcess));
  }

  @DisplayName(
      "Given a package without dependencies, when processing, then a result with messages is returned")
  @Test
  void expectProcessPackageWithoutDependencies() throws Exception {
    final var pkg = writePackage();
    final var outputDir = Files.createDirectory(tempDir.resolve("out"));

    final var contextConfiguration = contextConfiguration();
    final var context =
        ContextProvider.defaultProvider().snapshotGenerationContext(contextConfiguration);
    final var packageResolver =
        PackageResolverFactory.withConfiguration(contextConfiguration.packageLoading());

    final var processor = track(new DefaultPackageProcessor(context, packageResolver));
    final var result = processor.process(pkg.path(), outputDir, null, List.of());

    Assertions.assertNotNull(result);
    Assertions.assertFalse(result.messages().isEmpty());
  }

  @DisplayName(
      "Given a package whose package.json is missing, when processing, then a ParsingException propagates")
  @Test
  void expectProcessHandlesIoError() throws Exception {
    // A package whose package.json is missing -> dependency extraction fails with ParsingException
    final var pkgDir = Files.createDirectory(tempDir.resolve("bad.pkg#1.0.0"));
    final var pkg = new LocalDirectory(new PackageId("bad.pkg", "1.0.0"), pkgDir);
    final var outputDir = Files.createDirectory(tempDir.resolve("out"));

    final var contextConfiguration = contextConfiguration();
    final var context =
        ContextProvider.defaultProvider().snapshotGenerationContext(contextConfiguration);
    final var packageResolver =
        PackageResolverFactory.withConfiguration(contextConfiguration.packageLoading());

    final var processor = track(new DefaultPackageProcessor(context, packageResolver));
    final var result =
        Assertions.assertDoesNotThrow(
            () -> processor.process(pkg.path(), outputDir, null, List.of()));

    Assertions.assertEquals(1, result.messages().size());
    final var firstMessage = result.messages().stream().toList().getFirst();
    Assertions.assertEquals(IssueSeverity.ERROR, firstMessage.severity());
    Assertions.assertEquals(
        MessageId.SNAPSHOT_GENERATION_ERROR.getCode(), firstMessage.messageId());
  }

  @DisplayName(
      "Given an already-processed package, when processing again, then the already-processed message is returned")
  @Test
  void expectAlreadyProcessedPackageSkipped() throws Exception {
    final var pkg = writePackage();
    final var outputDir = Files.createDirectory(tempDir.resolve("out"));

    final var contextConfiguration = contextConfiguration();
    final var context =
        ContextProvider.defaultProvider().snapshotGenerationContext(contextConfiguration);
    final var packageResolver =
        PackageResolverFactory.withConfiguration(contextConfiguration.packageLoading());

    final var processor = track(new DefaultPackageProcessor(context, packageResolver));
    // First pass processes and caches
    processor.process(pkg.path(), outputDir, null, List.of());
    // A second invocation with the same package contents hits the persistent archive cache.
    final var pkg2 = writePackage();
    final var result = processor.process(pkg2.path(), outputDir, null, List.of());

    Assertions.assertNotNull(result);
    Assertions.assertTrue(
        result.messages().stream()
            .anyMatch(
                message -> message.messageContent().contains("Reused cached snapshot archive")));
  }

  @DisplayName(
      "Given changed package contents, when processing again, then the cache is invalidated")
  @Test
  void expectChangedPackageContentsInvalidateCache() throws Exception {
    final var pkg = writePackage();
    final var outputDir = Files.createDirectory(tempDir.resolve("out"));
    final var archiveCache = tempDir.resolve("snapshot-archives");
    final var firstConfiguration = contextConfiguration(tempDir.resolve("first-package-cache"));
    final var firstContext =
        ContextProvider.defaultProvider().snapshotGenerationContext(firstConfiguration);
    final var firstResolver =
        PackageResolverFactory.withConfiguration(firstConfiguration.packageLoading());
    final var firstProcessor =
        track(
            new DefaultPackageProcessor(
                firstContext, firstResolver, archiveCache, "test-namespace"));

    firstProcessor.process(pkg.path(), outputDir, null, List.of());
    Files.writeString(pkg.path().resolve("package/resource.txt"), "changed");

    final var secondConfiguration = contextConfiguration(tempDir.resolve("second-package-cache"));
    final var secondContext =
        ContextProvider.defaultProvider().snapshotGenerationContext(secondConfiguration);
    final var secondResolver =
        PackageResolverFactory.withConfiguration(secondConfiguration.packageLoading());
    final var secondProcessor =
        track(
            new DefaultPackageProcessor(
                secondContext, secondResolver, archiveCache, "test-namespace"));
    final var result = secondProcessor.process(pkg.path(), outputDir, null, List.of());

    Assertions.assertTrue(
        result.messages().stream()
            .anyMatch(message -> message.messageContent().contains("Snapshot stored at")));
    Assertions.assertFalse(
        result.messages().stream()
            .anyMatch(
                message -> message.messageContent().contains("Reused cached snapshot archive")));
  }

  @DisplayName("Given an unselected root package, when processing, then it reports no match")
  @Test
  void reportsNoMatchingRequestedRoot() throws Exception {
    LocalDirectory root = writePackage("root#1.0.0", Map.of());
    ProcessorResult harness = processPackages(List.of(resolved(root)));

    var result =
        harness
            .processor()
            .process(root.path(), harness.outputDirectory(), null, List.of("other#1.0.0"));

    Assertions.assertTrue(hasMessage(result, "None of the resolved packages matched"));
    verify(harness.context(), never()).cloneContext(anyCollection());
  }

  @DisplayName("Given a cyclic dependency graph, when processing, then it reports the cycle")
  @Test
  void reportsDependencyCycle() throws Exception {
    Path packageCache = tempDir.resolve("package-cache");
    Files.createDirectories(packageCache);
    LocalDirectory root = writePackage("root#1.0.0", Map.of("dependency", "1.0.0"));
    writePackage(packageCache, "dependency#1.0.0", Map.of("root", "1.0.0"));
    writePackage(packageCache, "root#1.0.0", Map.of());
    ProcessorResult processorResult = processPackages(List.of(resolved(root)), packageCache);

    var result =
        processorResult
            .processor()
            .process(root.path(), processorResult.outputDirectory(), null, List.of());

    Assertions.assertTrue(
        hasMessage(
            result,
            "Cyclic package dependency detected: root#1.0.0 -> dependency#1.0.0 -> root#1.0.0"));
    verify(processorResult.context(), never()).generateSnapshots(any(Path.class));
  }

  @DisplayName(
      "Given roots sharing a dependency, when processed together, then the dependency is generated once")
  @Test
  void processesSharedDependencyOncePerInvocation() throws Exception {
    Path packageCache = tempDir.resolve("package-cache");
    Files.createDirectories(packageCache);
    LocalDirectory firstRoot = writePackage("first#1.0.0", Map.of("shared", "1.0.0"));
    LocalDirectory secondRoot = writePackage("second#1.0.0", Map.of("shared", "1.0.0"));
    writePackage(packageCache, "shared#1.0.0", Map.of());
    ProcessorResult processorResult =
        processPackages(List.of(resolved(firstRoot), resolved(secondRoot)), packageCache);

    var result =
        processorResult
            .processor()
            .process(firstRoot.path(), processorResult.outputDirectory(), null, List.of());

    Assertions.assertFalse(
        result.messages().stream().anyMatch(message -> message.severity() == IssueSeverity.ERROR));
    verify(processorResult.context(), times(3)).generateSnapshots(any(Path.class));
  }

  @DisplayName("Given snapshot generation errors, when processing, then no archive is published")
  @Test
  void doesNotPublishArchiveWhenSnapshotGenerationFails() throws Exception {
    LocalDirectory root = writePackage("root#1.0.0", Map.of());
    ProcessorResult processorResult = processPackages(List.of(resolved(root)));
    try (var context = processorResult.context()) {
      when(context.generateSnapshots(any(Path.class)))
          .thenReturn(
              SnapshotGenerationResult.forMessage(
                  IssueSeverity.ERROR,
                  MessageId.SNAPSHOT_GENERATION_ERROR.getCode(),
                  "synthetic generation failure"));

      var result =
          processorResult
              .processor()
              .process(root.path(), processorResult.outputDirectory(), null, List.of());

      Assertions.assertTrue(
          result.messages().stream()
              .anyMatch(message -> message.severity() == IssueSeverity.ERROR));
      try (var outputFiles = Files.list(processorResult.outputDirectory())) {
        Assertions.assertTrue(outputFiles.findAny().isEmpty());
      }
    }
  }

  @DisplayName("Given blank requested coordinates, when processing, then validation rejects them")
  @Test
  void rejectsBlankRequestedCoordinates() throws Exception {
    LocalDirectory root = writePackage("root#1.0.0", Map.of());
    ProcessorResult processorResult = processPackages(List.of(resolved(root)));
    var processor = processorResult.processor();
    var sourcePath = root.path();
    var outputDirectory = processorResult.outputDirectory();
    var requestedCoordinates = List.of(" ");

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> processor.process(sourcePath, outputDirectory, null, requestedCoordinates));
  }

  @DisplayName(
      "Given a missing exact dependency version, when a compatible cached version exists, then it is used")
  @Test
  void usesCompatibleCachedDependencyVersion() throws Exception {
    Path packageCache = tempDir.resolve("package-cache");
    Files.createDirectories(packageCache);
    LocalDirectory cachedDependency =
        writePackage(tempDir.resolve("source-dependencies"), "hl7.fhir.r4.core#4.0.1", Map.of());
    LocalDirectory root =
        writePackage("de.gematik.isik#5.1.3", Map.of("hl7.fhir.r4.core", "4.0.0"));

    var configuration = contextConfiguration(packageCache);
    PackageResolver resolver =
        PackageResolverFactory.withConfiguration(configuration.packageLoading());
    resolver.resolveRequest(
        new PackageResolutionRequest(List.of(cachedDependency)),
        PackageResolver.TransitiveResolution.IGNORE);

    SnapshotGenerationContext context = mock(SnapshotGenerationContext.class);
    when(context.cachePath()).thenReturn(packageCache);
    when(context.cloneContext(anyCollection())).thenReturn(context);
    when(context.generateSnapshots(any(Path.class)))
        .thenReturn(
            SnapshotGenerationResult.forMessage(
                IssueSeverity.INFORMATION, MessageId.NO_ERROR.getCode(), "generation succeeded"));

    Path outputDirectory = Files.createDirectories(tempDir.resolve("output"));
    var processor =
        track(
            new DefaultPackageProcessor(
                context, resolver, tempDir.resolve("archive-cache"), "dependency-fallback-test"));
    var result = processor.process(root.path(), outputDirectory, null, List.of());

    Assertions.assertFalse(
        result.messages().stream().anyMatch(message -> message.severity() == IssueSeverity.ERROR));
    Assertions.assertTrue(
        Files.isRegularFile(outputDirectory.resolve("hl7.fhir.r4.core-4.0.1.tgz")));
  }

  private ProcessorResult processPackages(List<ResolvedPackage> roots) throws IOException {
    return processPackages(roots, tempDir.resolve("package-cache"));
  }

  private ProcessorResult processPackages(List<ResolvedPackage> roots, Path packageCache)
      throws IOException {
    Files.createDirectories(packageCache);
    Path outputDirectory = Files.createDirectories(tempDir.resolve("output"));
    Path archiveCache = tempDir.resolve("archive-cache");

    SnapshotGenerationContext context = mock(SnapshotGenerationContext.class);
    when(context.cachePath()).thenReturn(packageCache);
    when(context.cloneContext(anyCollection())).thenReturn(context);
    when(context.generateSnapshots(any(Path.class)))
        .thenReturn(
            SnapshotGenerationResult.forMessage(
                IssueSeverity.INFORMATION, MessageId.NO_ERROR.getCode(), "generation succeeded"));

    PackageResolver resolver = mock(PackageResolver.class);
    when(resolver.resolvePath(any(Path.class))).thenReturn(new PackageResolution(roots));
    when(resolver.getMatchingPackage(anyString()))
        .thenAnswer(
            invocation -> {
              var packageId = PackageId.parse(invocation.getArgument(0));
              Path cachedPackage = packageCache.resolve(packageId.coordinates());
              return new LocalDirectory(packageId, cachedPackage);
            });

    return new ProcessorResult(
        track(new DefaultPackageProcessor(context, resolver, archiveCache, "edge-case-tests")),
        context,
        outputDirectory);
  }

  private DefaultPackageProcessor track(DefaultPackageProcessor processor) {
    processors.add(processor);
    return processor;
  }

  private ResolvedPackage resolved(LocalDirectory pkg) {
    return new ResolvedPackage(pkg.id(), pkg.path(), List.of());
  }

  private boolean hasMessage(SnapshotGenerationResult result, String expectedText) {
    return result.messages().stream()
        .anyMatch(message -> message.messageContent().contains(expectedText));
  }

  private record ProcessorResult(
      DefaultPackageProcessor processor, SnapshotGenerationContext context, Path outputDirectory) {}

  private LocalDirectory writePackage(String coordinates, Map<String, String> dependencies)
      throws IOException {
    return writePackage(tempDir, coordinates, dependencies);
  }

  private LocalDirectory writePackage(
      Path parent, String coordinates, Map<String, String> dependencies) throws IOException {
    PackageId id = PackageId.parse(coordinates);
    Path root = Files.createDirectories(parent.resolve(coordinates));
    Path packageDirectory = Files.createDirectories(root.resolve("package"));
    Files.writeString(packageDirectory.resolve("package.json"), packageJson(id, dependencies));
    return new LocalDirectory(id, root);
  }

  private LocalDirectory writePackage() throws Exception {
    return writePackage("my.pkg#1.0.0", Map.of());
  }

  private String packageJson(PackageId id, Map<String, String> dependencies) {
    String dependencyJson =
        dependencies.entrySet().stream()
            .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"")
            .collect(java.util.stream.Collectors.joining(","));
    return "{\"name\":\""
        + id.name()
        + "\",\"version\":\""
        + id.version()
        + "\",\"dependencies\":{"
        + dependencyJson
        + "}}";
  }

  private ContextConfiguration contextConfiguration() {
    return contextConfiguration(tempDir.resolve("cache"));
  }

  private ContextConfiguration contextConfiguration(Path cachePath) {
    return new ContextConfiguration(
        FhirRelease.asR4(),
        "de",
        DisplayBehaviorConfiguration.defaultConfiguration(),
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED, cachePath),
        TerminologyConfiguration.defaultConfiguration(),
        ValidationPolicyConfiguration.defaultConfiguration());
  }
}
