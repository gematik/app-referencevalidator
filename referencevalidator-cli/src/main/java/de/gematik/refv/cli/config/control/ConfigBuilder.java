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
package de.gematik.refv.cli.config.control;

import de.gematik.refv.cli.commands.entity.ContextArguments;
import de.gematik.refv.cli.commands.entity.ReportArguments;
import de.gematik.refv.cli.commands.entity.SnapshotGenerationModuleArguments;
import de.gematik.refv.cli.commands.entity.SnapshotGeneratorArguments;
import de.gematik.refv.cli.commands.entity.ValidationArguments;
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

/** Builds the configuration from the CLI Arguments. */
public final class ConfigBuilder {
  private ConfigBuilder() {}

  /**
   * Attempts to load the configuration of the snapshot generator from a valid file path.
   *
   * @param configPath a valid file path pointing to a configuration in YAML format
   * @return an instance of {@link SnapshotCliConfig}, otherwise throws an exception in case of
   *     errors
   * @throws ConfigurationException in case of I/O Errors
   */
  public static @NonNull SnapshotCliConfig loadSnapshotConfigFromPath(
      @NonNull final Path configPath) throws ConfigurationException {
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
  public static @NonNull ValidationCliConfig loadValidationConfigFromPath(
      @NonNull final Path configPath) throws ConfigurationException {
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
   * Attempts to load the configuration of the snapshot generator from a set of command line
   * parameters
   *
   * @return an instance of {@link SnapshotCliConfig}, otherwise throws an exception in case of
   *     errors
   */
  public static @NonNull SnapshotCliConfig fromSnapshotGeneratorArguments(
      @NonNull SnapshotGeneratorArguments arguments) {
    final var contextConfig = buildContextConfig(arguments.context());
    final var moduleConfig = buildSnapshotModuleConfiguration(arguments.module());
    final var reportConfig = buildReportConfiguration(arguments.report());
    return new SnapshotCliConfig(contextConfig, moduleConfig, reportConfig);
  }

  /**
   * Attempts to load the configuration of the validator from a set of command line parameters
   *
   * @return an instance of {@link ValidationCliConfig}, otherwise throws an exception in case of
   *     errors
   */
  public static @NonNull ValidationCliConfig fromValidationArguments(
      @NonNull ValidationArguments arguments) {
    final var contextArguments = arguments.context();
    final var contextConfiguration = buildContextConfig(arguments.context());

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

  private static @NonNull ContextConfiguration buildContextConfig(ContextArguments arguments) {

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

  private static @NonNull ReportConfiguration buildReportConfiguration(
      @NonNull ReportArguments arguments) {
    return new ReportConfiguration(
        Objects.requireNonNull(arguments.path(), "The Report File Path must not be null"),
        arguments.useJson() ? ReportConfiguration.Format.JSON : ReportConfiguration.Format.HTML);
  }

  private static @NonNull ValidationModuleConfiguration buildModuleConfiguration(
      @NonNull Path moduleDirectory, @NonNull String moduleName) {
    return new ValidationModuleConfiguration(moduleDirectory, moduleName);
  }

  private static @Nullable SnapshotModuleConfiguration buildSnapshotModuleConfiguration(
      @NonNull SnapshotGenerationModuleArguments moduleArguments) {
    final var manifestPath = moduleArguments.manifestPath();
    if (Objects.isNull(manifestPath)) {
      // Snapshot will be generated without module
      return null;
    }
    if (Files.notExists(manifestPath)) {
      throw new InitializationException(
          "Failed to retrieve the directory where the module is defined");
    }
    return new SnapshotModuleConfiguration(
        manifestPath, moduleArguments.sourcePackagesPath(), moduleArguments.patchesPath());
  }

  private static @NonNull ValidationOptions buildValidationOptions(
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
}
