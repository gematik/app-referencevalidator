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
package de.gematik.refv.lib.validation.boundary;

import de.gematik.refv.lib.exceptions.ProfileOutsideValidityPeriodException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.ValidationModuleIndex;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helps select the correct set of dependencies from a Module, based on profile information. */
public class ValidationPackageSelector {
  private static final Logger log = LoggerFactory.getLogger(ValidationPackageSelector.class);
  private final ValidationModuleIndex validationModuleIndex;

  public ValidationPackageSelector(@NonNull ValidationModuleIndex validationModuleIndex) {
    this.validationModuleIndex =
        Objects.requireNonNull(validationModuleIndex, "The Validation Module Index cannot be null");
  }

  public ValidationPackageSelector() {
    this.validationModuleIndex = null;
  }

  /**
   * Retrieves a list of packages as defined in a Validation Module Configuration, based on a given
   * FHIR Resource.
   *
   * @param fhirResource the FHIR Resource to parse
   * @param fhirRelease the FHIR Standard Version to use for parsing the resource
   * @param validationOptions custom validation options for matching the profiles validity
   * @return a list of dependencies declared in the validation module that match the Resource
   *     profile validity
   */
  public @NonNull List<String> getPackagesForResource(
      @NonNull FhirResource fhirResource,
      @NonNull FhirRelease fhirRelease,
      @NonNull ValidationOptions validationOptions) {
    Objects.requireNonNull(
        this.validationModuleIndex, "The Validation Module Index has not been initialized");
    final var profileValidity =
        ProfileValidityExtractor.defaultExtractor()
            .extractProfileValidity(
                fhirResource, fhirRelease, validationOptions, validationModuleIndex);
    log.debug("### Profile Validity detected: {}", profileValidity);
    final List<String> profilePackages;
    if (profileValidity.validityDate().isPresent()) {
      profilePackages =
          validationModuleIndex.getPackagesForDate(
              profileValidity.profileCanonical().canonical().toString(),
              profileValidity.profileCanonical().version(),
              profileValidity.validityDate().get());

      if (ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE.equals(
              validationOptions.profileValidityPeriodCheckStrategy())
          && profilePackages.isEmpty()) {
        if (log.isErrorEnabled()) {
          log.error(
              "The profile {} is outside the validity period for date {}",
              profileValidity.profileCanonical().value(),
              profileValidity.validityDate().get());
        }
        throw new ProfileOutsideValidityPeriodException("Profile is outside of valid period");
      }

      log.debug("Found valid dependencies {}", profilePackages);

    } else {
      profilePackages =
          validationModuleIndex.getFallbackPackages(
              profileValidity.profileCanonical().canonical().toString(),
              profileValidity.profileCanonical().version());
    }

    log.debug("Profile Packages detected: {}", profilePackages);
    return profilePackages;
  }
}
