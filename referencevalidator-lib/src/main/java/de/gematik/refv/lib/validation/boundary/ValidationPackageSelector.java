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
import de.gematik.refv.valmodule.api.entity.MessageTransformation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

  /** The selected package group and only the message transformations declared by that group. */
  public record PackageSelection(
      @NonNull Optional<String> packageGroup,
      @NonNull List<String> packages,
      @NonNull List<String> messageTransformationIds,
      @NonNull List<MessageTransformation> messageTransformations) {
    public PackageSelection {
      Objects.requireNonNull(packageGroup, "packageGroup");
      packages = List.copyOf(packages);
      messageTransformationIds = List.copyOf(messageTransformationIds);
      messageTransformations = List.copyOf(messageTransformations);
    }
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
    return selectForResource(fhirResource, fhirRelease, validationOptions).packages();
  }

  /**
   * Resolves packages and group policies together from the resource's profile and validity date.
   */
  public @NonNull PackageSelection selectForResource(
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
    final var profileUrl = profileValidity.profileCanonical().canonical().toString();
    final var profileVersion = profileValidity.profileCanonical().version();
    final var validityDate = profileValidity.validityDate();
    final var selectedGroup =
        validityDate
            .map(
                date ->
                    validationModuleIndex.getPackageGroupForDate(profileUrl, profileVersion, date))
            .orElseGet(
                () -> validationModuleIndex.getFallbackPackageGroup(profileUrl, profileVersion));
    final List<String> profilePackages =
        selectedGroup.map(ValidationModuleIndex.PackageGroupSelection::packages).orElse(List.of());

    validityDate.ifPresent(
        date -> {
          if (ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE.equals(
                  validationOptions.profileValidityPeriodCheckStrategy())
              && profilePackages.isEmpty()) {
            if (log.isErrorEnabled()) {
              log.error(
                  "The profile {} is outside the validity period for date {}",
                  profileValidity.profileCanonical().value(),
                  date);
            }
            throw new ProfileOutsideValidityPeriodException("Profile is outside of valid period");
          }

          log.debug("Found valid dependencies {}", profilePackages);
        });

    log.debug("Profile Packages detected: {}", profilePackages);
    return new PackageSelection(
        selectedGroup.map(ValidationModuleIndex.PackageGroupSelection::packageGroup),
        profilePackages,
        selectedGroup
            .map(ValidationModuleIndex.PackageGroupSelection::messageTransformationIds)
            .orElse(List.of()),
        selectedGroup
            .map(ValidationModuleIndex.PackageGroupSelection::messageTransformations)
            .orElse(List.of()));
  }
}
