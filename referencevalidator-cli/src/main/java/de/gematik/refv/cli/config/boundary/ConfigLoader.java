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
package de.gematik.refv.cli.config.boundary;

import de.gematik.refv.cli.config.entity.SnapshotCliConfig;
import de.gematik.refv.cli.config.entity.SnapshotModuleConfiguration;
import de.gematik.refv.cli.config.entity.ValidationCliConfig;
import de.gematik.refv.cli.config.entity.ValidationModuleConfiguration;
import de.gematik.refv.cli.report.entity.ReportConfiguration;
import de.gematik.refv.lib.config_parser.boundary.YAMLMapperProvider;
import de.gematik.refv.lib.exceptions.ConfigurationException;
import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.TerminologyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Helper to load the Configuration for the FHIR Engine. */
public final class ConfigLoader {
  /**
   * Attempts to load the configuration of the snapshot generator from a valid file path.
   *
   * @param configPath a valid file path pointing to a configuration in YAML format
   * @return an instance of {@link SnapshotCliConfig}, otherwise throws an exception in case of
   *     errors
   * @throws ConfigurationException in case of I/O Errors
   */
  public SnapshotCliConfig loadSnapshotConfigFromPath(final Path configPath)
      throws ConfigurationException {
    if (!Files.exists(configPath) || !Files.isRegularFile(configPath)) {
      throw new InitializationException("Config file not found: " + configPath);
    }
    try (InputStream inputStream = Files.newInputStream(configPath)) {
      return YAMLMapperProvider.getMapper().readValue(inputStream, SnapshotCliConfig.class);
    } catch (IOException e) {
      throw new ConfigurationException("Could not load or parse config file: " + configPath, e);
    }
  }

  /**
   * Attempts to load the configuration of the validator from a valid file path.
   *
   * @param configPath a valid file path pointing to a configuration in YAML format
   * @return an instance of {@link ValidationCliConfig}, otherwise throws an exception in case of
   *     errors
   * @throws ConfigurationException in case of I/O Errors
   */
  public ValidationCliConfig loadValidationConfigFromPath(final Path configPath)
      throws ConfigurationException {
    if (!Files.exists(configPath) || !Files.isRegularFile(configPath)) {
      throw new InitializationException("Config file not found: " + configPath);
    }
    try (InputStream inputStream = Files.newInputStream(configPath)) {
      return YAMLMapperProvider.getMapper().readValue(inputStream, ValidationCliConfig.class);
    } catch (IOException e) {
      throw new ConfigurationException("Could not load or parse config file: " + configPath, e);
    }
  }

  /**
   * Attempts to load the configuration of the validator from a set of command line parameters
   *
   * @return an instance of {@link SnapshotCliConfig}, otherwise throws an exception in case of
   *     errors
   */
  public SnapshotCliConfig fromSnapshotGeneratorArguments(
      @NonNull SnapshotGeneratorArguments arguments) {
    final var contextArguments = arguments.context();
    final ContextConfiguration contextConfiguration = buildContextConfig(contextArguments);
    final Path moduleConfigPath = arguments.moduleConfigPath();
    final SnapshotModuleConfiguration moduleConfiguration;
    if (Objects.nonNull(moduleConfigPath) && Files.exists(moduleConfigPath)) {
      moduleConfiguration = buildSnapshotModuleConfiguration(moduleConfigPath);
    } else {
      moduleConfiguration = null;
    }
    final var reportConfig = buildReportConfiguration(arguments.report());
    return new SnapshotCliConfig(contextConfiguration, moduleConfiguration, reportConfig);
  }

  @SuppressWarnings("java:S107")
  public SnapshotCliConfig fromSnapshotGeneratorArguments(
      @Nullable Path moduleConfigPath,
      @NonNull String fhirVersion,
      @NonNull String locale,
      @Nullable Path cacheDir,
      boolean shouldUseTerminologyServer,
      @NonNull String terminologyServerUrl,
      boolean shouldUseOfflineMode,
      @NonNull Path reportPath,
      boolean shouldUseJson) {
    return fromSnapshotGeneratorArguments(
        new SnapshotGeneratorArguments(
            moduleConfigPath,
            new ContextArguments(
                fhirVersion,
                locale,
                cacheDir,
                new TerminologyArguments(shouldUseTerminologyServer, terminologyServerUrl),
                true,
                true,
                shouldUseOfflineMode),
            new ReportArguments(reportPath, shouldUseJson)));
  }

  public ValidationCliConfig fromValidationArguments(@NonNull ValidationArguments arguments) {
    final var contextArguments = arguments.context();
    final var contextConfiguration = buildContextConfig(contextArguments);

    final Path moduleDirectory = arguments.module().directory();
    final String moduleName = arguments.module().name();
    final ValidationModuleConfiguration moduleConfiguration;
    if (Objects.nonNull(moduleDirectory)
        && !moduleDirectory.toString().isBlank()
        && Objects.nonNull(moduleName)
        && !moduleName.isBlank()) {
      moduleConfiguration = buildModuleConfiguration(moduleDirectory, moduleName);
    } else {
      moduleConfiguration = null;
    }

    final var validationOptions =
        buildValidationOptions(
            arguments.profile(),
            contextArguments.showAllValidationMessages(),
            arguments.skipValidityPeriodChecks());
    final var reportConfig = buildReportConfiguration(arguments.report());

    return new ValidationCliConfig(
        contextConfiguration, moduleConfiguration, validationOptions, reportConfig);
  }

