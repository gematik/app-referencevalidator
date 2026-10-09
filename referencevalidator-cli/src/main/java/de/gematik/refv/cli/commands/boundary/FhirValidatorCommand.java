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
import de.gematik.refv.cli.commands.entity.TerminologyArguments;
import de.gematik.refv.cli.commands.entity.ValidationArguments;
import de.gematik.refv.cli.commands.entity.ValidationModuleArguments;
import de.gematik.refv.cli.config.boundary.ConfigLoader;
import de.gematik.refv.cli.config.entity.ValidationCliConfig;
import de.gematik.refv.cli.report.boundary.ResultReporter;
import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.exceptions.LoadModuleException;
import de.gematik.refv.lib.exceptions.ParsingException;
import de.gematik.refv.lib.exceptions.UnsupportedFileTypeException;
import de.gematik.refv.lib.exceptions.ValidationException;
import de.gematik.refv.lib.fhir_context.boundary.BatchValidationContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ContextResult;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.fhir_context.entity.ValidationModuleIndex;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolverFactory;
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.validation.boundary.ValidationPackageSelector;
import de.gematik.refv.lib.validation.boundary.Validator;
import de.gematik.refv.lib.validation.boundary.ValidatorFactory;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.JsonFhirResource;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationRequest;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import de.gematik.refv.lib.validation.entity.XmlFhirResource;
import de.gematik.refv.lib.valmodule.boundary.ModuleLoader;
import de.gematik.refv.lib.valmodule.entity.ValidationModule;
import de.gematik.refv.valmodule.api.entity.MessageTransformation;
import de.gematik.refv.valmodule.api.entity.SuppressionRule;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** Entrypoint Command for the validation of FHIR Resources. */
@CommandLine.Command(
    name = FhirValidatorCommand.COMMAND_NAME,
    mixinStandardHelpOptions = true,
    version = VersionProvider.PROJECT_VERSION,
    description =
        """

        Validates a list of comma-separated Files, using available plugins on the class path, with or without an external terminology server.
        """)
public class FhirValidatorCommand extends BaseCommand {
  private static final Logger log = LoggerFactory.getLogger(FhirValidatorCommand.class);
  // keeps the time of execution
  private final ResultReporter reporter = new ResultReporter();
  public static final String COMMAND_NAME = "validate";

  @CommandLine.Parameters(
      paramLabel = "FILE",
      description = "FHIR resource file(s)",
      arity = "0..*")
  private List<String> sources = new ArrayList<>();

  @CommandLine.Option(
      names = {"-c", "--config"},
      description = "Path to YAML configuration file")
  private String configPath;

  @CommandLine.Option(
      names = {"--modules-folder"},
      description = "Folder containing Validation Modules as JAR files")
  private Path modulesDir;

  @CommandLine.Option(
      names = {"--module-name"},
      description = "The Validation Module to use")
  private String moduleName;

  @CommandLine.Option(
      names = {"-j", "--json"},
      description = "Write the logs and the reports as JSON",
      defaultValue = "false")
  private boolean shouldUseJson;

  @CommandLine.Option(
      names = {"-r", "--report"},
      description = "Write validation report to a specified file path",
      defaultValue = "output.html")
  private Path reportFilePath;

  @CommandLine.Option(
      names = {"--use-terminology-server"},
      description =
          "Allows the usage of a terminology server for expanding ValueSets before validation")
  private boolean shouldUseTerminologyServer;

  @CommandLine.Option(
      names = {"-f", "--fhir-version"},
      description = "FHIR Version to validate against (e.g. R4, R5, etc. - Defaults to R4)",
      defaultValue = "R4")
  private String fhirVersion;

  @CommandLine.Option(
      names = {"-t", "--terminology-server"},
      description = "URL of the Terminology Service to be used for fetching resources")
  private String terminologyServer;

  @CommandLine.Option(
      names = {"--allow-example-urls"},
      description =
          "Specifies if a FHIR resource can have examples URLs (e.g. http://example.org/CodeSystem/123) that are not resolvable, and should be ignored for validation purposes")
  private boolean shouldAllowExampleUrls;

  @CommandLine.Option(
      names = {"--parallel-threads"},
      description = "Maximum number of validations running concurrently (1-32)",
      defaultValue = "5")
  private int parallelThreads;

