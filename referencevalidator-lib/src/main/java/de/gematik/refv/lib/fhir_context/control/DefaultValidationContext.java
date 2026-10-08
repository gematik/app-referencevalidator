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
import de.gematik.refv.lib.exceptions.ValidationException;
import de.gematik.refv.lib.fhir_context.boundary.ValidationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.JsonFhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.model.core.OperationOutcome;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.model.utilities.formats.FhirFormat;
import org.hl7.fhir.utilities.i18n.I18nConstants;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.validation.ValidationEngine;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for initializing the FHIR Context used for validation. Internally it
 * holds a {@link ValidationEngine} instance, which is the core of the HL7 Validation Library. The
 * ValidationEngine manages internally dependencies and resources, so they can be used directly. The
 * clone of an existing ValidationEngine is not deep, instead it attempts to reusing previously
 * loaded dependencies, so the initialization is sped up.
 */
class DefaultValidationContext implements ValidationContext {

  private static final Logger log = LoggerFactory.getLogger(DefaultValidationContext.class);
  private static final String SOURCE_MSG = "source.msg";
  private final ContextConfiguration contextConfiguration;
  private final ValidationEngine validationEngine;
  private final ManagedTemporaryDirectory terminologyCacheLease;

  public DefaultValidationContext(
      @NonNull ContextConfiguration contextConfiguration,
      @NonNull Collection<ResolvedPackage> packagesToLoad)
      throws InitializationException {
    Objects.requireNonNull(contextConfiguration, "A valid context configuration must be supplied");
    Objects.requireNonNull(packagesToLoad, "A list of packages to load must be supplied");
    this.contextConfiguration = contextConfiguration;
    try {
      final var engineSession = ValidationEngineFactory.createNewCoreEngine(contextConfiguration);
      this.validationEngine = engineSession.engine();
      this.terminologyCacheLease =
          engineSession.managedTerminologyCachePath() == null
              ? null
              : ManagedTemporaryDirectory.owned(engineSession.managedTerminologyCachePath());
      ContextPackageLoader.loadPackagesInContext(this.validationEngine, packagesToLoad);
    } catch (Exception e) {
      close();
      throw new InitializationException("Failed to initialize validation context", e);
    }
  }

  @Override
  public @NonNull FhirRelease fhirVersion() {
    return contextConfiguration.fhirRelease();
  }

  /**
   * Performs the validation of a resource within the context, against a collection of profiles
   * provided by user.
   *
   * <p>The call to this method should be done only after loading the NPM Packages with additional
   * information, if the provided profiles are different from FHIR basic ones.
   *
   * @param fhirResource the FHIR resource to be validated
   * @param profiles a list of {@link ProfileCanonical} to validate against. If empty, it falls back
   *     to validate core HL7 FHIR structures
   * @return a {@link ResultMessage} containing the information about the validation status of the
   *     resource
   * @throws ValidationException in case of internal failures
   */
  @Override
  public @NonNull ValidationResult validate(
      @NonNull FhirResource fhirResource, @NonNull Collection<ProfileCanonical> profiles)
      throws ValidationException {
    try {
      // Check that profiles are loaded in context
      assertProfilesAreLoadedInContext(profiles);
      final OperationOutcome operationOutcome;
      if (fhirResource instanceof JsonFhirResource jsonFhirResource) {
        operationOutcome =
            this.validationEngine.validate(
                FhirFormat.JSON,
                jsonFhirResource.inputStream(),
                profiles.stream().map(ProfileCanonical::value).toList());
      } else {
        operationOutcome =
            this.validationEngine.validate(
                FhirFormat.XML,
                fhirResource.inputStream(),
                profiles.stream().map(ProfileCanonical::value).toList());
      }
      // BUG HL7: the resources are still searched, even if the configuration is disabled
      final var fixedOutcome =
          suppressExternalAllowedContent(operationOutcome, contextConfiguration);
      return OutcomeConverter.toValidationResult(fixedOutcome);
    } catch (Exception e) {
      throw new ValidationException(e.getLocalizedMessage(), e);
    }
  }

  @Override
  public @NonNull ValidationContext cloneContext(
      @NonNull Collection<ResolvedPackage> resolvedPackages) {
    log.debug("Cloning an existing Context and loading custom dependencies");
    return new DefaultValidationContext(this, resolvedPackages);
  }

