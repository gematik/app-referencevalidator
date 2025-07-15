/*-
 * #%L
 * de.gematik.refv:referencevalidator-lib
 * %%
 * Copyright (C) 2025 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.plugins.configuration;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.Profile;
import de.gematik.refv.commons.configuration.ProfileConfiguration;
import de.gematik.refv.commons.validation.support.CustomNpmPackageValidationSupport;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ValidationModuleConfigurationSupportedProfilesBuilder {

  private final FhirContext fhirContext;
  private final Function<String, InputStream> resourceLoaderFunction;

  public ValidationModuleConfigurationSupportedProfilesBuilder(
      FhirContext fhirContext, Function<String, InputStream> resourceLoaderFunction) {
    this.fhirContext = fhirContext;
    this.resourceLoaderFunction = resourceLoaderFunction;
  }

  /**
   *
   * @param fhirPackage
   * @param creationDateLocators
   * @return Map of supported profiles with their versions (Map: <profileCanonical: String, <version: String, profileConfiguration: ProfileConfiguration>>)
   * @throws IOException
   */
  public Map<String, Map<String, ProfileConfiguration>> buildSupportedProfilesMap(
      @NonNull FhirPackage fhirPackage, List<CreationDateLocator> creationDateLocators)
      throws IOException {
    CustomNpmPackageValidationSupport customNpmPackageValidationSupport =
        CustomNpmPackageValidationSupport.create(
            fhirContext, fhirPackage.toFilename(), resourceLoaderFunction);
    var allProfiles = customNpmPackageValidationSupport.getAllProfiles();
    Map<String, Map<String, ProfileConfiguration>> supportedProfiles = new HashMap<>();
    if (allProfiles != null) {
      for (Profile definition : allProfiles) {

        Optional<String> creationDateLocator = creationDateLocators == null ? Optional.empty() :
            creationDateLocators.stream()
                .filter(locator -> locator.getProfile().equals(definition.getBaseCanonical()))
                .map(CreationDateLocator::getFhirPath)
                .findFirst();
        ProfileConfiguration profileConfiguration =
            new ProfileConfiguration(
                List.of(fhirPackage.toNameAndVersion()),
                creationDateLocator.orElse(null));
        Map<String, ProfileConfiguration> profileVersions = new HashMap<>();
        profileVersions.put(definition.getVersion(), profileConfiguration);

        // Caution! If multiple profiles with the same base canonical are present, the last one will
        // be used for validation of resources with meta.profile without patch number
        addVersionWithoutPatchnumber(
            definition.getVersion(), profileVersions, profileConfiguration);

        profileVersions.put("0.0.0", profileConfiguration);
        supportedProfiles.put(definition.getBaseCanonical(), profileVersions);
      }
    }
    return supportedProfiles;
  }

  /**
   * Adds profile with MAJOR.MINOR version to the map so that the profile can be also referenced
   * without the patch number, e.g. KBV_PR_ERP_Bundle|1.2
   */
  private static void addVersionWithoutPatchnumber(
      String profileVersion,
      Map<String, ProfileConfiguration> profileVersions,
      ProfileConfiguration profileConfiguration) {
    if (profileVersion.matches("\\d+\\.\\d+\\.\\d+")) {
      String[] versionParts = profileVersion.split("\\.");
      String majorMinorVersion = versionParts[0] + "." + versionParts[1];
      profileVersions.put(majorMinorVersion, profileConfiguration);
    }
  }
}
