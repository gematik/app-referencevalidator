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
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.fhir_context.entity.ModuleProfilePolicy;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration.RecursiveModePolicy;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.valmodule.api.entity.PackageGroup;
import de.gematik.refv.valmodule.api.entity.PackageGroupReference;
import de.gematik.refv.valmodule.api.entity.ProfileFamily;
import de.gematik.refv.valmodule.api.entity.ProfileVersion;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class RecursiveResourceValidationTest {
  private static final String PROFILE =
      "http://example.org/StructureDefinition/ActivePatient|1.0.0";
  private static final String SHORT_PROFILE =
      "http://example.org/StructureDefinition/ActivePatient|1.0";
  private static final String GENDER_PROFILE =
      "http://example.org/StructureDefinition/PatientGender";
  private static final String PATIENT_ORGANIZATION_PROFILE =
      "http://example.org/StructureDefinition/PatientOrganizationReference|1.0.0";
  private static final String ORGANIZATION_NAME_PROFILE =
      "http://example.org/StructureDefinition/OrganizationName|1.0.0";
  private static final String ORGANIZATION_URL = "https://example.org/fhir/Organization/org1";
  private static final String PATIENT_FULL_URL = "urn:uuid:7acf2d71-95df-43d0-89df-873492c7d27a";
  private static final String ORGANIZATION_FULL_URL =
      "urn:uuid:3f8f2f47-a15b-4ee7-81dc-f34129bf0c40";
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
            {"resourceType":"Bundle","entry":[{"fullUrl":"%s","resource":{"resourceType":"Patient","gender":"invalid"}}]}
            """
                .formatted(PATIENT_FULL_URL));
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

  @DisplayName(
      "Warns and continues for unsupported root and embedded profiles when the module allows them")
  @ParameterizedTest
  @CsvSource({"false, false", "true, true"})
  void unsupportedProfilesWarnAndContinue(boolean embedded, boolean recursive) throws Exception {
    var canonical = "http://other.example/StructureDefinition/Unknown|9.0.0";
    var resource =
        embedded
            ? FhirResource.fromJson(
                """
                {"resourceType":"Parameters","parameter":[{"name":"patient","resource":{
                  "resourceType":"Patient","meta":{"profile":["%s"]},"active":true
                }}]}
                """
                    .formatted(canonical))
            : FhirResource.fromJson(
                """
                {"resourceType":"Patient","meta":{"profile":["%s"]},"active":true}
                """
                    .formatted(canonical));
    try (var context =
        context(
            FhirRelease.asR4(),
            recursive ? RecursiveModePolicy.ALLOWED : RecursiveModePolicy.DISALLOWED,
            profilePackage(FhirRelease.asR4()),
            moduleProfilePolicy(false))) {
      var messages = context.validate(resource, List.of()).messages();
      assertTrue(
          messages.stream()
              .anyMatch(message ->
                  message.messageId().equals(MessageId.UNSUPPORTED_PROFILE.getCode())
                      && message.severity() == IssueSeverity.WARNING
                      && message.messageContent().contains(canonical)
                      && message.messageContent().contains("no module profile validation")),
          messages::toString);
      assertNoErrors(messages);
    }
  }

  @DisplayName("Rejects unsupported root and embedded profiles when the module requires them")
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void unsupportedProfilesFailWhenModuleRequiresKnownProfiles(boolean embedded) throws Exception {
    var canonical = "http://other.example/StructureDefinition/Unknown|9.0.0";
    var profiles = List.<ProfileCanonical>of();
    var resource =
        embedded
            ? FhirResource.fromJson(
                """
                {"resourceType":"Parameters","parameter":[{"name":"patient","resource":{
                  "resourceType":"Patient","meta":{"profile":["%s"]},"active":true
                }}]}
                """
                    .formatted(canonical))
            : FhirResource.fromJson(
                """
                {"resourceType":"Patient","meta":{"profile":["%s"]},"active":true}
                """
                    .formatted(canonical));
    try (var context =
        context(
            FhirRelease.asR4(),
            RecursiveModePolicy.ALLOWED,
            profilePackage(FhirRelease.asR4()),
            moduleProfilePolicy(true))) {
      assertThrows(ValidationException.class, () -> context.validate(resource, profiles));
    }
  }

  @DisplayName("Still fails when a module-supported embedded profile is missing from the engine")
  @Test
  void supportedButUnavailableChildProfileStillFailsInWarningMode() throws Exception {
    var resource =
        FhirResource.fromJson(
            """
            {"resourceType":"Parameters","parameter":[{"name":"patient","resource":{
              "resourceType":"Patient","meta":{"profile":["%s"]},"active":true
            }}]}
            """
                .formatted("http://example.org/StructureDefinition/Missing|1.0.0"));
    var profiles = List.<ProfileCanonical>of();
    try (var context =
        context(
            FhirRelease.asR4(),
            RecursiveModePolicy.ALLOWED,
            profilePackage(FhirRelease.asR4()),
            moduleProfilePolicy(false))) {
      var exception =
          assertThrows(ValidationException.class, () -> context.validate(resource, profiles));
      assertEquals(
          "The User-Provided FHIR profiles are not available in the Engine Context",
          exception.getMessage());
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Organization/org1",
        "https://example.org/fhir/Organization/org1",
        "urn:uuid:3f8f2f47-a15b-4ee7-81dc-f34129bf0c40"
      })
  void childTargetProfileResolvesSiblingReferenceInOwningBundle(String reference) throws Exception {
    var packageToLoad = profilePackage(FhirRelease.asR4());
    try (var context = context(FhirRelease.asR4(), RecursiveModePolicy.ALLOWED, packageToLoad)) {
      var validMessages =
          context.validate(bundleWithOrganizationReference(reference, true), List.of()).messages();
      assertNoErrors(validMessages);

      var invalidMessages =
          context.validate(bundleWithOrganizationReference(reference, false), List.of()).messages();
      assertTrue(
          invalidMessages.stream().anyMatch(RecursiveResourceValidationTest::isError),
          "Expected the Organization target profile to reject a missing name: " + invalidMessages);

      var wrongTargetNameMessages =
          context
              .validate(bundleWithOrganizationReference(reference, true, "Other"), List.of())
              .messages();
      assertTrue(
          wrongTargetNameMessages.stream().anyMatch(RecursiveResourceValidationTest::isError),
          "Expected the child's resolve() invariant to reject the target name: "
              + wrongTargetNameMessages);
    }
  }

  @Test
  void childTargetProfileResolvesSiblingInsideNestedBundle() throws Exception {
    var nestedBundle =
        FhirResource.fromJson(
            """
            {
              "resourceType":"Bundle","type":"collection","entry":[{
                "fullUrl":"urn:uuid:1a50bd8c-bb6d-4c97-9caf-141f970e00fa",
                "resource":{
                  "resourceType":"Bundle","type":"collection","entry":[
                    {"fullUrl":"%s","resource":{
                      "resourceType":"Patient","id":"patient1",
                      "meta":{"profile":["%s"]},
                      "managingOrganization":{"reference":"urn:uuid:3f8f2f47-a15b-4ee7-81dc-f34129bf0c40"}
                    }},
                    {"fullUrl":"%s","resource":{
                      "resourceType":"Organization","id":"org1","name":"Acme"
                    }}
                  ]
                }
              }]
            }
            """
                .formatted(PATIENT_FULL_URL, PATIENT_ORGANIZATION_PROFILE, ORGANIZATION_FULL_URL));

    try (var context =
        context(
            FhirRelease.asR4(),
            RecursiveModePolicy.ALLOWED,
            profilePackage(FhirRelease.asR4()),
            moduleProfilePolicy(false))) {
      var messages = context.validate(nestedBundle, List.of()).messages();
      assertNoErrors(messages);
    }
  }

  @Test
  void standaloneAndEmbeddedResourcesBothEnforceTheirDeclaredProfile() throws Exception {
    var standalone =
        FhirResource.fromJson(
            """
            {"resourceType":"Patient","id":"patient1",
             "meta":{"profile":["%s"]},"active":false}
            """
                .formatted(PROFILE));
    var contained =
        FhirResource.fromJson(
            """
            {"resourceType":"Bundle","type":"collection","entry":[{
              "fullUrl":"%s","resource":{
                "resourceType":"Patient","id":"patient1",
                "meta":{"profile":["%s"]},"active":false
              }
            }]}
            """
                .formatted(PATIENT_FULL_URL, PROFILE));
    try (var context =
        context(
            FhirRelease.asR4(),
            RecursiveModePolicy.ALLOWED,
            profilePackage(FhirRelease.asR4()),
            moduleProfilePolicy(false))) {
      var standaloneMessages =
          context.validate(standalone, List.of(ProfileCanonical.fromCanonical(PROFILE))).messages();
      var containedMessages = context.validate(contained, List.of()).messages();

      assertTrue(standaloneMessages.stream().anyMatch(RecursiveResourceValidationTest::isError));
      assertTrue(
          containedMessages.stream()
              .anyMatch(message ->
                  isError(message)
                      && message.messageContent().startsWith("Bundle.entry[0].resource:")),
          containedMessages::toString);
    }
  }

  @DisplayName(
      "Resolves a short canonical to the highest loaded patch and validates its unchanged canonical")
  @ParameterizedTest
  @CsvSource({"false, false", "true, true"})
  void shortProfileCanonicalUsesHighestLoadedPatch(boolean embedded, boolean recursive)
      throws Exception {
    var resource =
        embedded
            ? FhirResource.fromJson(
                """
                {"resourceType":"Parameters","parameter":[{"name":"patient","resource":{
                  "resourceType":"Patient","meta":{"profile":["%s"]},"active":true
                }}]}
                """
                    .formatted(SHORT_PROFILE))
            : FhirResource.fromJson(
                """
                {"resourceType":"Patient","meta":{"profile":["%s"]},"active":true}
                """
                    .formatted(SHORT_PROFILE));
    try (var context =
        context(
            FhirRelease.asR4(),
            recursive ? RecursiveModePolicy.ALLOWED : RecursiveModePolicy.DISALLOWED,
            profilePackage(FhirRelease.asR4()),
            moduleProfilePolicy(false))) {
      var messages = context.validate(resource, List.of()).messages();
      assertTrue(
          messages.stream()
              .anyMatch(message ->
                  message.severity() == IssueSeverity.ERROR
                      && message.messageId().equals("_DT_Fixed_Wrong")),
          messages::toString);
      assertFalse(
          messages.stream()
              .anyMatch(
                  message -> message.messageId().equals(MessageId.UNSUPPORTED_PROFILE.getCode())),
          messages::toString);

      var exactProfileResource =
          FhirResource.fromJson(
              """
              {"resourceType":"Patient","meta":{"profile":["%s"]},"active":true}
              """
                  .formatted(PROFILE));
      var exactProfileMessages = context.validate(exactProfileResource, List.of()).messages();
      assertFalse(
          exactProfileMessages.stream()
              .anyMatch(
                  message -> message.messageId().equals(MessageId.UNSUPPORTED_PROFILE.getCode())),
          exactProfileMessages::toString);
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

  private static void assertNoErrors(Collection<ResultMessage> messages) {
    assertFalse(
        messages.stream().anyMatch(RecursiveResourceValidationTest::isError), messages::toString);
  }

  private static boolean isError(ResultMessage message) {
    return message.severity() == IssueSeverity.ERROR || message.severity() == IssueSeverity.FATAL;
  }

  private DefaultValidationContext context(
      FhirRelease release, RecursiveModePolicy recursion, ResolvedPackage packageToLoad) {
    return context(release, recursion, packageToLoad, null);
  }

  private DefaultValidationContext context(
      FhirRelease release,
      RecursiveModePolicy recursion,
      ResolvedPackage packageToLoad,
      ModuleProfilePolicy moduleProfilePolicy) {
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
                policy.anyExtensionPolicy(),
                moduleProfilePolicy));
    return new DefaultValidationContext(configuration, List.of(packageToLoad));
  }

  private ModuleProfilePolicy moduleProfilePolicy(boolean errorOnUnknownProfile) {
    var group = new PackageGroup(List.of("recursive-test#1.0.0"), null);
    var version =
        new ProfileVersion(
            List.of(new PackageGroupReference("recursive-test", LocalDate.of(2020, 1, 1), null)));
    var family =
        new ProfileFamily(
            "http://example.org/StructureDefinition",
            "1.0.0",
            Map.of("1.0.0", version, "1.0.2", version, "1.0.3", version),
            Map.of());
    var manifest =
        new ValidationModuleManifest(
            "3.0",
            "recursive-test",
            "1.0.0",
            "test",
            "test",
            "",
            errorOnUnknownProfile,
            false,
            false,
            List.of(),
            Map.of("recursive-test", group),
            Map.of(),
            Map.of("recursive-test", family),
            List.of(),
            List.of());
    return ModuleProfilePolicy.fromManifest(manifest);
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
    writeProfile(directory, release, "ActivePatient", "1.0.0", "active", "\"fixedBoolean\":true");
    writeProfile(directory, release, "ActivePatient", "1.0.2", "active", "\"fixedBoolean\":true");
    writeProfile(directory, release, "ActivePatient", "1.0.3", "active", "\"fixedBoolean\":false");
    writeProfile(directory, release, "PatientGender", "1.0.0", "gender", "\"min\":1");
    writeOrganizationNameProfile(directory, release);
    writePatientOrganizationReferenceProfile(directory, release);
    return new ResolvedPackage(PackageId.parse("recursive-test#1.0.0"), root, List.of());
  }

  private static void writeOrganizationNameProfile(Path directory, FhirRelease release)
      throws Exception {
    Files.writeString(
        directory.resolve("StructureDefinition-OrganizationName.json"),
        """
        {"resourceType":"StructureDefinition",
         "url":"http://example.org/StructureDefinition/OrganizationName",
         "version":"1.0.0","name":"OrganizationName","status":"active",
         "fhirVersion":"%s","kind":"resource","abstract":false,"type":"Organization",
         "baseDefinition":"http://hl7.org/fhir/StructureDefinition/Organization",
         "derivation":"constraint","differential":{"element":[
           {"id":"Organization","path":"Organization"},
           {"id":"Organization.name","path":"Organization.name","min":1}]}}
        """
            .formatted(release.corePackageVersion()));
  }

  private static void writePatientOrganizationReferenceProfile(Path directory, FhirRelease release)
      throws Exception {
    Files.writeString(
        directory.resolve("StructureDefinition-PatientOrganizationReference.json"),
        """
        {"resourceType":"StructureDefinition",
         "url":"http://example.org/StructureDefinition/PatientOrganizationReference",
         "version":"1.0.0","name":"PatientOrganizationReference","status":"active",
         "fhirVersion":"%s","kind":"resource","abstract":false,"type":"Patient",
         "baseDefinition":"http://hl7.org/fhir/StructureDefinition/Patient",
         "derivation":"constraint","differential":{"element":[
           {"id":"Patient","path":"Patient","constraint":[
             {"key":"org-name","severity":"error","human":"Managing organization must be Acme",
              "expression":"managingOrganization.resolve().name = 'Acme'","xpath":"true()"}]},
           {"id":"Patient.managingOrganization","path":"Patient.managingOrganization",
            "type":[{"code":"Reference","targetProfile":["%s"]}]}]}}
        """
            .formatted(release.corePackageVersion(), ORGANIZATION_NAME_PROFILE));
  }

  private FhirResource bundleWithOrganizationReference(String reference, boolean includeName) {
    return bundleWithOrganizationReference(reference, includeName, "Acme");
  }

  private FhirResource bundleWithOrganizationReference(
      String reference, boolean includeName, String organizationName) {
    var organization =
        includeName
            ? "\"resourceType\":\"Organization\",\"id\":\"org1\",\"name\":\"%s\""
                .formatted(organizationName)
            : "\"resourceType\":\"Organization\",\"id\":\"org1\"";
    var organizationFullUrl = reference.startsWith("urn:") ? reference : ORGANIZATION_URL;
    return FhirResource.fromJson(
        """
        {"resourceType":"Bundle","type":"collection","entry":[
          {"fullUrl":"%s","resource":{
            "resourceType":"Patient","id":"patient1",
            "meta":{"profile":["%s"]},
            "managingOrganization":{"reference":"%s"}
          }},
          {"fullUrl":"%s","resource":{%s}}
        ]}
        """
            .formatted(
                PATIENT_FULL_URL,
                PATIENT_ORGANIZATION_PROFILE,
                reference,
                organizationFullUrl,
                organization));
  }

  private static void writeProfile(
      Path directory,
      FhirRelease release,
      String name,
      String version,
      String field,
      String constraint)
      throws Exception {
    Files.writeString(
        directory.resolve("StructureDefinition-" + name + "-" + version + ".json"),
        """
        {"resourceType":"StructureDefinition","url":"http://example.org/StructureDefinition/%s",
         "version":"%s","name":"%s","status":"active","fhirVersion":"%s",
         "kind":"resource","abstract":false,"type":"Patient",
         "baseDefinition":"http://hl7.org/fhir/StructureDefinition/Patient","derivation":"constraint",
         "differential":{"element":[{"id":"Patient","path":"Patient"},
           {"id":"Patient.%s","path":"Patient.%s",%s}]}}
        """
            .formatted(
                name, version, name, release.corePackageVersion(), field, field, constraint));
  }

  private static FhirResource nestedResource(boolean xml) {
    if (xml) {
      return FhirResource.fromXml(
          """
          <Bundle xmlns="http://hl7.org/fhir"><type value="collection"/>
            <entry><fullUrl value="urn:uuid:1a50bd8c-bb6d-4c97-9caf-141f970e00fa"/><resource><Parameters><parameter><name value="group"/>
              <part><name value="nested"/><part><name value="bundle"/><resource>
                <Bundle><type value="collection"/><entry><fullUrl value="urn:uuid:2b61ce9d-cc7e-4da8-8db4-2520810f11ab"/><resource><Patient>
                  <meta><profile value="%s"/><profile value="%s"/></meta><active value="false"/>
                </Patient></resource></entry></Bundle>
              </resource></part></part>
            </parameter></Parameters></resource></entry>
            <entry><fullUrl value="urn:uuid:3c72dfae-dd8f-4eb9-9ec5-3631921f22bc"/><resource><Patient><active value="true"/></Patient></resource></entry>
          </Bundle>
          """
              .formatted(PROFILE, GENDER_PROFILE));
    }
    return FhirResource.fromJson(
        """
        {"resourceType":"Bundle","type":"collection","entry":[
          {"fullUrl":"urn:uuid:4d83e0bf-ee90-4fca-8fd6-4742a32033cd","resource":{"resourceType":"Parameters","parameter":[{"name":"group","part":[
            {"name":"nested","part":[{"name":"bundle","resource":{
              "resourceType":"Bundle","type":"collection","entry":[{"fullUrl":"urn:uuid:5e94f1c0-ffa1-40db-90e7-5853b43144de","resource":{
                "resourceType":"Patient","meta":{"profile":["%s","%s"]},"active":false
              }}]
            }}]}
          ]}]}},
          {"fullUrl":"urn:uuid:6fa502d1-00b2-41ec-a1f8-6964c54255ef","resource":{"resourceType":"Patient","active":true}}
        ]}
        """
            .formatted(PROFILE, GENDER_PROFILE));
  }
}