  void prepareForConcurrentCloning() {
    validationEngine.prepare();
  }

  @Override
  public void close() {
    if (terminologyCacheLease != null) {
      terminologyCacheLease.close();
    }
  }

  /**
   * Checks that the given profiles are loaded in the context.
   *
   * @param profiles collection of profiles to be validated
   */
  private void assertProfilesAreLoadedInContext(Collection<ProfileCanonical> profiles) {
    boolean areProfilesLoaded =
        // The loaded profile check uses the same canonical/version pair as profile resolution.
        profiles.stream()
            .allMatch(
                profile ->
                    this.validationEngine
                        .getContext()
                        .hasResourceVersion(
                            StructureDefinition.class,
                            profile.canonical().toString(),
                            profile.version()));

    if (!areProfilesLoaded) {
      log.error("FHIR profiles were not loaded: {}", profiles);
      throw new ValidationException(
          "The User-Provided FHIR profiles are not available in the Engine Context");
    }
  }

  /**
   * Initializes internally a new FHIR context with the one of a previously initialized FHIR Context
   * and additionally with the supplied {@link ResolvedPackage} content. Packages are not resolved
   * recursively.
   *
   * @param other the source FHIR Context to use
   * @param packages a list of {@link ResolvedPackage} to be imported
   * @throws InitializationException in case of initialization errors
   */
  private DefaultValidationContext(
      @NonNull DefaultValidationContext other, @NonNull Collection<ResolvedPackage> packages)
      throws InitializationException {
    Objects.requireNonNull(other, "A valid context must be supplied");
    Objects.requireNonNull(packages, "A list of packages must be supplied");
    try {
      this.contextConfiguration = other.contextConfiguration;
      this.validationEngine = new ValidationEngine(other.validationEngine);
      this.terminologyCacheLease =
          other.terminologyCacheLease == null ? null : other.terminologyCacheLease.retain();
      log.trace(
          "Source Snapshot Generation Context: {}",
          System.identityHashCode(other.validationEngine));
      log.trace(
          "New Snapshot Generation Context: {}", System.identityHashCode(this.validationEngine));
      ContextPackageLoader.loadPackagesInContext(this.validationEngine, packages);
    } catch (Exception e) {
      close();
      throw new InitializationException(
          "Failed to clone validation context with additional packages", e);
    }
  }

  private static OperationOutcome suppressExternalAllowedContent(
      OperationOutcome operationOutcome, ContextConfiguration contextConfiguration) {
    List<OperationOutcome.OperationOutcomeIssueComponent> outputComponents = new ArrayList<>();
    for (var issue : operationOutcome.getIssueList()) {
      // skip the issue containing the error
      if (shouldMessageBeIgnored(issue, contextConfiguration)) {
        continue;
      }

      outputComponents.add(issue);
    }

    return new OperationOutcome().setIssueList(outputComponents);
  }

  private static boolean shouldMessageBeIgnored(
      OperationOutcome.OperationOutcomeIssueComponent issue,
      @NonNull ContextConfiguration contextConfiguration) {
    if (!issue.hasUserData(SOURCE_MSG)
        || !(issue.getUserData(SOURCE_MSG) instanceof ValidationMessage validationMessage)
        || Objects.isNull(validationMessage.getMessageId())) {
      return false;
    }

    String messageId = validationMessage.getMessageId();

    if (ValidationPolicyConfiguration.AnyExtensionPolicy.ALLOWED.equals(
            contextConfiguration.validationPolicy().anyExtensionPolicy())
        && I18nConstants.EXTENSION_EXT_UNKNOWN_NOTHERE.contentEquals(messageId)) {
      return true;
    }

    if (ValidationPolicyConfiguration.ExampleRestReferencesPolicy.ALLOWED.equals(
            contextConfiguration.validationPolicy().exampleRestReferencesPolicy())
        && (I18nConstants.REFERENCE_REF_CANTRESOLVE.contentEquals(messageId)
            || I18nConstants.TYPE_SPECIFIC_CHECKS_DT_URL_RESOLVE.contains(messageId)
            || I18nConstants.BUNDLE_ENTRY_URL_MATCHES_TYPE_ID.contains(messageId))) {
      return true;
    }
    log.debug("Got Message Id {} with level: {}", messageId, validationMessage.getLevel());
    return false;
  }
}
