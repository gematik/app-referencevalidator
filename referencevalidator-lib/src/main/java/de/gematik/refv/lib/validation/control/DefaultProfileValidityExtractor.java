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
package de.gematik.refv.lib.validation.control;

import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.exceptions.InvalidDateFormatException;
import de.gematik.refv.lib.exceptions.MissingProfileDefinitionException;
import de.gematik.refv.lib.exceptions.ParsingException;
import de.gematik.refv.lib.exceptions.ProfileMismatchException;
import de.gematik.refv.lib.exceptions.UnsupportedProfileException;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.ProfileValidity;
import de.gematik.refv.lib.fhir_context.entity.ValidationModuleIndex;
import de.gematik.refv.lib.validation.boundary.ProfileValidityExtractor;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.JsonFhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.model.core.BaseDateTimeType;
import org.hl7.fhir.model.core.Bundle;
import org.hl7.fhir.model.core.Parameters;
import org.hl7.fhir.model.core.Resource;
import org.hl7.fhir.services.fhirpath.FHIRPathEngine;
import org.hl7.fhir.standalone.context.SimpleWorkerContext;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Extracts the relevant Profile Canonical and Validity Date from a FHIR Resource, */
public class DefaultProfileValidityExtractor implements ProfileValidityExtractor {

  private static final Logger log = LoggerFactory.getLogger(DefaultProfileValidityExtractor.class);

  private final FHIRPathEngine fhirPathEngine;

  /**
   * Default constructor. Internally creates a new {@link FHIRPathEngine} instance to be used for
   * search.
   */
  public DefaultProfileValidityExtractor() {
    this.fhirPathEngine = createFhirPathEngine();
  }

  /**
   * Extracts the main Profile Canonical and the validity period of the Profile from the FHIR
   * Resource being analyzed and returns it in form of a {@link ProfileValidity} structure, matched
   * with the ones declared in the validation module.
   *
   * <p>Internally, it parses the FHIR Resource and extracts the main (first) Canonical encountered
   * in the resource. It checks that the Validation Module supports the Profile and returns the
   * associated validity period.
   *
   * @param fhirResource the {@link FhirResource} instance to analyze
   * @param fhirRelease the {@link FhirRelease} associated with the resource
   * @return an instance of {@link ProfileValidity}
   * @throws ParsingException in case of issues during the parsing of the resource
   */
  @Override
  public @NonNull ProfileValidity extractProfileValidity(
      @NonNull FhirResource fhirResource,
      @NonNull FhirRelease fhirRelease,
      @NonNull ValidationOptions validationOptions,
      @NonNull ValidationModuleIndex validationModuleIndex)
      throws ParsingException {
    Objects.requireNonNull(fhirResource, "The FHIR Resource cannot be null");
    Objects.requireNonNull(fhirRelease, "The FHIR Version cannot be null");
    Objects.requireNonNull(validationOptions, "The Validation Options cannot be null");
    Objects.requireNonNull(validationModuleIndex, "The Module Index cannot be null");
    try {
      final Resource resource = parseResource(fhirResource, fhirRelease);
      var profileInResource = extractProfileFromResource(resource);
      if (profileInResource.isEmpty()) {
        throw new MissingProfileDefinitionException(
            "FHIR resources without a referenced profile are currently unsupported. Please provide a profile parameter or a profile in the resource meta.profile element.");
      }

      var profileForValidation =
          getProfileForValidation(
              ProfileCanonical.fromCanonical(profileInResource.get()),
              validationOptions,
              validationModuleIndex);

      // extract and pass Date information
      var profileDateDefinition =
          findDateInResourceByConfiguredLocator(
              resource, profileForValidation, validationModuleIndex);
      return new ProfileValidity(profileForValidation, profileDateDefinition);
    } catch (Exception e) {
      throw new ParsingException("Failed to parse or resolve the FHIR resource profile", e);
    } finally {
      try {
        fhirResource.inputStream().close();
      } catch (IOException _) {
        log.warn("Failed to close the resource stream: {}", fhirResource);
      }
    }
  }

