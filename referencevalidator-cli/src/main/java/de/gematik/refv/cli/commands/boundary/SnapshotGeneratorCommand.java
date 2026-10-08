/*-
 * #%L
 * referencevalidator-cli
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
package de.gematik.refv.cli.commands.boundary;

import de.gematik.refv.cli.BaseCommand;
import de.gematik.refv.cli.commands.VersionProvider;
import de.gematik.refv.cli.commands.entity.ContextArguments;
import de.gematik.refv.cli.commands.entity.ReportArguments;
import de.gematik.refv.cli.commands.entity.SnapshotGenerationModuleArguments;
import de.gematik.refv.cli.commands.entity.SnapshotGeneratorArguments;
import de.gematik.refv.cli.commands.entity.TerminologyArguments;
import de.gematik.refv.cli.config.boundary.ConfigLoader;
import de.gematik.refv.cli.config.entity.SnapshotCliConfig;
import de.gematik.refv.cli.config.entity.SnapshotModuleConfiguration;
import de.gematik.refv.cli.report.boundary.ResultReporter;
import de.gematik.refv.lib.config_parser.boundary.YAMLMapperProvider;
import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.fhir_context.entity.ContextResult;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolver;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolverFactory;
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.snapshot.boundary.SnapshotGenerator;
import de.gematik.refv.lib.snapshot.boundary.SnapshotGeneratorFactory;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationOptions;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationRequest;
import de.gematik.refv.lib.valmodule.boundary.ModuleConfigImporter;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(
    name = SnapshotGeneratorCommand.COMMAND_NAME,
    mixinStandardHelpOptions = true,
    version = VersionProvider.PROJECT_VERSION,
    description =
        """

                  Generates a snapshot from a profile or a folder containing dependencies.
                  """)
public class SnapshotGeneratorCommand extends BaseCommand {

  private final Logger log = LoggerFactory.getLogger(SnapshotGeneratorCommand.class);
  // keeps the time of execution
  private final ResultReporter reporter = new ResultReporter();
  public static final String COMMAND_NAME = "generate-snapshot";

  @CommandLine.Option(
      names = {"-s", "--source"},
      description =
          "Remote Package Coordinates (config#version) or the path to a single package (.tgz) or the path to a directory containing dependencies",
      required = false)
  private Path sourcePackagePath;

  @CommandLine.Option(
      names = {"-o", "--output-dir"},
      description = "Output directory for snapshots or JSON reports")
  private Path outputDir;

  @CommandLine.Option(
      names = {"-r", "--report"},
      description = "Write validation report to a specified file path",
      required = false)
  private Path reportFilePath;

  @CommandLine.Option(
      names = {"-j", "--json"},
      description = "Write the logs and the reports as JSON",
      required = false)
  private boolean shouldUseJson;

  @CommandLine.Option(
      names = {"-c", "--config"},
      description =
          "Path to YAML configuration file to use as source for the generation of snapshots",
      required = false)
  private Path configFilePath;

  @CommandLine.Option(
      names = {"-p", "--packages"},
      split = ",",
      description =
          "Comma-separated list of package coordinates (e.g. config#version) to generate snapshots for. If omitted, all FHIR dependencies are processed.",
      required = false)
  private final Set<String> userPackageNames = Set.of();

  @CommandLine.Option(
      names = {"--module-manifest"},
      description =
          "The Validation Module Manifest YAML to use as source for the generation of snapshots",
      required = false)
  private Path moduleManifest;

  @CommandLine.Option(
      names = {"--patches-dir"},
      description = "Path to directory containing patches for dependencies",
      required = false)
  private Path patchesDir;

  @CommandLine.Option(
      names = {"--cache-dir"},
      description = "Path to directory used as cache for the processing of packages",
      required = false)
  private Path cacheDir;

  @CommandLine.Option(
      names = {"-f", "--fhir-version"},
      description = "FHIR Version to validate against (e.g. R4, R5, etc. - Defaults to R4)")
  private String fhirVersion;

  @CommandLine.Option(
      names = {"--use-terminology-server"},
      description =
          "Allows the usage of a terminology server for expanding ValueSets before validation",
      required = false)
  private boolean shouldUseTerminologyServer;

  @CommandLine.Option(
      names = {"-t", "--terminology-server"},
      description = "URL of the Terminology Service to be used for fetching resources",
      required = false)
  private String terminologyServer;

  @CommandLine.Option(
      names = {"--offline-mode"},
      description = "Disables the download of remote dependencies",
      required = false)
  private boolean shouldRunInOfflineMode;

  @CommandLine.Option(
      names = {"-e", "--expand-valuesets"},
      description = "Pre-expand all referenced ValueSets before validation",
      required = false)
  private boolean shouldExpandValueSets;

  @CommandLine.Option(
      names = {"--locale"},
      description = "The Language to be used for the Validation Messages (e.g. de, en-GB, en-US)",
      required = false)
  private String locale;

  public SnapshotGeneratorCommand() {
    super();
  }

  @Override
  public Integer call() throws Exception {
    try {
      super.call();

      final var configLoader = new ConfigLoader();
      final var cliConfig =
          configFilePath != null
              ? configLoader.loadSnapshotConfigFromPath(configFilePath)
              : configLoader.fromSnapshotGeneratorArguments(
                  new SnapshotGeneratorArguments(
                      new ContextArguments(
                          fhirVersion,
                          locale,
                          cacheDir,
                          new TerminologyArguments(shouldUseTerminologyServer, terminologyServer),
                          true,
                          true,
                          !shouldRunInOfflineMode),
                      new SnapshotGenerationModuleArguments(
                          moduleManifest, sourcePackagePath, patchesDir),
                      new ReportArguments(reportFilePath, shouldUseJson)));

      log.info("### User configuration loaded successfully");

      if (Files.notExists(outputDir)) {
        log.info("Creating output directory {}", outputDir);
        Files.createDirectories(outputDir);
      }

      // Compare found dependencies with passed information over CLI
      final var validPackageNames =
          userPackageNames.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();

      final Map<String, ContextResult> resultMap = new HashMap<>();
      if (Objects.nonNull(cliConfig.module())) {
        resultMap.putAll(snapshotGenerationWithModule(cliConfig));
      } else {
        log.info("### Initializing the Snapshot Generator");
        try (SnapshotGenerator snapshotGenerator =
            SnapshotGeneratorFactory.fromConfiguration(cliConfig.context())) {
          resultMap.put(
              sourcePackagePath.getFileName().toString(),
              snapshotGenerator.generateSnapshots(
                  new SnapshotGenerationRequest(sourcePackagePath, outputDir, validPackageNames),
                  new SnapshotGenerationOptions(patchesDir)));
        }
      }
      boolean resultValid = reporter.generateReport(resultMap);

      if (shouldUseJson) {
        reporter.writeJsonReport(resultMap, cliConfig.report().filePath());
      } else {
        reporter.writeHtmlReport(
            resultMap,
            cliConfig.report().filePath(),
            cliConfig.context().fhirRelease().corePackageVersion());
      }
      return resultValid ? 0 : 1;
    } catch (Exception e) {
      log.error("Failed to generate snapshots: {}", e.getLocalizedMessage());
      log.debug(e.getLocalizedMessage(), e);
      return 1;
    }
  }

  private Map<String, ContextResult> snapshotGenerationWithModule(
      @NonNull SnapshotCliConfig cliConfig) {

    final Map<String, ContextResult> resultMap = new HashMap<>();
    log.info("Loading module {}...", Objects.requireNonNull(cliConfig.module()).manifestPath());

    // read the packages to load
    final var packageGroups = getPackageGroupsForModule(cliConfig.module());
    final var packagesToGenerate =
        packageGroups.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    // import packages in cache
    cacheModulePackages(
        cliConfig.context().packageLoading(),
        cliConfig.module().sourcePackagesPath(),
        packagesToGenerate);
    // Filter Packages
    final var filteredPackageGroups = filterPackageGroupsByUser(packageGroups);
    log.debug("Package groups to process: {}", filteredPackageGroups);

    for (var packageGroup : filteredPackageGroups.entrySet()) {
      log.info(
          "### Initializing the Snapshot Generator for package group {}", packageGroup.getKey());
      try (SnapshotGenerator snapshotGenerator =
          SnapshotGeneratorFactory.fromConfiguration(cliConfig.context())) {
        for (var packageName : packageGroup.getValue()) {
          final var result =
              snapshotGenerator.generateSnapshots(
                  new SnapshotGenerationRequest(
                      cliConfig.context().packageLoading().cachePath().resolve(packageName),
                      outputDir,
                      packageGroup.getValue()),
                  new SnapshotGenerationOptions(cliConfig.module().patchesPath()));
          resultMap.put(packageName, result);
        }
      }
    }
    return resultMap;
  }

  private @NonNull Map<String, List<String>> filterPackageGroupsByUser(
      @NonNull Map<String, List<String>> packageGroups) {
    return packageGroups.entrySet().stream()
        .map(
            group ->
                Map.entry(
                    group.getKey(),
                    group.getValue().stream()
                        .filter(
                            packageName ->
                                userPackageNames.isEmpty()
                                    || userPackageNames.contains(packageName))
                        .map(
                            packageName ->
                                packageName.endsWith(LocalArchive.ARCHIVE_PACKAGE_EXTENSION)
                                    ? LocalArchive.parse(packageName).coordinates()
                                    : packageName)
                        .toList()))
        .filter(group -> !group.getValue().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static void cacheModulePackages(
      @NonNull PackageDownloadConfiguration packageDownloadConfiguration,
      @NonNull Path packagesDir,
      @NonNull Set<String> packagesToCache) {
    final PackageResolver packageResolver =
        PackageResolverFactory.withConfiguration(packageDownloadConfiguration);

    for (var packageEntry : packagesToCache) {
      if (packageEntry.contains(PackageId.PACKAGE_SEPARATOR)) {
        packageResolver.resolvePath(Path.of(packageEntry));
      } else if (packageEntry.endsWith(LocalArchive.ARCHIVE_PACKAGE_EXTENSION)) {
        packageResolver.resolvePath(packagesDir.resolve(packageEntry));
      }
    }
  }

  private static Map<String, List<String>> getPackageGroupsForModule(
      @NonNull SnapshotModuleConfiguration snapshotConfiguration) {
    final var moduleConfigImporter = new ModuleConfigImporter(YAMLMapperProvider.getMapper());
    try (var is = new FileInputStream(snapshotConfiguration.manifestPath().toFile())) {
      final var moduleConfig = moduleConfigImporter.importFromStream(is);
      if (moduleConfig.isEmpty()) {
        throw new InitializationException("Failed to parse the module configuration from file");
      }

      return moduleConfig.get().packageGroups().entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().packages()));

    } catch (Exception e) {
      throw new InitializationException("Failed to load configuration", e);
    }
  }
}
