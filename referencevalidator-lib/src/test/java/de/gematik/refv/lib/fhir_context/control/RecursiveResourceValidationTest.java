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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.refv.lib.exceptions.ValidationException;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration.RecursiveModePolicy;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class RecursiveResourceValidationTest {
  private static final String PROFILE =
      "http://example.org/StructureDefinition/ActivePatient|1.0.0";
  private static final String GENDER_PROFILE =
      "http://example.org/StructureDefinition/PatientGender";
  private static final String CHILD_PATH =
      "Bundle.entry[0].resource.parameter[0].part[0].part[0].resource.entry[0].resource";

  @TempDir Path temporaryDirectory;

  @ParameterizedTest
  @MethodSource("formatsAndReleases")
  void nestedChildrenUseTheirOwnProfilesOnlyWhenRecursionIsAllowed(FhirRelease release, boolean xml)
      throws Exception {
    var resource = nestedResource(xml);
    var packageToLoad = profilePackage(release);
    // A root profile must not be passed on to children of different resource types.
    var rootProfiles =
        List.of(ProfileCanonical.fromCanonical("http://hl7.org/fhir/StructureDefinition/Bundle"));
    try (var context = context(release, RecursiveModePolicy.DISALLOWED, packageToLoad)) {
      var parentMessages = context.validate(resource, rootProfiles).messages();
      assertFalse(
          parentMessages.stream().anyMatch(RecursiveResourceValidationTest::isChildMessage));
    }
    try (var context = context(release, RecursiveModePolicy.ALLOWED, packageToLoad)) {
      var messages = context.validate(resource, rootProfiles).messages();
      var errors = messages.stream().filter(RecursiveResourceValidationTest::isChildError).toList();
      // Both profiles are violated: active must be true, and gender is required.
      assertEquals(
          2L,
          errors.stream()
              .filter(message -> message.messageContent().startsWith(CHILD_PATH + ":"))
              .count(),
          messages::toString);
      assertTrue(
          messages.stream()
              .anyMatch(
                  message -> message.messageContent().startsWith("Bundle.entry[1].resource:")),
          messages::toString);
    }
  }

  @Test
  void aggregatesParentErrorsAndCoreValidationOfProfilelessChildren() throws Exception {
    var resource =
        FhirResource.fromJson(
            """
        {"resourceType":"Bundle","entry":[{"resource":{"resourceType":"Patient","gender":"invalid"}}]}
        """);
    try (var context =
        context(
            FhirRelease.asR4(), RecursiveModePolicy.ALLOWED, profilePackage(FhirRelease.asR4()))) {
      var messages = context.validate(resource, List.of()).messages();
      assertTrue(
          messages.stream()
              .anyMatch(
                  message -> message.severity() == IssueSeverity.ERROR && !isChildMessage(message)),
          messages::toString);
      assertTrue(
          messages.stream().anyMatch(RecursiveResourceValidationTest::isChildError),
          messages::toString);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://example.org/missing|9.0",
        "http://example.org/StructureDefinition/ActivePatient|9.0"
      })
  void unavailableChildProfileProducesTheSameFailureAsAnUnavailableRootProfile(String canonical)
      throws Exception {
    var resource =
        FhirResource.fromJson(
            """
        {"resourceType":"Parameters","parameter":[{"name":"patient","resource":{
          "resourceType":"Patient","meta":{"profile":["%s"]}
        }}]}
        """
                .formatted(canonical));
    try (var context =
        context(
            FhirRelease.asR4(), RecursiveModePolicy.ALLOWED, profilePackage(FhirRelease.asR4()))) {
      List<ProfileCanonical> profiles = List.of();
      var exception =
          assertThrows(ValidationException.class, () -> context.validate(resource, profiles));
      assertEquals(
          "The User-Provided FHIR profiles are not available in the Engine Context",
          exception.getMessage());
    }
  }

  static Stream<Arguments> formatsAndReleases() {
    return Stream.of(
        Arguments.of(FhirRelease.asR4(), false),
        Arguments.of(FhirRelease.asR4(), true),
        Arguments.of(FhirRelease.asR5(), false),
        Arguments.of(FhirRelease.asR5(), true));
  }

  private static boolean isChildMessage(ResultMessage message) {
    return message.messageContent().startsWith("Bundle.entry[");
  }

  private static boolean isChildError(ResultMessage message) {
    return isChildMessage(message) && message.severity() == IssueSeverity.ERROR;
  }

  private DefaultValidationContext context(
      FhirRelease release, RecursiveModePolicy recursion, ResolvedPackage packageToLoad) {
    var defaults = ContextConfiguration.defaultConfiguration();
    var policy = defaults.validationPolicy();
    var configuration =
        new ContextConfiguration(
            release,
            "en-US",
            defaults.displayBehavior(),
            new PackageDownloadConfiguration(
                PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED,
                temporaryDirectory.resolve("cache")),
            defaults.terminology(),
            new ValidationPolicyConfiguration(
                policy.validationLevelPolicy(),
                policy.unknownCodeSystemsPolicy(),
                policy.exampleCodeSystemsUsagePolicy(),
                policy.exampleUrlsUsagePolicy(),
                policy.exampleRestReferencesPolicy(),
                recursion,
                policy.anyExtensionPolicy()));
    return new DefaultValidationContext(configuration, List.of(packageToLoad));
  }

  private ResolvedPackage profilePackage(FhirRelease release) throws Exception {
    var root = temporaryDirectory.resolve("recursive-test#1.0.0");
    var directory = Files.createDirectories(root.resolve("package"));
    Files.writeString(
        directory.resolve("package.json"),
        """
        {"name":"recursive-test","version":"1.0.0","fhirVersions":["%s"]}
        """
            .formatted(release.corePackageVersion()));
    writeProfile(directory, release, "ActivePatient", "active", "\"fixedBoolean\":true");
    writeProfile(directory, release, "PatientGender", "gender", "\"min\":1");
    return new ResolvedPackage(PackageId.parse("recursive-test#1.0.0"), root, List.of());
  }

  private static void writeProfile(
      Path directory, FhirRelease release, String name, String field, String constraint)
      throws Exception {
    Files.writeString(
        directory.resolve("StructureDefinition-" + name + ".json"),
        """
        {"resourceType":"StructureDefinition","url":"http://example.org/StructureDefinition/%s",
         "version":"1.0.0","name":"%s","status":"active","fhirVersion":"%s",
         "kind":"resource","abstract":false,"type":"Patient",
         "baseDefinition":"http://hl7.org/fhir/StructureDefinition/Patient","derivation":"constraint",
         "differential":{"element":[{"id":"Patient","path":"Patient"},
           {"id":"Patient.%s","path":"Patient.%s",%s}]}}
        """
            .formatted(name, name, release.corePackageVersion(), field, field, constraint));
  }

  private static FhirResource nestedResource(boolean xml) {
    if (xml) {
      return FhirResource.fromXml(
          """
          <Bundle xmlns="http://hl7.org/fhir"><type value="collection"/>
            <entry><resource><Parameters><parameter><name value="group"/>
              <part><name value="nested"/><part><name value="bundle"/><resource>
                <Bundle><type value="collection"/><entry><resource><Patient>
                  <meta><profile value="%s"/><profile value="%s"/></meta><active value="false"/>
                </Patient></resource></entry></Bundle>
              </resource></part></part>
            </parameter></Parameters></resource></entry>
            <entry><resource><Patient><active value="true"/></Patient></resource></entry>
          </Bundle>
          """
              .formatted(PROFILE, GENDER_PROFILE));
    }
    return FhirResource.fromJson(
        """
        {"resourceType":"Bundle","type":"collection","entry":[
          {"resource":{"resourceType":"Parameters","parameter":[{"name":"group","part":[
            {"name":"nested","part":[{"name":"bundle","resource":{
              "resourceType":"Bundle","type":"collection","entry":[{"resource":{
                "resourceType":"Patient","meta":{"profile":["%s","%s"]},"active":false
              }}]
            }}]}
          ]}]}},
          {"resource":{"resourceType":"Patient","active":true}}
        ]}
        """
            .formatted(PROFILE, GENDER_PROFILE));
  }
}
