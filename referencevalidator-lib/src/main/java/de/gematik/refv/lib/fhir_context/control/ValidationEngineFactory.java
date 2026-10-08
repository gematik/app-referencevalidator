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
package de.gematik.refv.lib.fhir_context.control;

import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.exceptions.RefValException;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.TerminologyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import org.hl7.fhir.services.validation.constants.BestPracticeWarningLevel;
import org.hl7.fhir.utilities.FhirPublication;
import org.hl7.fhir.utilities.logging.Slf4JLoggingService;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.validation.ValidationEngine;
import org.hl7.fhir.validation.service.model.InstanceValidatorParameters;
import org.hl7.fhir.validation.service.utils.ValidationLevel;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory responsible for creating a new {@link ValidationEngine} with a working WorkerContext,
 * which can be used for performing FHIR Operations (validation, snapshot-generation).
 */
final class ValidationEngineFactory {
  private static final Logger log = LoggerFactory.getLogger(ValidationEngineFactory.class);

  private ValidationEngineFactory() {}

  /**
   * Creates a new {@link ValidationEngine} with core definitions for the given {@link
   * ContextConfiguration} options.
   *
   * @param contextConfiguration the structure holding the configuration options necessary for
   *     creating the Engine
   * @return a configured {@link ValidationEngine} with core profiles
   * @throws RefValException If there is an error with the creation of a new engine.
   */
  @SuppressWarnings("java:S5443")
  static EngineSession createNewCoreEngine(@NonNull ContextConfiguration contextConfiguration)
      throws RefValException {
    Objects.requireNonNull(contextConfiguration, "The context configuration must not be null");
    final var fhirRelease = contextConfiguration.fhirRelease().alias();
    log.info("Creating a new FHIR Engine for Version {}", fhirRelease);
    Path managedTerminologyCachePath = null;
    try {
      Path terminologyCachePath = contextConfiguration.terminology().cachePath();
      if (terminologyCachePath == null) {
        managedTerminologyCachePath = Files.createTempDirectory("refv-tx-");
        terminologyCachePath = managedTerminologyCachePath;
      }
      return new EngineSession(
          buildValidationEngine(contextConfiguration, terminologyCachePath),
          managedTerminologyCachePath);
    } catch (Exception e) {
      if (managedTerminologyCachePath != null) {
        ManagedTemporaryDirectory.owned(managedTerminologyCachePath).close();
      }
      throw new InitializationException("Failed to create a new engine", e);
    }
  }

  /**
   * Creates {@link InstanceValidatorParameters} out of given {@link ContextConfiguration}
   * configuration
   */
  private static InstanceValidatorParameters buildInstanceValidationParameters(
      @NonNull ContextConfiguration contextConfiguration) {
    final var instanceParameters = new InstanceValidatorParameters();
    // Fixed Parameters
    instanceParameters.setAllowDoubleQuotesInFHIRPath(
        false); // Double quotes are not FHIR-conform, fixed to stay as false
    // Configurable parameters
    instanceParameters.setLevel(
        contextConfiguration
                .displayBehavior()
                .displayWarnings()
                .equals(DisplayBehaviorConfiguration.DisplayWarnings.ENABLED)
            ? ValidationLevel.WARNINGS
            : ValidationLevel.HINTS);
    instanceParameters.setBestPracticeLevel(
        toBestPracticeWarningLevel(
            contextConfiguration.displayBehavior().displayBestPracticeMessageLevel()));
    instanceParameters.setAllowExampleUrls(
        contextConfiguration
            .validationPolicy()
            .exampleUrlsUsagePolicy()
            .equals(ValidationPolicyConfiguration.ExampleUrlsUsagePolicy.ALLOWED));
    instanceParameters.setShowMessageIds(
        contextConfiguration
            .displayBehavior()
            .displayMessageIds()
            .equals(DisplayBehaviorConfiguration.DisplayMessageIds.ENABLED));
    instanceParameters.setAssumeValidRestReferences(
        contextConfiguration
            .validationPolicy()
            .exampleRestReferencesPolicy()
            .equals(ValidationPolicyConfiguration.ExampleRestReferencesPolicy.ALLOWED));
    instanceParameters.setHintAboutNonMustSupport(
        contextConfiguration
            .displayBehavior()
            .displayHintAboutNonMustSupport()
            .equals(DisplayBehaviorConfiguration.DisplayHintAboutNonMustSupport.ENABLED));
    instanceParameters.setShowMessagesFromReferences(
        contextConfiguration
            .displayBehavior()
            .displayMessagesFromReferences()
            .equals(DisplayBehaviorConfiguration.DisplayMessagesFromReferences.ENABLED));
    instanceParameters.setUnknownCodeSystemsCauseErrors(
        contextConfiguration
            .validationPolicy()
            .unknownCodeSystemsPolicy()
            .equals(ValidationPolicyConfiguration.UnknownCodeSystemsPolicy.DISALLOWED));
    instanceParameters.setWantInvariantsInMessages(
        contextConfiguration
            .displayBehavior()
            .displayInvariantsInMessage()
            .equals(DisplayBehaviorConfiguration.DisplayInvariantsInMessage.ENABLED));
    return instanceParameters;
  }