  @CommandLine.Option(
      names = "--max-input-files",
      description = "Maximum number of FHIR resources accepted in one batch",
      defaultValue = "1000")
  private int maximumInputFiles;

  @CommandLine.Option(
      names = "--max-file-bytes",
      description = "Maximum size of an individual FHIR resource in bytes",
      defaultValue = "52428800")
  private long maximumFileBytes;

  @CommandLine.Option(
      names = "--max-total-bytes",
      description = "Maximum combined size of FHIR resources in bytes",
      defaultValue = "524288000")
  private long maximumTotalBytes;

  @CommandLine.Option(
      names = "--max-directory-depth",
      description = "Maximum number of nested directories below each input directory",
      defaultValue = "32")
  private int maximumDirectoryDepth;

  @CommandLine.Option(
      names = {"-p", "--profile"},
      description = "Canonical url of a profile to validate against")
  private String profileToUse;

  @CommandLine.Option(
      names = {"--implementation-guide"},
      description = "Implementation Guide to use for validating, if available")
  private String implementationGuideToUse;

  @CommandLine.Option(
      names = {"-v", "--verbose"},
      description = "Print additional INFORMATION/WARNING validation messages")
  private boolean shouldPrintExtraMessages;

  @CommandLine.Option(
      names = {"--skip-validity-period-checks"},
      description = "Disables the pre-configured validity periods for given profiles",
      defaultValue = "false")
  private boolean shouldSkipValidityPeriodChecks;

  @CommandLine.Option(
      names = {"--online-mode"},
      description = "Enables the download of remote dependencies",
      defaultValue = "false",
      required = true)
  private boolean shouldRunInOnlineMode;

  @CommandLine.Option(
      names = {"--locale"},
      description = "The Language to be used for the Validation Messages (e.g. de, en-GB or en-US)",
      defaultValue = "de")
  private String locale;

  public FhirValidatorCommand() {
    super();
  }

  @Override
  public Integer call() {
    try {
      super.call();

      final var configLoader = new ConfigLoader();
      final var cliConfig =
          configPath != null
              ? configLoader.loadValidationConfigFromPath(Path.of(configPath))
              : configLoader.fromValidationArguments(
                  new ValidationArguments(
                      new ValidationModuleArguments(modulesDir, moduleName),
                      new ContextArguments(
                          fhirVersion,
                          locale,
                          null,
                          new TerminologyArguments(shouldUseTerminologyServer, terminologyServer),
                          shouldPrintExtraMessages,
                          shouldAllowExampleUrls,
                          shouldRunInOnlineMode),
                      profileToUse,
                      shouldSkipValidityPeriodChecks,
                      new ReportArguments(reportFilePath, shouldUseJson)));

      if (cliConfig == null) {
        throw new InitializationException("Failed to load the User configuration");
      }

      if (parallelThreads < 1
          || parallelThreads > BoundedValidationExecutor.MAX_IN_FLIGHT_VALIDATIONS) {
        throw new InitializationException(
            "--parallel-threads must be between 1 and "
                + BoundedValidationExecutor.MAX_IN_FLIGHT_VALIDATIONS);
      }
      final var inputLimits =
          new ValidationInputCollector.Limits(
              maximumInputFiles, maximumFileBytes, maximumTotalBytes, maximumDirectoryDepth);

      log.debug("User configuration loaded successfully");

      final ContextConfiguration contextConfig;
      final ValidationPackageSelector validationPackageSelector;
      final Optional<ValidationModule> validationModule;
      var validationCliConfig = cliConfig;
      if (Objects.nonNull(cliConfig.module())) {
        log.info("Attempting to load module '{}'...", cliConfig.module().name());
        validationModule =
            ModuleLoader.defaultLoader()
                .forValidation(cliConfig.module().directory(), cliConfig.module().name());
        if (validationModule.isEmpty()) {
          throw new LoadModuleException("Could not find a suitable module");
        }
        final var validationModuleIndex = new ValidationModuleIndex(validationModule.get());
        validationPackageSelector = new ValidationPackageSelector(validationModuleIndex);
        validationCliConfig =
            new ValidationCliConfig(
                cliConfig.context(),
                cliConfig.module(),
                withValidationModuleOptions(
                    cliConfig.validationOptions(),
                    validationModuleIndex.getValidationModuleConfiguration()),
                cliConfig.report());
        // Merge the module's engine-level validation settings into the context configuration
        contextConfig = withModuleValidation(cliConfig.context(), validationModuleIndex);
      } else {
        log.info("No explicit module selected, using core definitions");
        validationModule = Optional.empty();
        contextConfig = cliConfig.context();
        validationPackageSelector = new ValidationPackageSelector();
      }
      final var validationSources = getFilesForValidation(sources, inputLimits);
      preloadCache(cliConfig.context(), validationModule.orElse(null));
      log.info("Loading {} files for the validation", validationSources.size());
      final Map<String, ContextResult> resultMap = new HashMap<>();

      try (var batchContexts =
          BatchValidationContextProvider.defaultProvider(
              ContextProvider.defaultProvider(), contextConfig, parallelThreads)) {
        if (validationSources.size() == 1) {
          final var singleResult =
              validateSingle(
                  validationSources.getFirst(),
                  validationCliConfig,
                  validationPackageSelector,
                  batchContexts);
          resultMap.put(validationSources.toString(), singleResult);
        } else {
          var concurrentResult =
              validateConcurrently(
                  validationSources, validationPackageSelector, validationCliConfig, batchContexts);
          resultMap.putAll(concurrentResult);
        }
      }

      boolean overallValid = reporter.generateReport(resultMap);
      if (shouldUseJson) {
        reporter.writeJsonReport(resultMap, cliConfig.report().filePath());
      } else {
        reporter.writeHtmlReport(
            resultMap,
            cliConfig.report().filePath(),
            cliConfig.context().fhirRelease().corePackageVersion());
      }
      return overallValid ? 0 : 1;
    } catch (Exception e) {
      log.error("FHIR Validation failed with error: {}", e.getMessage());
      log.debug("Stack trace:", e);
      return 1;
    }
  }