  private @NonNull Optional<String> extractProfileFromResource(@NonNull Resource parsedResource) {

    if (parsedResource.hasMeta() && parsedResource.getMeta().hasProfile()) {
      if (parsedResource.getMeta().getProfile().size() > 1) {
        throw new UnsupportedProfileException("Multiple Profiles detected for resource");
      }
      var profile = parsedResource.getMeta().getProfile().getFirst();
      log.debug("Found profile: {}", profile.getValue());
      return Optional.of(profile.getValue());
    }
    Optional<String> containedProfile = Optional.empty();
    if (parsedResource instanceof Bundle bundle) {
      containedProfile = extractProfileFromBundle(bundle);
    } else if (parsedResource instanceof Parameters parameters) {
      containedProfile = extractProfileFromParameters(parameters);
    }
    if (containedProfile.isPresent()) {
      return containedProfile;
    }

    log.debug("No profiles detected from resource");
    return Optional.empty();
  }

  private Optional<String> extractProfileFromBundle(Bundle bundle) {
    for (var entry : bundle.getEntryList()) {
      if (entry.hasResource()) {
        return extractSingleProfileFromResource(entry.getResource());
      }
    }
    return Optional.empty();
  }

  private Optional<String> extractProfileFromParameters(Parameters parameters) {
    for (var entry : parameters.getParameterList()) {
      if (entry.hasResource()) {
        return extractSingleProfileFromResource(entry.getResource());
      }
    }
    return Optional.empty();
  }

  private Optional<String> extractSingleProfileFromResource(@NonNull Resource resource) {
    if (resource.hasMeta() && resource.getMeta().hasProfile()) {
      final var profile = resource.getMeta().getProfile().getFirst();
      if (profile != null) {
        return Optional.of(profile.getValue());
      }
    }
    return Optional.empty();
  }

  private boolean hasNoMatchingProfile(
      @NonNull ProfileCanonical profileInResource, @NonNull Pattern profileFilterRegex) {
    log.debug(
        "Checking if the referenced profile in the resource matches the profile filter: {}...",
        profileFilterRegex);
    return !profileFilterRegex.matcher(profileInResource.toString()).find();
  }

  private Resource parseResource(
      @NonNull FhirResource fhirResource, @NonNull FhirRelease fhirRelease) throws IOException {
    final Resource resource;
    if (fhirResource instanceof JsonFhirResource jsonFhirResource) {
      resource = parseJsonToResource(jsonFhirResource.inputStream(), fhirRelease.alias());
    } else {
      resource = parseXmlToResource(fhirResource.inputStream(), fhirRelease.alias());
    }
    return resource;
  }

  private ProfileCanonical getProfileForValidation(
      @NonNull ProfileCanonical profileInResource,
      @NonNull ValidationOptions validationOptions,
      @NonNull ValidationModuleIndex validationModuleIndex) {

    // Verify first that the Profile Filter Regular Expression matches against any of the declared
    // resources
    if (validationOptions.profileFilterRegex() != null
        && hasNoMatchingProfile(profileInResource, validationOptions.profileFilterRegex())) {
      throw new ProfileMismatchException(
          String.format(
              "The referenced profiles in the resource does not match the profile filter: %s. Referenced profile: %s",
              validationOptions.profileFilterRegex(), profileInResource));
    }

    if (Objects.nonNull(validationOptions.profileToValidate())) {
      // Only one user defined profile is supported at the moment
      var userDefinedProfile = validationOptions.profileToValidate();
      log.warn("Profile for validation has been passed by user: {}", userDefinedProfile);
      // If a Validation Module is being used here, check that the validation module supports it
      if (!validationModuleIndex.isProfileDefined(userDefinedProfile.canonical().toString())) {
        throw new UnsupportedProfileException(
            "The profile specified in the resource is not supported");
      }
      return userDefinedProfile;
    }

    if (!validationModuleIndex.isProfileDefined(profileInResource.canonical().toString())) {
      throw new UnsupportedProfileException(
          "The profile specified in the resource is not supported");
    }

    return profileInResource;
  }