  private static @NonNull ValidationEngine buildValidationEngine(
      @NonNull ContextConfiguration contextConfiguration, Path terminologyCachePath)
      throws IOException, URISyntaxException {
    final var instanceValidatorParameters = buildInstanceValidationParameters(contextConfiguration);
    log.debug("Initializing a new ValidationEngine with core definitions");
    final ValidationEngine validationEngine =
        new ValidationEngine.ValidationEngineBuilder()
            // Override the cache location for downloading the terminologies
            .withTerminologyCachePath(terminologyCachePath.toString())
            .withDefaultInstanceValidatorParameters(instanceValidatorParameters)
            .withVersion(contextConfiguration.fhirRelease().corePackageVersion())
            .withTimeTracker(null) // no Time Tracking
            .withUserAgent("gematik/refvalidator")
            .withThoVersion(null)
            .withExtensionsVersion(null)
            .fromSource(
                contextConfiguration.fhirRelease().corePackageName()
                    + "#"
                    + contextConfiguration.fhirRelease().corePackageVersion());

    return configureValidationEngine(validationEngine, contextConfiguration, terminologyCachePath);
  }

  private static @NonNull ValidationEngine configureValidationEngine(
      @NonNull ValidationEngine validationEngine,
      @NonNull ContextConfiguration contextConfiguration,
      Path terminologyCachePath)
      throws IOException, URISyntaxException {
    // Fixed Parameters
    // Fixed engine defaults; only the settings below are exposed as configuration.
    validationEngine.setDebug(true);
    // Perform Schema Validation
    validationEngine.setDoNative(true);
    validationEngine.getContext().setProgress(false);
    validationEngine.getContext().setLogger(new Slf4JLoggingService(log));
    // Configurable Parameters
    validationEngine.setDisplayWarnings(
        contextConfiguration
            .displayBehavior()
            .displayWarnings()
            .equals(DisplayBehaviorConfiguration.DisplayWarnings.ENABLED));
    if (contextConfiguration.locale().contains("-")) {
      final var localeStrings = contextConfiguration.locale().split("-");
      validationEngine.setLocale(Locale.of(localeStrings[0], localeStrings[1]));
    } else {
      validationEngine.setLanguage(contextConfiguration.locale());
    }
    validationEngine.setNoExtensibleBindingMessages(
        contextConfiguration
            .displayBehavior()
            .displayExtensibleBindingsWarnings()
            .equals(DisplayBehaviorConfiguration.DisplayExtensibleBindingsWarnings.DISABLED));
    validationEngine.setAnyExtensionsAllowed(
        contextConfiguration
            .validationPolicy()
            .anyExtensionPolicy()
            .equals(ValidationPolicyConfiguration.AnyExtensionPolicy.ALLOWED));

    // ValidationEngineBuilder.fromNothing() ignores the
    // builder's tx-server flags (withNoTerminologyServer/withCanRunWithoutTerminologyServer),
    // so they must be applied on the engine/context directly.
    configureTerminologyServer(validationEngine, contextConfiguration, terminologyCachePath);
    // There is no way to set up the custom package path with the builder
    // so the setting must be applied on the engine directly.
    configurePackageDownloadSettings(validationEngine, contextConfiguration);
    return validationEngine;
  }

  private static void configurePackageDownloadSettings(
      @NonNull ValidationEngine validationEngine,
      @NonNull ContextConfiguration contextConfiguration) {
    try {
      Files.createDirectories(contextConfiguration.packageLoading().cachePath());
      final var packageManager =
          new FilesystemPackageCacheManager.Builder()
              .withCacheFolder(contextConfiguration.packageLoading().cachePath().toString())
              .build();
      validationEngine.setPcm(packageManager);
    } catch (Exception e) {
      throw new InitializationException(
          "Failed to configure the package download settings in the FHIR Context", e);
    }
  }

  private static void configureTerminologyServer(
      @NonNull ValidationEngine validationEngine,
      @NonNull ContextConfiguration contextConfiguration,
      Path terminologyCachePath)
      throws IOException, URISyntaxException {
    final var terminologyConfiguration = contextConfiguration.terminology();
    validationEngine.getContext().initTxCache(terminologyCachePath.toString());
    validationEngine.getContext().setCachingAllowed(true);

    // Never abort the whole validation because of terminology-server problems
    validationEngine.getContext().setCanRunWithoutTerminology(true);
    if (terminologyConfiguration
            .remoteLoadingPolicy()
            .equals(TerminologyConfiguration.RemoteLoadingPolicy.ALLOWED)
        && Objects.nonNull(terminologyConfiguration.serverUri())) {
      log.debug("Connecting to the terminology server {}", terminologyConfiguration.serverUri());
      validationEngine.getContext().setCanRunWithoutTerminology(false);
      validationEngine.setTerminologyServer(
          terminologyConfiguration.serverUri().toString(),
          null,
          FhirPublication.fromCode(contextConfiguration.fhirRelease().corePackageVersion()),
          true);
    } else {
      // Set Offline mode
      validationEngine.getContext().setNoTerminologyServer(true);
    }
  }

  record EngineSession(ValidationEngine engine, Path managedTerminologyCachePath) {}

  /** Converts the BestPracticeWarningLevel structure */
  private static BestPracticeWarningLevel toBestPracticeWarningLevel(
      DisplayBehaviorConfiguration.@NonNull DisplayBestPracticeMessageLevel
          displayBestPracticeMessageLevel) {

    return switch (displayBestPracticeMessageLevel) {
      case IGNORE -> BestPracticeWarningLevel.Ignore;
      case SHOW_HINTS -> BestPracticeWarningLevel.Hint;
      case SHOW_WARNINGS -> BestPracticeWarningLevel.Warning;
      case SHOW_ERRORS -> BestPracticeWarningLevel.Error;
    };
  }
}
