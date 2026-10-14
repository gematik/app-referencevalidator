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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AutomaticChildValidationTest {
  private static final String STRICT_PROFILE =
      "http://example.org/StructureDefinition/recursive-patient|1.0.0";
  private static final String LOOSE_PROFILE =
      "http://example.org/StructureDefinition/recursive-patient-loose|1.0.0";

  @TempDir Path temporaryDirectory;

  @ParameterizedTest
  @ValueSource(strings = {"Bundle", "Parameters"})
  void recursiveModeUsesChildMetaProfilesInsteadOfParentAlternatives(String type) throws Exception {
    var packages = packages();
    var resource = resource(type, LOOSE_PROFILE);
    var profiles = List.of(parentProfile(type));
    try (var context = context(RecursiveModePolicy.DISALLOWED, packages)) {
      var messages = context.validate(resource, profiles).messages();
      assertTrue(
          messages.stream().anyMatch(AutomaticChildValidationTest::isError), messages::toString);
    }
    try (var source = context(RecursiveModePolicy.ALLOWED, packages);
        var clone = source.cloneContext(List.of())) {
      var messages = clone.validate(resource, profiles).messages();
      assertFalse(
          messages.stream().anyMatch(AutomaticChildValidationTest::isError), messages::toString);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Bundle", "Parameters"})
  void recursiveModeStillReportsErrorsFromTheChildsOwnProfile(String type) throws Exception {
    try (var context = context(RecursiveModePolicy.ALLOWED, packages())) {
      var messages =
          context.validate(resource(type, STRICT_PROFILE), List.of(parentProfile(type))).messages();
      var childPath =
          type + (type.equals("Bundle") ? ".entry[0].resource:" : ".parameter[0].resource:");
      var errors = messages.stream().filter(AutomaticChildValidationTest::isError).toList();
      assertFalse(errors.isEmpty(), messages::toString);
      assertTrue(
          errors.stream().allMatch(message -> message.messageContent().startsWith(childPath)),
          messages::toString);
    }
  }

  @Test
  void recursiveModeKeepsParentSliceMatchingAndRequiredSliceChecks() throws Exception {
    var resource = resource("Parameters", LOOSE_PROFILE);
    var profile =
        List.of(
            ProfileCanonical.fromCanonical(
                "http://example.org/StructureDefinition/SlicedParameters|1.0.0"));
    try (var context = context(RecursiveModePolicy.ALLOWED, packages())) {
      var validMessages = context.validate(resource, profile).messages();
      assertFalse(
          validMessages.stream().anyMatch(AutomaticChildValidationTest::isError),
          validMessages::toString);
      var wrongSlice =
          FhirResource.fromJson(
              """
          {"resourceType":"Parameters","parameter":[{"name":"other","resource":{
            "resourceType":"Patient","meta":{"profile":["%s"]},"active":false
          }}]}
          """
                  .formatted(LOOSE_PROFILE));
      var invalidMessages = context.validate(wrongSlice, profile).messages();
      assertTrue(
          invalidMessages.stream()
              .anyMatch(
                  message ->
                      isError(message)
                          && !message
                              .messageContent()
                              .startsWith("Parameters.parameter[0].resource:")),
          invalidMessages::toString);
    }
  }

  private static boolean isError(ResultMessage message) {
    return message.severity() == IssueSeverity.ERROR || message.severity() == IssueSeverity.FATAL;
  }

  private static ProfileCanonical parentProfile(String type) {
    return ProfileCanonical.fromCanonical(
        "http://example.org/StructureDefinition/Restrictive" + type + "|1.0.0");
  }

  private static FhirResource resource(String type, String childProfile) {
    var child =
        """
        {"resourceType":"Patient","id":"patient","meta":{"profile":["%s"]},"active":false}
        """
            .formatted(childProfile);
    return FhirResource.fromJson(
        type.equals("Bundle")
            ? """
          {"resourceType":"Bundle","type":"collection","entry":[{
            "fullUrl":"urn:uuid:7acf2d71-95df-43d0-89df-873492c7d27a","resource":%s}]}
          """
                .formatted(child)
            : """
          {"resourceType":"Parameters","parameter":[{"name":"subject","resource":%s}]}
          """
                .formatted(child));
  }

  private DefaultValidationContext context(
      RecursiveModePolicy recursion, List<ResolvedPackage> packages) {
    var defaults = ContextConfiguration.defaultConfiguration();
    var policy = defaults.validationPolicy();
    var configuration =
        new ContextConfiguration(
            defaults.fhirRelease(),
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
    return new DefaultValidationContext(configuration, packages);
  }

  private List<ResolvedPackage> packages() throws Exception {
    var root = temporaryDirectory.resolve("automatic-child#1.0.0");
    var directory = Files.createDirectories(root.resolve("package"));
    Files.writeString(
        directory.resolve("package.json"),
        """
        {"name":"automatic-child","version":"1.0.0","fhirVersions":["4.0.1"]}
        """);
    writeProfile(
        directory,
        "PatientGender",
        "Patient",
        """
        {"id":"Patient.gender","path":"Patient.gender","min":1}
        """);
    for (var type : List.of("Bundle", "Parameters")) {
      var path = type + (type.equals("Bundle") ? ".entry.resource" : ".parameter.resource");
      writeProfile(
          directory,
          "Restrictive" + type,
          type,
          """
          {"id":"%s","path":"%s","type":[{"code":"Resource","profile":[
            "%s","http://example.org/StructureDefinition/PatientGender|1.0.0"]}]}
          """
              .formatted(path, path, STRICT_PROFILE));
    }
    writeProfile(
        directory,
        "SlicedParameters",
        "Parameters",
        """
        {"id":"Parameters.parameter","path":"Parameters.parameter","slicing":{
          "discriminator":[{"type":"value","path":"name"}],"rules":"closed"}},
        {"id":"Parameters.parameter:subject","path":"Parameters.parameter","sliceName":"subject","min":1,"max":"1"},
        {"id":"Parameters.parameter:subject.name","path":"Parameters.parameter.name","fixedString":"subject"}
        """);
    return List.of(
        new ResolvedPackage(
            PackageId.parse("recursive-profile#1.0.0"),
            Path.of("src/test/resources/packages/recursive-profile#1.0.0"),
            List.of()),
        new ResolvedPackage(PackageId.parse("automatic-child#1.0.0"), root, List.of()));
  }

  private static void writeProfile(Path directory, String name, String type, String constraints)
      throws Exception {
    Files.writeString(
        directory.resolve("StructureDefinition-" + name + ".json"),
        """
        {"resourceType":"StructureDefinition","url":"http://example.org/StructureDefinition/%s",
         "version":"1.0.0","name":"%s","status":"active","fhirVersion":"4.0.1",
         "kind":"resource","abstract":false,"type":"%s",
         "baseDefinition":"http://hl7.org/fhir/StructureDefinition/%s","derivation":"constraint",
         "differential":{"element":[{"id":"%s","path":"%s"},%s]}}
        """
            .formatted(name, name, type, type, type, type, constraints));
  }
}