  private Optional<LocalDate> findDateInResourceByConfiguredLocator(
      @NonNull Resource resource,
      @NonNull ProfileCanonical fhirProfile,
      @NonNull ValidationModuleIndex validationModuleIndex) {
    var dateSource =
        validationModuleIndex.getValidityDateSource(
            fhirProfile.canonical().toString(), fhirProfile.version());
    if (dateSource.isPresent()) {
      log.debug("Validity date source detected: {}", dateSource.get());
      return findCreationDate(resource, dateSource.get());
    }

    return Optional.empty();
  }

  /**
   * Scans a {@link Resource} for a given FHIR Expression to extract a LocalDate field.
   *
   * <p>If no TimeZone is detected, the Europe/Berlin one will be implicitly applied
   *
   * @param resource a valid parsed FHIR resource
   * @param expression a FHIR expression to be used for extracting the LocalDate field.
   * @return an Optional object with the found LocalDate, if present
   */
  private Optional<LocalDate> findCreationDate(
      @NonNull Resource resource, @NonNull String expression) {
    if (expression.isBlank()) {
      throw new IllegalArgumentException(
          "Invalid arguments provided for the evaluation of date validity");
    }
    try {
      // Each virtual thread has its own FHIRPathEngine bound via ScopedValue
      // which means, no shared mutable state, no synchronization needed.
      var foundPaths = fhirPathEngine.evaluate(resource, expression);
      if (foundPaths == null || foundPaths.isEmpty()) {
        return Optional.empty();
      }

      if (foundPaths.size() > 1) {
        throw new InvalidDateFormatException(
            String.format(
                "Multiple values for validityDate found using expression: '%s'", foundPaths));
      }

      var first = foundPaths.getFirst();
      if (first instanceof BaseDateTimeType creationDate) {
        if (creationDate.getValue() == null) {
          throw new InvalidDateFormatException(
              "Could not parse validityDate: " + creationDate.asStringValue());
        }

        // Not timezone information supplied --> assume Europe/Berlin
        if (creationDate.getTimeZone() == null) {
          return Optional.of(
              LocalDate.of(
                  creationDate.getYear(), creationDate.getMonth() + 1, creationDate.getDay()));
        }

        // TimeZone information supplied
        return Optional.of(
            LocalDate.ofInstant(creationDate.getValue().toInstant(), ZoneId.of("Europe/Berlin")));
      }

      throw new InvalidDateFormatException("No valid result found using path " + expression);
    } catch (Exception e) {
      log.warn("FHIRPath evaluation failed for expression: {}", expression, e);
      return Optional.empty();
    }
  }

  /**
   * Parses the input stream according to the FHIR Version specified in the {@link
   * ContextConfiguration} configuration.
   *
   * @param inputStream the stream containing the FHIR content to be parsed
   * @param fhirVersion version of FHIR to use for parsing
   * @return an instance of {@link Resource}
   * @throws IOException in case of I/O Errors
   */
  static Resource parseJsonToResource(@NonNull InputStream inputStream, @NonNull String fhirVersion)
      throws IOException {
    return ProfileResourceParser.parseJsonToResource(inputStream, fhirVersion);
  }

  /**
   * Parses the input stream according to the FHIR Version specified in the {@link
   * ContextConfiguration} configuration.
   *
   * @param inputStream the stream containing the FHIR content to be parsed
   * @param fhirVersion version of FHIR to use for parsing
   * @return an instance of {@link Resource}
   * @throws IOException in case of I/O Errors
   */
  static Resource parseXmlToResource(@NonNull InputStream inputStream, @NonNull String fhirVersion)
      throws IOException {
    return ProfileResourceParser.parseXmlToResource(inputStream, fhirVersion);
  }

  /** Creates a fresh {@link FHIRPathEngine} with a blank {@link SimpleWorkerContext}. */
  private FHIRPathEngine createFhirPathEngine() {
    try {
      var context =
          new SimpleWorkerContext.SimpleWorkerContextBuilder(ModelContext.minimalContext())
              .fromNothing();
      return new FHIRPathEngine(context);
    } catch (IOException e) {
      throw new InitializationException("Failed to create FHIRPathEngine", e);
    }
  }
}