  private ValidationResult validateSingle(
      ValidationInputCollector.ValidationInput validationSource,
      ValidationCliConfig cliConfig,
      ValidationPackageSelector validationPackageSelector,
      BatchValidationContextProvider batchContexts)
      throws IOException, ValidationException {
    final FhirResource resource;
    try {
      resource = createResource(validationSource);
    } catch (UnsupportedFileTypeException e) {
      return new ValidationResult(
          List.of(
              ResultMessage.fromMessage(
                  IssueSeverity.INFORMATION,
                  MessageId.IO_ERROR.getCode(),
                  e.getLocalizedMessage())));
    }
    final var packagesToLoad = detectPackagesToLoad(cliConfig, validationPackageSelector, resource);
    final var validationRequest = createRequest(resource);
    try (var lease = batchContexts.acquire(packagesToLoad);
        var context = lease.newIsolatedContext()) {
      final var validator = ValidatorFactory.withValidationContext(context);
      log.info("Starting the validation of {}", validationSource.path());
      return validator.validate(validationRequest, cliConfig.validationOptions());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ValidationException(e.getLocalizedMessage(), e);
    } catch (Exception e) {
      throw new ValidationException(e.getLocalizedMessage(), e);
    }
  }

  /**
   * Validates all sources concurrently using Virtual Threads.
   *
   * <p>Each virtual thread receives its own isolated {@link Validator} clone, removing the need for
   * synchronization across threads, if the package set to load is different.
   */
  private HashMap<String, ValidationResult> validateConcurrently(
      List<ValidationInputCollector.ValidationInput> sourcesToValidate,
      @NonNull ValidationPackageSelector validationPackageSelector,
      @NonNull ValidationCliConfig validationCliConfig,
      @NonNull BatchValidationContextProvider batchContexts) {
    // Every task validates through an isolated context cloned from its package-set template.
    List<Callable<Map.Entry<String, ValidationResult>>> tasks =
        new ArrayList<>(sourcesToValidate.size());
    for (var source : sourcesToValidate) {
      tasks.add(
          () -> {
            try {
              final var resource = createResource(source);
              final var packagesToLoad =
                  detectPackagesToLoad(validationCliConfig, validationPackageSelector, resource);
              final ValidationRequest validationRequest = createRequest(resource);
              try (var lease = batchContexts.acquire(packagesToLoad);
                  var context = lease.newIsolatedContext()) {
                final var validator = ValidatorFactory.withValidationContext(context);
                log.info("Validating '{}'", source.path());
                final var result =
                    validator.validate(validationRequest, validationCliConfig.validationOptions());
                return Map.entry(source.path().toString(), result);
              }
            } catch (UnsupportedFileTypeException e) {
              return Map.entry(
                  source.path().toString(),
                  new ValidationResult(
                      List.of(
                          ResultMessage.fromMessage(
                              IssueSeverity.INFORMATION,
                              MessageId.IO_ERROR.getCode(),
                              e.getLocalizedMessage()))));
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              return validationError(source, e);
            } catch (Exception e) {
              return validationError(source, e);
            }
          });
    }

    var results = new HashMap<String, ValidationResult>();
    try {
      for (var entry : BoundedValidationExecutor.runAll(tasks, parallelThreads)) {
        results.put(entry.getKey(), entry.getValue());
      }
    } catch (ExecutionException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      results.put(
          "PROCESS",
          new ValidationResult(
              List.of(
                  ResultMessage.fromMessage(
                      IssueSeverity.ERROR,
                      MessageId.INTERNAL_ERROR.getCode(),
                      "Failed to perform the validation: " + e.getLocalizedMessage()))));
    }

    return results;
  }