  @SuppressWarnings("java:S107")
  public ValidationCliConfig fromValidationArguments(
      @Nullable Path moduleDirectory,
      @Nullable String moduleName,
      @NonNull String fhirVersion,
      @NonNull String locale,
      boolean shouldUseTerminologyServer,
      @Nullable String terminologyServerUrl,
      @Nullable String profile,
      boolean shouldAllowExampleUrls,
      boolean shouldShowAllValidationMessages,
      boolean shouldRunInOnlineMode,
      boolean shouldSkipValidityPeriodChecks,
      @NonNull Path reportPath,
      boolean shouldUseJson) {
    return fromValidationArguments(
        new ValidationArguments(
            new ModuleArguments(moduleDirectory, moduleName),
            new ContextArguments(
                fhirVersion,
                locale,
                null,
                new TerminologyArguments(shouldUseTerminologyServer, terminologyServerUrl),
                shouldShowAllValidationMessages,
                shouldAllowExampleUrls,
                shouldRunInOnlineMode),
            profile,
            shouldSkipValidityPeriodChecks,
            new ReportArguments(reportPath, shouldUseJson)));
  }

  private @NonNull ContextConfiguration buildContextConfig(ContextArguments arguments) {

    final var displayBehavior =
        arguments.showAllValidationMessages()
            ? DisplayBehaviorConfiguration.displayAll()
            : DisplayBehaviorConfiguration.displayNone();
    Path cacheDir = arguments.cacheDir();
    if (Objects.isNull(cacheDir)) {
      cacheDir = Path.of(System.getProperty("user.home"), ".fhir", "packages");
    }
    final var packageLoadingConfig =
        new PackageDownloadConfiguration(
            arguments.onlineMode()
                ? PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED
                : PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED,
            cacheDir);
    final var terminologyConfig =
        arguments.terminology().useServer() || Objects.isNull(arguments.terminology().serverUrl())
            ? TerminologyConfiguration.offlineMode()
            : new TerminologyConfiguration(
                TerminologyConfiguration.RemoteLoadingPolicy.ALLOWED,
                URI.create(arguments.terminology().serverUrl()));
    final var validationPolicy =
        new ValidationPolicyConfiguration(
            ValidationPolicyConfiguration.ValidationLevelPolicy.SHOW_WARNINGS_AND_ERRORS,
            ValidationPolicyConfiguration.UnknownCodeSystemsPolicy.DISALLOWED,
            ValidationPolicyConfiguration.ExampleCodeSystemsUsagePolicy.DISALLOWED,
            arguments.allowExampleUrls()
                ? ValidationPolicyConfiguration.ExampleUrlsUsagePolicy.ALLOWED
                : ValidationPolicyConfiguration.ExampleUrlsUsagePolicy.DISALLOWED,
            arguments.allowExampleUrls()
                ? ValidationPolicyConfiguration.ExampleRestReferencesPolicy.ALLOWED
                : ValidationPolicyConfiguration.ExampleRestReferencesPolicy.DISALLOWED,
            ValidationPolicyConfiguration.RecursiveModePolicy.DISALLOWED,
            ValidationPolicyConfiguration.AnyExtensionPolicy.ALLOWED);

    return new ContextConfiguration(
        FhirRelease.ofVersion(arguments.fhirVersion()),
        arguments.locale(),
        displayBehavior,
        packageLoadingConfig,
        terminologyConfig,
        validationPolicy);
  }

  private @NonNull ReportConfiguration buildReportConfiguration(ReportArguments arguments) {
    return new ReportConfiguration(
        Objects.requireNonNull(arguments.path(), "The Report File Path must not be null"),
        arguments.useJson() ? ReportConfiguration.Format.JSON : ReportConfiguration.Format.HTML);
  }

  private @NonNull ValidationModuleConfiguration buildModuleConfiguration(
      @NonNull Path moduleDirectory, @NonNull String moduleName) {
    return new ValidationModuleConfiguration(moduleDirectory, moduleName);
  }

  private @NonNull SnapshotModuleConfiguration buildSnapshotModuleConfiguration(
      @NonNull Path modulePath) {
    final var moduleParentDir = modulePath.getParent().getParent();
    if (Files.notExists(moduleParentDir)) {
      throw new InitializationException(
          "Failed to retrieve the directory where the module is defined");
    }
    return new SnapshotModuleConfiguration(
        modulePath,
        moduleParentDir.resolve("src-package"),
        moduleParentDir.resolve("src-package", "patches"));
  }

  private @NonNull ValidationOptions buildValidationOptions(
      String profile, boolean showAllValidationMessages, boolean shouldSkipValidityPeriodChecks) {
    return new ValidationOptions(
        Objects.isNull(profile) || profile.isBlank()
            ? null
            : ProfileCanonical.fromCanonical(profile),
        null,
        showAllValidationMessages
            ? ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL
            : ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ERRORS_ONLY,
        shouldSkipValidityPeriodChecks
            ? ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE
            : ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE);
  }

  public record SnapshotGeneratorArguments(
      @Nullable Path moduleConfigPath,
      @NonNull ContextArguments context,
      @NonNull ReportArguments report) {}

  public record ValidationArguments(
      @NonNull ModuleArguments module,
      @NonNull ContextArguments context,
      @Nullable String profile,
      boolean skipValidityPeriodChecks,
      @NonNull ReportArguments report) {}

  public record ModuleArguments(@Nullable Path directory, @Nullable String name) {}

  public record ContextArguments(
      @NonNull String fhirVersion,
      @NonNull String locale,
      @Nullable Path cacheDir,
      @NonNull TerminologyArguments terminology,
      boolean showAllValidationMessages,
      boolean allowExampleUrls,
      boolean onlineMode) {}

  public record TerminologyArguments(boolean useServer, @Nullable String serverUrl) {}

  public record ReportArguments(@Nullable Path path, boolean useJson) {}
}
