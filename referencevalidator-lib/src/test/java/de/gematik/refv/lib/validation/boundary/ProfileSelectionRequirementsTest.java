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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.refv.lib.exceptions.MissingProfileDefinitionException;
import de.gematik.refv.lib.exceptions.ParsingException;
import de.gematik.refv.lib.exceptions.ProfileMismatchException;
import de.gematik.refv.lib.exceptions.ProfileOutsideValidityPeriodException;
import de.gematik.refv.lib.exceptions.UnsupportedProfileException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.ValidationModuleIndex;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ProfileSelectionRequirementsTest {
  static final String PROFILE = "http://example.org/Patient";
  static final String OVERRIDE = "http://example.org/Override";

  @ParameterizedTest(name = "{0}")
  @MethodSource("profileCases")
  void extractsSupportedProfiles(String requirement) {
    var extractor = ProfileValidityExtractor.defaultExtractor();
    var index = index(null);
    var release = FhirRelease.asR4();
    var options = ValidationOptions.defaultConfiguration();
    var patient = patient("");
    switch (requirement) {
      case "R3.1" -> {
        for (var resource :
            List.of(
                patient,
                FhirResource.fromXml(
                    "<Patient xmlns=\"http://hl7.org/fhir\"><meta><profile value=\""
                        + PROFILE
                        + "|1.0\"/></meta></Patient>"))) {
          assertEquals(
              ProfileCanonical.fromCanonical(PROFILE + "|1.0"),
              extractor
                  .extractProfileValidity(resource, release, options, index)
                  .profileCanonical());
        }
      }
      case "R3.2" ->
          assertEquals(
              ProfileCanonical.fromCanonical(PROFILE + "|1.0"),
              extractor
                  .extractProfileValidity(patient, release, options, index)
                  .profileCanonical());
      case "R3.3" -> {
        for (var type : List.of("Bundle", "Parameters")) {
          var field = type.equals("Bundle") ? "entry" : "parameter";
          var first =
              "{\"resourceType\":\"Patient\",\"meta\":{\"profile\":[\""
                  + PROFILE
                  + "|1.0\",\""
                  + OVERRIDE
                  + "|2.0\"]}}";
          var second = patient.toString();
          var nested =
              FhirResource.fromJson(
                  "{\"resourceType\":\""
                      + type
                      + "\",\""
                      + field
                      + "\":[{\"resource\":"
                      + first
                      + "},{\"resource\":"
                      + second
                      + "}]}");
          assertEquals(
              ProfileCanonical.fromCanonical(PROFILE + "|1.0"),
              extractor.extractProfileValidity(nested, release, options, index).profileCanonical());
          var missingFirst =
              FhirResource.fromJson(
                  "{\"resourceType\":\""
                      + type
                      + "\",\""
                      + field
                      + "\":[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"first\"}},"
                      + "{\"resource\":"
                      + second
                      + "}]}");
          assertInstanceOf(
              MissingProfileDefinitionException.class,
              assertThrows(
                      ParsingException.class,
                      () -> extractor.extractProfileValidity(missingFirst, release, options, index))
                  .getCause());
        }
      }
      case "R3.4" -> {
        var missing = FhirResource.fromJson("{\"resourceType\":\"Patient\"}");
        var override = options(OVERRIDE + "|2.0", null);
        assertInstanceOf(
            MissingProfileDefinitionException.class,
            assertThrows(
                    ParsingException.class,
                    () -> extractor.extractProfileValidity(missing, release, override, index))
                .getCause());
      }
      case "R3.5" -> {
        var multiple =
            FhirResource.fromJson(
                "{\"resourceType\":\"Patient\",\"meta\":{\"profile\":[\""
                    + PROFILE
                    + "\",\""
                    + OVERRIDE
                    + "\"]}}");
        assertInstanceOf(
            UnsupportedProfileException.class,
            assertThrows(
                    ParsingException.class,
                    () -> extractor.extractProfileValidity(multiple, release, options, index))
                .getCause());
      }
      case "R3.6" -> {
        var filtered = options(OVERRIDE + "|2.0", Pattern.compile("does-not-match"));
        assertInstanceOf(
            ProfileMismatchException.class,
            assertThrows(
                    ParsingException.class,
                    () -> extractor.extractProfileValidity(patient, release, filtered, index))
                .getCause());
        verify(index, never()).isProfileDefined(anyString());
      }
      case "R3.7" -> {
        var override = options(OVERRIDE + "|2.0", Pattern.compile("example"));
        assertEquals(
            ProfileCanonical.fromCanonical(OVERRIDE + "|2.0"),
            extractor.extractProfileValidity(patient, release, override, index).profileCanonical());
      }
      case "R3.8" -> {
        when(index.isProfileDefined(PROFILE)).thenReturn(false);
        assertInstanceOf(
            UnsupportedProfileException.class,
            assertThrows(
                    ParsingException.class,
                    () -> extractor.extractProfileValidity(patient, release, options, index))
                .getCause());
        when(index.isProfileDefined(OVERRIDE)).thenReturn(false);
        var override = options(OVERRIDE, null);
        assertInstanceOf(
            UnsupportedProfileException.class,
            assertThrows(
                    ParsingException.class,
                    () -> extractor.extractProfileValidity(patient, release, override, index))
                .getCause());
      }
      case "R3.9" -> {
        var malformed = FhirResource.fromJson("{not JSON");
        assertNotNull(
            assertThrows(
                    ParsingException.class,
                    () -> extractor.extractProfileValidity(malformed, release, options, index))
                .getCause());
      }
      case "R3.10" -> {
        assertThrows(
            NullPointerException.class,
            () -> extractor.extractProfileValidity(null, release, options, index));
        assertThrows(
            NullPointerException.class,
            () -> extractor.extractProfileValidity(patient, null, options, index));
        assertThrows(
            NullPointerException.class,
            () -> extractor.extractProfileValidity(patient, release, null, index));
        assertThrows(
            NullPointerException.class,
            () -> extractor.extractProfileValidity(patient, release, options, null));
      }
      default -> fail("Unexpected requirement " + requirement);
    }
  }

  static Stream<String> profileCases() {
    return Stream.of(
        "R3.1", "R3.2", "R3.3", "R3.4", "R3.5", "R3.6", "R3.7", "R3.8", "R3.9", "R3.10");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("dateCases")
  void extractsValidityDates(String requirement) {
    switch (requirement) {
      case "R4.1" -> {
        var index = index("Patient.birthDate");
        assertEquals(
            Optional.of(LocalDate.of(2025, 1, 2)),
            extract(patient(",\"birthDate\":\"2025-01-02\""), index).validityDate());
        verify(index).getValidityDateSource(PROFILE, "1.0");
      }
      case "R4.2" -> {
        for (var release : List.of(FhirRelease.asR4(), FhirRelease.asR5())) {
          for (var resource : patientsWithBirthDate(null)) {
            assertTrue(extract(resource, index(null), release).validityDate().isEmpty());
            for (var expression :
                List.of(
                    "Patient.birthDate",
                    "???",
                    "(@2025-01-01 | @2025-01-02)",
                    "'not a date'",
                    "true",
                    "1")) {
              var result = extract(resource, index(expression), release);
              assertEquals(
                  ProfileCanonical.fromCanonical(PROFILE + "|1.0"), result.profileCanonical());
              assertTrue(result.validityDate().isEmpty(), expression);
            }
          }
          for (var partialDate : List.of("2025", "2025-07")) {
            for (var resource : patientsWithBirthDate(partialDate)) {
              assertTrue(
                  extract(resource, index("Patient.birthDate"), release).validityDate().isEmpty(),
                  partialDate);
            }
          }
        }
      }
      case "R4.3" ->
          assertEquals(
              Optional.of(LocalDate.of(2025, 1, 2)),
              extract(patient(""), index("@2025-01-01T23:30:00Z")).validityDate());
      case "R4.4" ->
          assertEquals(
              Optional.of(LocalDate.of(2025, 1, 1)),
              extract(patient(""), index("@2025-01-01T23:30:00")).validityDate());
      case "R4.5" -> {
        var index = index(" ");
        for (var release : List.of(FhirRelease.asR4(), FhirRelease.asR5())) {
          for (var resource : patientsWithBirthDate(null)) {
            assertInstanceOf(
                IllegalArgumentException.class,
                assertThrows(ParsingException.class, () -> extract(resource, index, release))
                    .getCause());
          }
        }
      }
      default -> fail("Unexpected requirement " + requirement);
    }
  }

  static Stream<String> dateCases() {
    return Stream.of("R4.1", "R4.2", "R4.3", "R4.4", "R4.5");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("packageCases")
  void selectsModulePackages(String requirement) {
    var index = index("Patient.birthDate");
    var selector = new ValidationPackageSelector(index);
    var resource = patient(",\"birthDate\":\"2025-01-02\"");
    var release = FhirRelease.asR4();
    var options = ValidationOptions.defaultConfiguration();
    var date = LocalDate.of(2025, 1, 2);
    var packages = List.of("example#1.0.0", "dependency#2.0.0");
    switch (requirement) {
      case "R5.1" -> {
        when(index.getPackagesForDate(PROFILE, "1.0", date)).thenReturn(packages);
        assertEquals(packages, selector.getPackagesForResource(resource, release, options));
        verify(index).getPackagesForDate(PROFILE, "1.0", date);
        verify(index, never()).getFallbackPackages(anyString(), anyString());
      }
      case "R5.2" -> {
        when(index.getFallbackPackages(PROFILE, "1.0")).thenReturn(packages);
        for (var expression :
            List.of("Patient.birthDate", "???", "(@2025-01-01 | @2025-01-02)", "'not a date'")) {
          when(index.getValidityDateSource(PROFILE, "1.0")).thenReturn(Optional.of(expression));
          for (var birthDate : List.of("2025", "2025-07")) {
            for (var input : patientsWithBirthDate(birthDate)) {
              assertEquals(packages, selector.getPackagesForResource(input, release, options));
            }
          }
        }
        assertEquals(packages, selector.getPackagesForResource(patient(""), release, options));
        verify(index, atLeastOnce()).getFallbackPackages(PROFILE, "1.0");
        verify(index, never()).getPackagesForDate(anyString(), anyString(), any());
      }
      case "R5.3" ->
          assertThrows(
              ProfileOutsideValidityPeriodException.class,
              () -> selector.getPackagesForResource(resource, release, options));
      case "R5.4" -> {
        var ignored =
            new ValidationOptions(
                null,
                null,
                ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
                ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE);
        assertEquals(List.of(), selector.getPackagesForResource(resource, release, ignored));
      }
      case "R5.5" -> {
        var uninitialized = new ValidationPackageSelector();
        assertThrows(
            NullPointerException.class,
            () -> uninitialized.getPackagesForResource(resource, release, options));
      }
      case "R5.6" -> {
        var missing = FhirResource.fromJson("{\"resourceType\":\"Patient\"}");
        assertInstanceOf(
            MissingProfileDefinitionException.class,
            assertThrows(
                    ParsingException.class,
                    () -> selector.getPackagesForResource(missing, release, options))
                .getCause());
        verify(index, never()).getPackagesForDate(anyString(), anyString(), any());
        verify(index, never()).getFallbackPackages(anyString(), anyString());
      }
      default -> fail("Unexpected requirement " + requirement);
    }
  }

  static Stream<String> packageCases() {
    return Stream.of("R5.1", "R5.2", "R5.3", "R5.4", "R5.5", "R5.6");
  }

  static ValidationModuleIndex index(String locator) {
    var index = mock(ValidationModuleIndex.class);
    when(index.isProfileDefined(PROFILE)).thenReturn(true);
    when(index.isProfileDefined(OVERRIDE)).thenReturn(true);
    when(index.getValidityDateSource(anyString(), anyString()))
        .thenReturn(Optional.ofNullable(locator));
    return index;
  }

  static FhirResource patient(String extraFields) {
    return FhirResource.fromJson(
        "{\"resourceType\":\"Patient\",\"meta\":{\"profile\":[\""
            + PROFILE
            + "|1.0\"]}"
            + extraFields
            + "}");
  }

  static de.gematik.refv.lib.fhir_context.entity.ProfileValidity extract(
      FhirResource resource, ValidationModuleIndex index) {
    return extract(resource, index, FhirRelease.asR4());
  }

  static de.gematik.refv.lib.fhir_context.entity.ProfileValidity extract(
      FhirResource resource, ValidationModuleIndex index, FhirRelease release) {
    return ProfileValidityExtractor.defaultExtractor()
        .extractProfileValidity(resource, release, ValidationOptions.defaultConfiguration(), index);
  }

  static List<FhirResource> patientsWithBirthDate(String birthDate) {
    var jsonField = birthDate == null ? "" : ",\"birthDate\":\"%s\"".formatted(birthDate);
    var xmlField = birthDate == null ? "" : "<birthDate value=\"%s\"/>".formatted(birthDate);
    return List.of(
        patient(jsonField),
        FhirResource.fromXml(
            """
            <Patient xmlns="http://hl7.org/fhir">
              <meta><profile value="%s|1.0"/></meta>
              %s
            </Patient>
            """
                .formatted(PROFILE, xmlField)));
  }

  static ValidationOptions options(String profile, Pattern filter) {
    return new ValidationOptions(
        ProfileCanonical.fromCanonical(profile),
        filter,
        ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
        ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE);
  }
}