  private Map.Entry<String, ValidationResult> validationError(
      ValidationInputCollector.ValidationInput source, Exception exception) {
    return Map.entry(
        source.path().toString(),
        new ValidationResult(
            List.of(
                ResultMessage.fromMessage(
                    IssueSeverity.ERROR,
                    MessageId.VALIDATION_ERROR.getCode(),
                    exception.getLocalizedMessage()))));
  }

  private @NonNull List<ValidationInputCollector.ValidationInput> getFilesForValidation(
      @NonNull List<String> sources, ValidationInputCollector.Limits limits) throws IOException {
    return ValidationInputCollector.collect(sources, limits);
  }

  /** Merges the validation settings of the loaded module into the given context configuration. */
  private @NonNull ContextConfiguration withModuleValidation(
      @NonNull ContextConfiguration base, @NonNull ValidationModuleIndex moduleIndex) {
    final var moduleManifest = moduleIndex.getValidationModuleConfiguration();

    ValidationPolicyConfiguration validationPolicyConfiguration =
        new ValidationPolicyConfiguration(
            base.validationPolicy().validationLevelPolicy(),
            base.validationPolicy().unknownCodeSystemsPolicy(),
            base.validationPolicy().exampleCodeSystemsUsagePolicy(),
            base.validationPolicy().exampleUrlsUsagePolicy(),
            base.validationPolicy().exampleRestReferencesPolicy(),
            base.validationPolicy().recursiveModePolicy(),
            moduleManifest.anyExtensionsAllowed()
                ? ValidationPolicyConfiguration.AnyExtensionPolicy.ALLOWED
                : ValidationPolicyConfiguration.AnyExtensionPolicy.DISALLOWED);

    return new ContextConfiguration(
        base.fhirRelease(),
        base.locale(),
        base.displayBehavior(),
        base.packageLoading(),
        base.terminology(),
        validationPolicyConfiguration);
  }

  static @NonNull ValidationOptions withValidationModuleOptions(
      @NonNull ValidationOptions base, @NonNull ValidationModuleManifest moduleManifest) {

    List<SuppressionRule> suppressionRules = getSuppressionRules(base, moduleManifest);
    List<MessageTransformation> transformations = getTransformations(base, moduleManifest);

    return new ValidationOptions(
        base.profileToValidate(),
        base.profileFilterRegex(),
        base.validationMessagesFilterStrategy(),
        base.profileValidityPeriodCheckStrategy(),
        transformations,
        suppressionRules,
        moduleManifest.ignoredCodeSystems(),
        moduleManifest.ignoredValueSets());
  }

  private @NonNull ValidationRequest createRequest(@NonNull FhirResource fhirResource) {
    return new ValidationRequest(fhirResource);
  }

