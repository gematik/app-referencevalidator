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

import de.gematik.refv.lib.config_parser.boundary.JSONMapperProvider;
import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.SnapshotGenerationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
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
import java.util.LinkedHashMap;
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
      "R1.6 — successfully generated packages are archived in the supplied output directory")
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
    Assertions.assertTrue(Files.isRegularFile(outputDir.resolve("my.pkg-1.0.0.tgz")));
  }

  @DisplayName("R1.2 — the complete dependency graph resolves before any snapshot generation")
  @Test
  void resolvesCompleteGraphBeforeStartingSnapshotGeneration() throws Exception {
    Path packageCache = tempDir.resolve("package-cache");
    Files.createDirectories(packageCache);
    writePackage(packageCache, "first#1.0.0", Map.of());
    var dependencies = new LinkedHashMap<String, String>();
    dependencies.put("first", "1.0.0");
    dependencies.put("missing", "1.0.0");
    LocalDirectory root = writePackage("root#1.0.0", dependencies);
    ProcessorResult processorResult = processPackages(List.of(resolved(root)), packageCache);

    try (var processor = processorResult.processor()) {
      var result =
          processor.process(root.path(), processorResult.outputDirectory(), null, List.of());

      Assertions.assertTrue(
          result.messages().stream()
              .anyMatch(message -> message.severity() == IssueSeverity.ERROR));
      verify(processorResult.context(), never()).generateSnapshots(any(Path.class));
    }
  }

  @DisplayName("R1.4 — the complete graph is generated dependency-first with dependency archives")
  @Test
  void resolvesTransitiveGraphBeforeGeneratingInDependencyOrder() throws Exception {
    Path packageCache = tempDir.resolve("package-cache");
    Files.createDirectories(packageCache);
    LocalDirectory leaf = writePackage(packageCache, "leaf#1.0.0", Map.of());
    LocalDirectory dependency =
        writePackage(packageCache, "dependency#1.0.0", Map.of("leaf", "1.0.0"));
    LocalDirectory root = writePackage("root#1.0.0", Map.of("dependency", "1.0.0"));
    ProcessorResult processorResult = processPackages(List.of(resolved(root)), packageCache);
    List<String> generatedCoordinates = new ArrayList<>();
    List<String> rootDependencyCoordinates = new ArrayList<>();
    try (var context = processorResult.context()) {
      when(context.cloneContext(anyCollection()))
          .thenAnswer(
              invocation -> {
                var dependencies = invocation.<List<ResolvedPackage>>getArgument(0);
                rootDependencyCoordinates.clear();
                rootDependencyCoordinates.addAll(
                    dependencies.stream().map(pkg -> pkg.id().coordinates()).toList());
                return processorResult.context();
              });
      when(context.generateSnapshots(any(Path.class)))
          .thenAnswer(
              invocation -> {
                Path packagePath = invocation.getArgument(0);
                generatedCoordinates.add(packageCoordinates(packagePath));
                return SnapshotGenerationResult.forMessage(
                    IssueSeverity.INFORMATION,
                    MessageId.NO_ERROR.getCode(),
                    "generation succeeded");
              });
    }

    try (var processor = processorResult.processor()) {
      processor.process(root.path(), processorResult.outputDirectory(), null, List.of());
      Assertions.assertEquals(
          List.of(leaf.id().coordinates(), dependency.id().coordinates(), root.id().coordinates()),
          generatedCoordinates);
      Assertions.assertEquals(
          List.of(leaf.id().coordinates(), dependency.id().coordinates()),
          rootDependencyCoordinates);
    }
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

  @DisplayName("R1.7 — changed matching patches trigger snapshot generation again")
  @Test
  void changedPackagePatchInvalidatesCachedSnapshot() throws Exception {
    LocalDirectory pkg = writePackage("patched#1.0.0", Map.of());
    Files.writeString(pkg.path().resolve("package/patched.txt"), "source");
    Path patchesRoot = Files.createDirectories(tempDir.resolve("patches"));
    Path packagePatchDirectory = Files.createDirectories(patchesRoot.resolve("patched#1.0.0"));
    Path patchFile = packagePatchDirectory.resolve("patched.txt");
    Files.writeString(patchFile, "first patch");
    ProcessorResult processorResult = processPackages(List.of(resolved(pkg)));
    List<String> observedPatches = new ArrayList<>();

    try (var context = processorResult.context()) {
      when(context.generateSnapshots(any(Path.class)))
          .thenAnswer(
              invocation -> {
                Path packagePath = invocation.getArgument(0);
                observedPatches.add(Files.readString(packagePath.resolve("package/patched.txt")));
                return SnapshotGenerationResult.forMessage(
                    IssueSeverity.INFORMATION,
                    MessageId.NO_ERROR.getCode(),
                    "generation succeeded");
              });
    }

    try (var processor = processorResult.processor()) {
      processor.process(pkg.path(), processorResult.outputDirectory(), patchesRoot, List.of());
      Files.writeString(patchFile, "second patch");
      processor.process(pkg.path(), processorResult.outputDirectory(), patchesRoot, List.of());
      var unchangedPatchResult =
          processor.process(pkg.path(), processorResult.outputDirectory(), patchesRoot, List.of());

      Assertions.assertEquals(List.of("first patch", "second patch"), observedPatches);
      Assertions.assertTrue(
          unchangedPatchResult.messages().stream()
              .anyMatch(
                  message -> message.messageContent().contains("Reused cached snapshot archive")));
    }
  }

  @DisplayName("Different package groups reuse an already generated shared dependency snapshot")
  @Test
  void differentPackageGroupsReuseSharedDependencySnapshots() throws Exception {
    Path packageCache = tempDir.resolve("package-cache");
    Files.createDirectories(packageCache);
    LocalDirectory shared = writePackage(packageCache, "shared#1.0.0", Map.of());
    LocalDirectory firstRoot = writePackage("group-a#1.0.0", Map.of("shared", "1.0.0"));
    LocalDirectory secondRoot = writePackage("group-b#1.0.0", Map.of("shared", "1.0.0"));
    ProcessorResult firstGroup = processPackages(List.of(resolved(firstRoot)), packageCache);
    List<String> firstGroupGenerations = new ArrayList<>();
    try (var context = firstGroup.context()) {
      when(context.generateSnapshots(any(Path.class)))
          .thenAnswer(
              invocation -> {
                firstGroupGenerations.add(packageCoordinates(invocation.getArgument(0)));
                return SnapshotGenerationResult.forMessage(
                    IssueSeverity.INFORMATION,
                    MessageId.NO_ERROR.getCode(),
                    "generation succeeded");
              });
    }

    try (var processor = firstGroup.processor()) {
      processor.process(firstRoot.path(), firstGroup.outputDirectory(), null, List.of());
    }

    ProcessorResult secondGroup = processPackages(List.of(resolved(secondRoot)), packageCache);
    List<String> secondGroupGenerations = new ArrayList<>();
    List<String> secondGroupDependencies = new ArrayList<>();
    try (var context = secondGroup.context()) {
      when(context.cloneContext(anyCollection()))
          .thenAnswer(
              invocation -> {
                var dependencies = invocation.<List<ResolvedPackage>>getArgument(0);
                secondGroupDependencies.addAll(
                    dependencies.stream().map(pkg -> pkg.id().coordinates()).toList());
                return context;
              });
      when(context.generateSnapshots(any(Path.class)))
          .thenAnswer(
              invocation -> {
                secondGroupGenerations.add(packageCoordinates(invocation.getArgument(0)));
                return SnapshotGenerationResult.forMessage(
                    IssueSeverity.INFORMATION,
                    MessageId.NO_ERROR.getCode(),
                    "generation succeeded");
              });
    }

    try (var processor = secondGroup.processor()) {
      var result =
          processor.process(secondRoot.path(), secondGroup.outputDirectory(), null, List.of());

      Assertions.assertEquals(
          List.of(shared.id().coordinates(), firstRoot.id().coordinates()), firstGroupGenerations);
      Assertions.assertEquals(List.of(secondRoot.id().coordinates()), secondGroupGenerations);
      Assertions.assertEquals(List.of(shared.id().coordinates()), secondGroupDependencies);
      Assertions.assertTrue(
          hasMessage(result, "Reused cached snapshot archive for " + shared.id().coordinates()));
    }
  }

  @DisplayName("Given an unselected root package, when processing, then it reports no match")
  @Test
  void reportsNoMatchingRequestedRoot() throws Exception {
    LocalDirectory root = writePackage("root#1.0.0", Map.of());
    ProcessorResult processorResult = processPackages(List.of(resolved(root)));
    try (var processor = processorResult.processor()) {
      var result =
          processor.process(
              root.path(), processorResult.outputDirectory(), null, List.of("other#1.0.0"));

      Assertions.assertTrue(hasMessage(result, "None of the resolved packages matched"));
      verify(processorResult.context(), never()).cloneContext(anyCollection());
    }
  }

  @DisplayName("R1.3 — a cyclic dependency graph fails before generation")
  @Test
  void reportsDependencyCycle() throws Exception {
    Path packageCache = tempDir.resolve("package-cache");
    Files.createDirectories(packageCache);
    LocalDirectory root = writePackage("root#1.0.0", Map.of("dependency", "1.0.0"));
    writePackage(packageCache, "dependency#1.0.0", Map.of("root", "1.0.0"));
    writePackage(packageCache, "root#1.0.0", Map.of());
    ProcessorResult processorResult = processPackages(List.of(resolved(root)), packageCache);

    try (var processor = processorResult.processor()) {
      var result =
          processor.process(root.path(), processorResult.outputDirectory(), null, List.of());

      Assertions.assertTrue(
          hasMessage(
              result,
              "Cyclic package dependency detected: root#1.0.0 -> dependency#1.0.0 -> root#1.0.0"));
      verify(processorResult.context(), never()).generateSnapshots(any(Path.class));
    }

    LocalDirectory unresolvedRoot =
        writePackage("unresolved-root#1.0.0", Map.of("missing", "1.0.0"));
    ProcessorResult unresolvedGraph =
        processPackages(List.of(resolved(unresolvedRoot)), packageCache);
    try (var processor = unresolvedGraph.processor()) {
      var unresolvedResult =
          processor.process(
              unresolvedRoot.path(), unresolvedGraph.outputDirectory(), null, List.of());

      Assertions.assertTrue(
          unresolvedResult.messages().stream()
              .anyMatch(message -> message.severity() == IssueSeverity.ERROR));
      verify(unresolvedGraph.context(), never()).generateSnapshots(any(Path.class));
    }
  }

  @DisplayName("R1.5 — a repeated package coordinate is generated once per request")
  @Test
  void processesSharedDependencyOncePerInvocation() throws Exception {
    Path packageCache = tempDir.resolve("package-cache");
    Files.createDirectories(packageCache);
    LocalDirectory firstRoot = writePackage("first#1.0.0", Map.of("shared", "1.0.0"));
    LocalDirectory secondRoot = writePackage("second#1.0.0", Map.of("shared", "1.0.0"));
    writePackage(packageCache, "shared#1.0.0", Map.of());
    ProcessorResult processorResult =
        processPackages(
            List.of(resolved(firstRoot), resolved(secondRoot), resolved(firstRoot)), packageCache);

    try (var processor = processorResult.processor()) {
      var result =
          processor.process(firstRoot.path(), processorResult.outputDirectory(), null, List.of());

      Assertions.assertFalse(
          result.messages().stream()
              .anyMatch(message -> message.severity() == IssueSeverity.ERROR));
      verify(processorResult.context(), times(3)).generateSnapshots(any(Path.class));
    }
  }

  @DisplayName("R1.8 — a failed dependency blocks dependents but not unrelated packages")
  @Test
  void dependencyFailureDoesNotBlockUnrelatedRoot() throws Exception {
    Path packageCache = tempDir.resolve("package-cache");
    Files.createDirectories(packageCache);
    LocalDirectory dependency = writePackage(packageCache, "dependency#1.0.0", Map.of());
    LocalDirectory dependent = writePackage("dependent#1.0.0", Map.of("dependency", "1.0.0"));
    LocalDirectory unrelated = writePackage("unrelated#1.0.0", Map.of());
    ProcessorResult processorResult =
        processPackages(List.of(resolved(dependent), resolved(unrelated)), packageCache);
    List<String> generatedPackages = new ArrayList<>();
    try (var context = processorResult.context()) {
      when(context.generateSnapshots(any(Path.class)))
          .thenAnswer(
              invocation -> {
                Path packagePath = invocation.getArgument(0);
                String coordinates = packageCoordinates(packagePath);
                generatedPackages.add(coordinates);
                return coordinates.equals(dependency.id().coordinates())
                    ? SnapshotGenerationResult.forMessage(
                        IssueSeverity.ERROR,
                        MessageId.SNAPSHOT_GENERATION_ERROR.getCode(),
                        "dependency generation failed")
                    : SnapshotGenerationResult.forMessage(
                        IssueSeverity.INFORMATION,
                        MessageId.NO_ERROR.getCode(),
                        "generation succeeded");
              });
    }

    try (var processor = processorResult.processor()) {
      var result =
          processor.process(dependent.path(), processorResult.outputDirectory(), null, List.of());
      Assertions.assertTrue(generatedPackages.contains(dependency.id().coordinates()));
      Assertions.assertTrue(generatedPackages.contains(unrelated.id().coordinates()));
      Assertions.assertFalse(generatedPackages.contains(dependent.id().coordinates()));
      Assertions.assertTrue(
          result.messages().stream()
              .anyMatch(message -> message.severity() == IssueSeverity.ERROR));
    }
  }

  @DisplayName("R1.9 — ERROR and FATAL generation messages fail the request and retain outcomes")
  @Test
  void errorAndFatalGenerationMessagesFailRequestWithPackageOutcomes() throws Exception {
    LocalDirectory root = writePackage("root#1.0.0", Map.of());
    ProcessorResult processorResult = processPackages(List.of(resolved(root)));
    try (var context = processorResult.context()) {
      when(context.generateSnapshots(any(Path.class)))
          .thenReturn(
              new SnapshotGenerationResult(
                  List.of(
                      ResultMessage.fromMessage(
                          IssueSeverity.ERROR,
                          MessageId.SNAPSHOT_GENERATION_ERROR.getCode(),
                          "ERROR outcome for root#1.0.0"),
                      ResultMessage.fromMessage(
                          IssueSeverity.FATAL,
                          MessageId.SNAPSHOT_GENERATION_ERROR.getCode(),
                          "FATAL outcome for root#1.0.0"))));
      try (var processor = processorResult.processor()) {
        var result =
            processor.process(root.path(), processorResult.outputDirectory(), null, List.of());

        Assertions.assertTrue(
            result.messages().stream()
                .anyMatch(message -> message.severity() == IssueSeverity.ERROR));
        Assertions.assertTrue(
            result.messages().stream()
                .anyMatch(message -> message.severity() == IssueSeverity.FATAL));
        Assertions.assertTrue(
            result.messages().stream()
                .anyMatch(message -> message.messageContent().contains("root#1.0.0")));
      }
    }
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
      try (var processor = processorResult.processor()) {
        var result =
            processor.process(root.path(), processorResult.outputDirectory(), null, List.of());

        Assertions.assertTrue(
            result.messages().stream()
                .anyMatch(message -> message.severity() == IssueSeverity.ERROR));
        try (var outputFiles = Files.list(processorResult.outputDirectory())) {
          Assertions.assertTrue(outputFiles.findAny().isEmpty());
        }
      }
    }
  }

  @DisplayName("Given blank requested coordinates, when processing, then validation rejects them")
  @Test
  void rejectsBlankRequestedCoordinates() throws Exception {
    LocalDirectory root = writePackage("root#1.0.0", Map.of());
    ProcessorResult processorResult = processPackages(List.of(resolved(root)));
    try (var processor = processorResult.processor()) {
      var sourcePath = root.path();
      var outputDirectory = processorResult.outputDirectory();
      var requestedCoordinates = List.of(" ");

      Assertions.assertThrows(
          IllegalArgumentException.class,
          () -> processor.process(sourcePath, outputDirectory, null, requestedCoordinates));
    }
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

  private String packageCoordinates(Path packagePath) throws IOException {
    var packageJson =
        JSONMapperProvider.getMapper()
            .readTree(
                packagePath
                    .resolve(PackageDependencyExtractor.PACKAGE_JSON_RELATIVE_PATH)
                    .toFile());
    return packageJson.get("name").asText() + "#" + packageJson.get("version").asText();
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
