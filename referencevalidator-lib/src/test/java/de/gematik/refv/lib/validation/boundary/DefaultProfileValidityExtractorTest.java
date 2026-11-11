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

import de.gematik.refv.lib.exceptions.ParsingException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.ValidationModuleIndex;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.valmodule.entity.ValidationModule;
import de.gematik.refv.valmodule.api.entity.PackageGroup;
import de.gematik.refv.valmodule.api.entity.PackageGroupReference;
import de.gematik.refv.valmodule.api.entity.ProfileDefinition;
import de.gematik.refv.valmodule.api.entity.ProfileFamily;
import de.gematik.refv.valmodule.api.entity.ProfileVersion;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DefaultProfileValidityExtractorTest {

  private static final String BASE = "http://example.org/fhir/StructureDefinition";

  private final ProfileValidityExtractor extractor = ProfileValidityExtractor.defaultExtractor();

  private static ValidationModuleIndex indexSupporting() {
    return indexSupporting(true);
  }

  private static ValidationModuleIndex indexSupporting(boolean errorOnUnknownProfile) {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null)));
    final var families =
        Map.of(
            "fam",
            new ProfileFamily(
                BASE,
                "1.0",
                Map.of("1.0", periods),
                Map.of("MyProfile", new ProfileDefinition(null, null))));
    final var config =
        new ValidationModuleManifest(
            "4.0",
            "m",
            "1.0",
            "a",
            "d",
            "",
            errorOnUnknownProfile,
            false,
            false,
            List.of(),
            groups,
            Map.of(),
            families,
            List.of(),
            List.of());
    return new ValidationModuleIndex(new ValidationModule(config, Path.of("/modules/m.jar")));
  }

  private static FhirResource patientWithProfile(String profileUrl) {
    final String json =
        "{\"resourceType\":\"Patient\",\"meta\":{\"profile\":[\"" + profileUrl + "\"]}}";
    return FhirResource.fromJson(json);
  }

  @DisplayName(
      "Given a resource with a supported profile, when extracting, then the profile validity is returned")
  @Test
  void expectExtractionForSupportedProfile() {
    final var index = indexSupporting();
    final var resource = patientWithProfile(BASE + "/MyProfile|1.0");
    final var validity =
        Assertions.assertDoesNotThrow(() ->
            extractor.extractProfileValidity(
                resource, FhirRelease.asR4(), ValidationOptions.defaultConfiguration(), index));
    Assertions.assertNotNull(validity);
    Assertions.assertEquals(
        BASE + "/MyProfile", validity.profileCanonical().canonical().toString());
  }

  @DisplayName(
      "Given a versionless profile and a module default version, when extracting, then the default profile is resolved")
  @Test
  void resolveVersionlessProfileUsingModuleDefault() {
    final var index = indexSupporting();
    final var resource = patientWithProfile(BASE + "/MyProfile");
    final var validity =
        extractor.extractProfileValidity(
            resource, FhirRelease.asR4(), ValidationOptions.defaultConfiguration(), index);
    Assertions.assertEquals(
        BASE + "/MyProfile", validity.profileCanonical().canonical().toString());
  }

  @DisplayName(
      "Given a resource without a profile, when extracting, then a ParsingException is thrown")
  @Test
  void expectMissingProfileThrows() {
    final var index = indexSupporting();
    final var resource = FhirResource.fromJson("{\"resourceType\":\"Patient\"}");
    final var release = FhirRelease.asR4();
    final var options = ValidationOptions.defaultConfiguration();
    Assertions.assertThrows(
        ParsingException.class,
        () -> extractor.extractProfileValidity(resource, release, options, index));
  }

  @DisplayName(
      "Given an unsupported profile URL or version and warning policy, when extracting, then the profile is retained for core validation")
  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://another-example.org/fhir/StructureDefinition/Unsupported|1.0",
        BASE + "/MyProfile|2.0"
      })
  void continueForUnsupportedProfilesWhenModuleAllowsUnknownProfiles(String canonical) {
    final var index = indexSupporting(false);
    final var resource = patientWithProfile(canonical);
    final var validity =
        extractor.extractProfileValidity(
            resource, FhirRelease.asR4(), ValidationOptions.defaultConfiguration(), index);
    Assertions.assertEquals(ProfileCanonical.fromCanonical(canonical), validity.profileCanonical());
    Assertions.assertTrue(validity.validityDate().isEmpty());
  }

  @DisplayName(
      "Given an unsupported version and strict policy, when extracting, then a profile error is thrown")
  @Test
  void rejectUnsupportedProfileVersionWhenModuleRequiresKnownProfiles() {
    final var index = indexSupporting(true);
    final var resource = patientWithProfile(BASE + "/MyProfile|2.0");
    final var release = FhirRelease.asR4();
    final var options = ValidationOptions.defaultConfiguration();
    final var exception =
        Assertions.assertThrows(
            ParsingException.class,
            () -> extractor.extractProfileValidity(resource, release, options, index));
    Assertions.assertTrue(
        exception.getMessage().contains("The profile specified in the resource is not supported"));
  }

  @DisplayName(
      "Given an unsupported profile URL or version and warning policy, when selecting packages, then no module packages are selected")
  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://another-example.org/fhir/StructureDefinition/Unsupported|1.0",
        BASE + "/MyProfile|2.0"
      })
  void selectNoModulePackagesForUnsupportedProfiles(String canonical) {
    final var selector = new ValidationPackageSelector(indexSupporting(false));
    final var resource = patientWithProfile(canonical);
    final var packages =
        selector.getPackagesForResource(
            resource, FhirRelease.asR4(), ValidationOptions.defaultConfiguration());
    Assertions.assertTrue(packages.isEmpty());
  }

  @DisplayName(
      "Given a resource with an unsupported profile, when extracting, then a ParsingException is thrown")
  @Test
  void expectUnsupportedProfileThrows() {
    final var index = indexSupporting();
    final var resource =
        patientWithProfile("http://another-example.org/fhir/StructureDefinition/Unsupported|1.0");
    final var release = FhirRelease.asR4();
    final var options = ValidationOptions.defaultConfiguration();
    Assertions.assertThrows(
        ParsingException.class,
        () -> extractor.extractProfileValidity(resource, release, options, index));
  }

  @DisplayName("Given an invalid FHIR resource, when extracting, then a ParsingException is thrown")
  @Test
  void expectInvalidResourceThrows() {
    final var index = indexSupporting();
    final var resource = FhirResource.fromJson("{not valid fhir");
    final var release = FhirRelease.asR4();
    final var options = ValidationOptions.defaultConfiguration();
    Assertions.assertThrows(
        ParsingException.class,
        () -> extractor.extractProfileValidity(resource, release, options, index));
  }

  @DisplayName("Given a null resource, when extracting, then a NullPointerException is thrown")
  @Test
  void expectNullResourceThrows() {
    final var index = indexSupporting();
    final var release = FhirRelease.asR4();
    final var options = ValidationOptions.defaultConfiguration();
    Assertions.assertThrows(
        NullPointerException.class,
        () -> extractor.extractProfileValidity(null, release, options, index));
  }
}