  private @NonNull FhirResource createResource(
      ValidationInputCollector.ValidationInput validationInput) throws IOException {
    Path filePath = validationInput.path();

    final var contentType = Files.probeContentType(filePath);
    if (Objects.isNull(contentType)) {
      throw new ParsingException("Failed to detect the type of file for " + filePath);
    }

    final var content =
        ValidationInputCollector.readResourceBytes(validationInput, maximumFileBytes);
    if (contentType.toLowerCase(Locale.ROOT).contains("json")) {
      return new JsonFhirResource(content);
    } else if (contentType.toLowerCase(Locale.ROOT).contains("xml")) {
      return new XmlFhirResource(content);
    }

    throw new UnsupportedFileTypeException(
        filePath + " has an unsupported file type: " + contentType);
  }

  private void preloadCache(
      @NonNull ContextConfiguration contextConfiguration,
      @Nullable ValidationModule validationModule) {
    final var packageResolver =
        PackageResolverFactory.withConfiguration(contextConfiguration.packageLoading());
    final var fhriReleaseString = contextConfiguration.fhirRelease().alias();
    log.info("Loading core definitions for FHIR {}", fhriReleaseString);
    packageResolver.loadCore(contextConfiguration.fhirRelease());
    if (validationModule != null) {
      log.info(
          "Loading definitions from Validation Module '{}'",
          validationModule.configuration().name());
      packageResolver.loadModulePackages(validationModule.modulePath());
    }
  }

  private @NonNull List<String> detectPackagesToLoad(
      @NonNull ValidationCliConfig cliConfig,
      @NonNull ValidationPackageSelector validationPackageSelector,
      @NonNull FhirResource resource) {
    // Core packages must always be loaded
    final List<String> packagesToLoad =
        new ArrayList<>(cliConfig.context().fhirRelease().packages());

    // do not parse the resource profile, attempt to test directly against a package
    if (Objects.nonNull(implementationGuideToUse)) {
      packagesToLoad.add(implementationGuideToUse);
      return packagesToLoad;
    }

    if (Objects.nonNull(cliConfig.module())) {
      packagesToLoad.addAll(
          validationPackageSelector.getPackagesForResource(
              resource, cliConfig.context().fhirRelease(), cliConfig.validationOptions()));
    }
    // Translate all the tgz coordinate packages into remote ones (they are already in cache)
    return packagesToLoad.stream()
        .map(
            s ->
                s.contains(LocalArchive.PACKAGE_SEPARATOR)
                        && s.endsWith(LocalArchive.ARCHIVE_PACKAGE_EXTENSION)
                    ? LocalArchive.parse(s).coordinates()
                    : s)
        .toList();
  }

  private static @NonNull List<SuppressionRule> getSuppressionRules(
      @NonNull ValidationOptions base, @NonNull ValidationModuleManifest moduleManifest) {
    List<SuppressionRule> suppressionRules;
    Collection<SuppressionRule> baseRules =
        Objects.requireNonNullElse(base.suppressionRules(), Collections.emptyList());
    if (Objects.nonNull(moduleManifest.globalSuppressionRules())) {
      suppressionRules =
          new ArrayList<>(baseRules.size() + moduleManifest.globalSuppressionRules().size());
      suppressionRules.addAll(baseRules);
      suppressionRules.addAll(moduleManifest.globalSuppressionRules());
    } else {
      suppressionRules = base.suppressionRules();
    }
    log.debug("Total Suppression Rules loaded: {}", suppressionRules.size());
    return suppressionRules;
  }

  private static @NonNull List<MessageTransformation> getTransformations(
      @NonNull ValidationOptions base, @NonNull ValidationModuleManifest moduleManifest) {
    List<MessageTransformation> transformations;
    Collection<MessageTransformation> baseTransformations =
        Objects.requireNonNullElse(base.messageTransformations(), Collections.emptyList());
    if (Objects.nonNull(moduleManifest.messageTransformations())) {
      transformations =
          new ArrayList<>(
              baseTransformations.size() + moduleManifest.messageTransformations().size());
      transformations.addAll(baseTransformations);
      transformations.addAll(
          moduleManifest.messageTransformations().values().stream()
              .flatMap(Collection::stream)
              .toList());
    } else {
      transformations = base.messageTransformations();
    }
    log.debug("Total Transformation Messages loaded: {}", transformations.size());
    return transformations;
  }
}
