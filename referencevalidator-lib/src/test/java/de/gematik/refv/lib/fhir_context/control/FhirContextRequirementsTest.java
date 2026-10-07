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

import de.gematik.refv.lib.exceptions.ValidationException;
import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.TerminologyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FhirContextRequirementsTest {

  private static final String CUSTOM_PROFILE =
      "http://fhir.de/StructureDefinition/identifier-reisepassnummer|1.4.0";
  private static final String MINIMAL_PACKAGE_COORDINATES = "minimal.example#1.0.0";

  @TempDir Path temporaryDirectory;

  /// Requirement `R1.1`
  @Test
  @DisplayName("R1.1")
  void r1_1_contextsReportTheirConfiguredFhirRelease() {
    var configuration = configuration(FhirRelease.asR5());
    var provider = ContextProvider.defaultProvider();

    try (var validationContext = provider.validationContext(configuration);
        var snapshotContext = provider.snapshotGenerationContext(configuration)) {
      Assertions.assertEquals(FhirRelease.asR5(), validationContext.fhirVersion());
      Assertions.assertEquals(FhirRelease.asR5(), snapshotContext.fhirVersion());
    } catch (Exception e) {
      Assertions.fail(e.getMessage());
    }
  }

  /// Requirement `R1.2` Requirement `R2.2`
  @Test
  @DisplayName("R1.2 · R2.2")
  void r1_2_r2_2_validationWithoutProfilesUsesCoreFhirStructures() {
    var configuration = configuration(FhirRelease.asR4());
    var provider = ContextProvider.defaultProvider();
    var patient =
        FhirResource.fromJson(
            """
        {"resourceType":"Patient","id":"core-patient","active":true}
        """);

    try (var context = provider.validationContext(configuration)) {
      var result = context.validate(patient, List.of());

      Assertions.assertFalse(
          result.messages().stream()
              .anyMatch(
                  message ->
                      message.severity() == IssueSeverity.ERROR
                          || message.severity() == IssueSeverity.FATAL));
    } catch (Exception e) {
      Assertions.fail(e.getMessage());
    }
  }

  /// Requirement `R1.3` Requirement `R2.1` Requirement `R4.1`
  @Test
  @DisplayName("R1.3 · R2.1 · R4.1")
  void r1_3_r2_1_r4_1_clonedValidationContextLoadsResolvedProfiles() {
    var configuration = configuration(FhirRelease.asR4());
    var provider = ContextProvider.defaultProvider();
    var source = provider.validationContext(configuration);
    var customPackage = minimalResolvedPackage();
    var clone = source.cloneContext(List.of(customPackage));
    var invalidIdentifier =
        FhirResource.fromJson(
            """
            {
              "resourceType":"Identifier",
              "system":"https://example.org/incorrect-system",
              "value":"passport-123"
            }
            """);

    try (source;
        clone) {
      Assertions.assertEquals(configuration.fhirRelease(), clone.fhirVersion());
      var result =
          clone.validate(
              invalidIdentifier, List.of(ProfileCanonical.fromCanonical(CUSTOM_PROFILE)));

      Assertions.assertTrue(
          result.messages().stream()
              .anyMatch(message -> message.severity() == IssueSeverity.ERROR));
    } catch (Exception e) {
      Assertions.fail(e.getMessage());
    }
  }

  /// Requirement `R2.3`
  @Test
  @DisplayName("R2.3")
  void r2_3_unavailableProfileIsReportedAsValidationFailure() {
    var provider = ContextProvider.defaultProvider();
    var context = provider.validationContext(configuration(FhirRelease.asR4()));
    var resource =
        FhirResource.fromJson(
            """
        {"resourceType":"Patient","id":"profile-check"}
        """);
    var missingProfile =
        new ProfileCanonical(
            URI.create("https://example.org/StructureDefinition/missing"), "1.0.0");
    var missingProfiles = List.of(missingProfile);

    try (context) {
      Assertions.assertThrows(
          ValidationException.class, () -> context.validate(resource, missingProfiles));
    } catch (Exception e) {
      Assertions.fail(e.getMessage());
    }
  }

  /// Requirement `R3.1`
  @Test
  @DisplayName("R3.1")
  void r3_1_packageStructureDefinitionsWithoutSnapshotsAreWrittenBack() throws Exception {
    var packageRoot = createPackageWithStructureDefinition(true);
    var context =
        ContextProvider.defaultProvider()
            .snapshotGenerationContext(configuration(FhirRelease.asR4()));

    try (context) {
      var result = context.generateSnapshots(packageRoot);
      var definition = Files.readString(packageRoot.resolve("package/Profile-test.json"));

      Assertions.assertFalse(
          result.messages().stream()
              .anyMatch(message -> message.severity() == IssueSeverity.ERROR));
      Assertions.assertTrue(definition.contains("\"snapshot\""));
    }
  }

  /// Requirement `R3.2`
  @Test
  @DisplayName("R3.2")
  void r3_2_packageWithoutStructureDefinitionsReturnsWarning() throws Exception {
    var packageRoot = createPackageWithStructureDefinition(false);
    var context =
        ContextProvider.defaultProvider()
            .snapshotGenerationContext(configuration(FhirRelease.asR4()));

    try (context) {
      var result = context.generateSnapshots(packageRoot);

      Assertions.assertTrue(
          result.messages().stream()
              .anyMatch(message -> message.severity() == IssueSeverity.WARNING));
    }
  }

  /// Requirement `R4.2`
  @Test
  @DisplayName("R4.2")
  void r4_2_clonedSnapshotContextLoadsResolvedProfiles() throws Exception {
    var configuration = configuration(FhirRelease.asR4());
    var provider = ContextProvider.defaultProvider();
    var source = provider.snapshotGenerationContext(configuration);
    var clone = source.cloneContext(List.of(minimalResolvedPackage()));
    var packageRoot =
        createPackageWithStructureDefinition(
            true, "http://fhir.de/StructureDefinition/identifier-reisepassnummer");

    try (source;
        clone) {
      Assertions.assertEquals(configuration.fhirRelease(), clone.fhirVersion());
      var result = clone.generateSnapshots(packageRoot);
      var definition = Files.readString(packageRoot.resolve("package/Profile-test.json"));

      Assertions.assertFalse(
          result.messages().stream()
              .anyMatch(message -> message.severity() == IssueSeverity.ERROR));
      Assertions.assertTrue(definition.contains("\"snapshot\""));
    }
  }

  /// Requirement R5.1
  @Test
  @DisplayName("R5.1")
  void r5_1_snapshotContextReportsConfiguredPackageCachePath() throws Exception {
    var configuration = configuration(FhirRelease.asR4());
    Assertions.assertFalse(Files.exists(configuration.packageLoading().cachePath()));
    var context = ContextProvider.defaultProvider().snapshotGenerationContext(configuration);

    try (context) {
      Assertions.assertEquals(configuration.packageLoading().cachePath(), context.cachePath());
      Assertions.assertTrue(Files.isDirectory(configuration.packageLoading().cachePath()));
    }
  }

  private ContextConfiguration configuration(FhirRelease fhirRelease) {
    var packageCache = temporaryDirectory.resolve("cache");
    return new ContextConfiguration(
        fhirRelease,
        "de",
        DisplayBehaviorConfiguration.defaultConfiguration(),
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED, packageCache),
        TerminologyConfiguration.defaultConfiguration(),
        ValidationPolicyConfiguration.defaultConfiguration());
  }

  private ResolvedPackage minimalResolvedPackage() {
    var packagePath = Path.of("src/test/resources/packages/minimal.example#1.0.0");
    return new ResolvedPackage(
        PackageId.parse(MINIMAL_PACKAGE_COORDINATES), packagePath, List.of());
  }

  private Path createPackageWithStructureDefinition(boolean includeStructureDefinition)
      throws Exception {
    return createPackageWithStructureDefinition(
        includeStructureDefinition, "http://hl7.org/fhir/StructureDefinition/Identifier");
  }

  private Path createPackageWithStructureDefinition(
      boolean includeStructureDefinition, String baseDefinition) throws Exception {
    var packageRoot = temporaryDirectory.resolve("test.package#1.0.0");
    var packageDirectory = Files.createDirectories(packageRoot.resolve("package"));
    Files.writeString(
        packageDirectory.resolve("package.json"),
        """
        {"name":"test.package","version":"1.0.0","fhirVersions":["4.0.1"]}
        """);
    if (includeStructureDefinition) {
      Files.writeString(
          packageDirectory.resolve("Profile-test.json"),
          """
          {
            "resourceType":"StructureDefinition",
            "url":"https://example.org/fhir/StructureDefinition/TestIdentifier",
            "name":"TestIdentifier",
            "status":"draft",
            "fhirVersion":"4.0.1",
            "kind":"complex-type",
            "abstract":false,
            "type":"Identifier",
            "baseDefinition":"%s",
            "derivation":"constraint",
            "differential":{"element":[
              {"id":"Identifier","path":"Identifier"},
              {"id":"Identifier.value","path":"Identifier.value","min":1}
            ]}
          }
          """
              .formatted(baseDefinition));
    }
    return packageRoot;
  }
}
