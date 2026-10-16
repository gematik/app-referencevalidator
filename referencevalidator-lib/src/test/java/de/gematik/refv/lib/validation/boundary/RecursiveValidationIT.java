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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration.RecursiveModePolicy;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationRequest;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecursiveValidationIT {
  private static final String PROFILE =
      "http://example.org/StructureDefinition/recursive-patient|1.0.0";
  private static final String LOOSE_PROFILE =
      "http://example.org/StructureDefinition/recursive-patient-loose|1.0.0";
  private static final String PARAMETERS_PROFILE =
      "http://example.org/StructureDefinition/recursive-parameters|1.0.0";
  private static final String PACKAGE_PATH = "src/test/resources/packages/recursive-profile#1.0.0";

  @TempDir private Path packageCachePath;

  @DisplayName("Validates a Bundle entry's declared profile only when recursive mode is allowed")
  @Test
  void bundleEntryProfileValidationHonorsRecursiveModePolicy() {
    final var json =
        """
        {
          "resourceType": "Bundle",
          "type": "collection",
          "entry": [{
            "fullUrl": "urn:uuid:7acf2d71-95df-43d0-89df-873492c7d27a",
            "resource": {
              "resourceType": "Patient",
              "id": "local-patient",
              "meta": {"profile": ["%s"]},
              "active": true
            }
          }]
        }
        """
            .formatted(PROFILE);
    final var bundle = FhirResource.fromJson(json);
    assertNoErrors(validate(bundle, RecursiveModePolicy.DISALLOWED));
    assertNoErrors(validate(bundle, RecursiveModePolicy.ALLOWED));

    final var invalidBundle =
        FhirResource.fromJson(json.replace("\"active\": true", "\"active\": false"));
    assertRecursiveProfileError(invalidBundle, "Bundle.entry[0].resource:");
  }

  @DisplayName("Validates a Parameters resource parameter's declared profile recursively")
  @Test
  void parametersResourceProfileIsValidatedRecursively() {
    final var xml =
        """
        <Parameters xmlns="http://hl7.org/fhir">
          <meta><profile value="%s"/></meta>
          <parameter>
            <name value="group"/>
            <part>
              <name value="nested"/>
              <part>
                <name value="patient"/>
                <resource>
                  <Patient>
                    <id value="local-patient"/>
                    <meta><profile value="%s"/></meta>
                    <active value="true"/>
                  </Patient>
                </resource>
              </part>
            </part>
          </parameter>
        </Parameters>
        """
            .formatted(PARAMETERS_PROFILE, PROFILE);
    final var parameters = FhirResource.fromXml(xml);
    assertNoErrors(validate(parameters, RecursiveModePolicy.DISALLOWED));
    assertNoErrors(validate(parameters, RecursiveModePolicy.ALLOWED));

    final var invalidParameters =
        FhirResource.fromXml(xml.replace("<active value=\"true\"/>", "<active value=\"false\"/>"));
    assertRecursiveProfileError(
        invalidParameters, "Parameters.parameter[0].part[0].part[0].resource:");
  }

  @DisplayName("Uses an embedded resource's meta.profile instead of its parent's targetProfile")
  @Test
  void embeddedMetaProfileOverridesParentTargetProfile() {
    final var parameters =
        FhirResource.fromJson(
            """
            {
              "resourceType": "Parameters",
              "parameter": [{
                "name": "subject",
                "resource": {
                  "resourceType": "Patient",
                  "meta": {"profile": ["%s"]},
                  "active": false
                }
              }]
            }
            """
                .formatted(LOOSE_PROFILE));

    assertNoErrors(validate(parameters, RecursiveModePolicy.DISALLOWED, PARAMETERS_PROFILE));
    assertNoErrors(validate(parameters, RecursiveModePolicy.ALLOWED, PARAMETERS_PROFILE));
  }

  private void assertRecursiveProfileError(FhirResource resource, String childPath) {
    final var nonRecursiveMessages = validate(resource, RecursiveModePolicy.DISALLOWED);
    assertFalse(
        nonRecursiveMessages.stream()
            .anyMatch(message -> message.messageContent().startsWith(childPath)),
        nonRecursiveMessages::toString);
    final var recursiveMessages = validate(resource, RecursiveModePolicy.ALLOWED);
    assertTrue(
        recursiveMessages.stream()
            .anyMatch(
                message ->
                    message.severity() == IssueSeverity.ERROR
                        && message.messageContent().startsWith(childPath)),
        recursiveMessages::toString);
  }

  private List<ResultMessage> validate(
      FhirResource resource, RecursiveModePolicy recursiveModePolicy) {
    return validate(resource, recursiveModePolicy, null);
  }

  private List<ResultMessage> validate(
      FhirResource resource, RecursiveModePolicy recursiveModePolicy, String profileCanonical) {
    final var defaults = ContextConfiguration.defaultConfiguration();
    final var defaultPolicy = defaults.validationPolicy();
    final var validationPolicy =
        new ValidationPolicyConfiguration(
            defaultPolicy.validationLevelPolicy(),
            defaultPolicy.unknownCodeSystemsPolicy(),
            defaultPolicy.exampleCodeSystemsUsagePolicy(),
            defaultPolicy.exampleUrlsUsagePolicy(),
            defaultPolicy.exampleRestReferencesPolicy(),
            recursiveModePolicy,
            defaultPolicy.anyExtensionPolicy());
    final var configuration =
        new ContextConfiguration(
            defaults.fhirRelease(),
            defaults.locale(),
            defaults.displayBehavior(),
            new PackageDownloadConfiguration(
                PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED, packageCachePath),
            defaults.terminology(),
            validationPolicy);

    final var validationOptions =
        profileCanonical == null
            ? ValidationOptions.defaultConfiguration()
            : new ValidationOptions(
                ProfileCanonical.fromCanonical(profileCanonical),
                null,
                ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
                ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE);
    try (var validator =
        ValidatorFactory.withCustomPackages(configuration, List.of(PACKAGE_PATH))) {
      return validator
          .validate(new ValidationRequest(resource), validationOptions)
          .messages()
          .stream()
          .toList();
    }
  }

  private static void assertNoErrors(List<ResultMessage> messages) {
    assertFalse(
        messages.stream()
            .anyMatch(
                message ->
                    message.severity() == IssueSeverity.ERROR
                        || message.severity() == IssueSeverity.FATAL),
        () -> "Expected no errors, received: " + messages);
  }
}
