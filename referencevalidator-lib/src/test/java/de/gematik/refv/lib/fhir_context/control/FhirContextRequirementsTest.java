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

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.refv.lib.exceptions.ValidationException;
import de.gematik.refv.lib.fhir_context.boundary.BatchValidationContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.ValidationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.fhir_context.entity.TerminologyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolverFactory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.PackageResolution;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FhirContextRequirementsTest {

  private static final String CUSTOM_PROFILE =
      "http://fhir.de/StructureDefinition/identifier-reisepassnummer|1.4.0";
  private static final String MINIMAL_PACKAGE_COORDINATES = "minimal.example#1.0.0";

  @TempDir Path temporaryDirectory;

  @ParameterizedTest(name = "{0}")
  @MethodSource("contextCreationCases")
  void contextCreationRequirements(String requirement, RequirementScenario scenario)
      throws Exception {
    scenario.verify(this);
  }

  static Stream<Arguments> contextCreationCases() {
    return Stream.of(
        requirement(
            "R1.1", FhirContextRequirementsTest::r1_1_contextsReportTheirConfiguredFhirRelease),
        requirement(
            "R1.2",
            FhirContextRequirementsTest::r1_2_r2_2_validationWithoutProfilesUsesCoreFhirStructures),
        requirement("R1.3", FhirContextRequirementsTest::createdContextsLoadSuppliedPackages),
        requirement(
            "R1.4", FhirContextRequirementsTest::closedContextsReleaseSharedTerminologyCaches));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("resourceValidationCases")
  void resourceValidationRequirements(String requirement, RequirementScenario scenario)
      throws Exception {
    scenario.verify(this);
  }

  static Stream<Arguments> resourceValidationCases() {
    return Stream.of(
        requirement(
            "R2.1",
            FhirContextRequirementsTest
                ::r1_3_r2_1_r4_1_clonedValidationContextLoadsResolvedProfiles),
        requirement(
            "R2.2",
            FhirContextRequirementsTest::r1_2_r2_2_validationWithoutProfilesUsesCoreFhirStructures),
        requirement(
            "R2.3",
            FhirContextRequirementsTest::r2_3_unavailableProfileIsReportedAsValidationFailure),
        requirement(
            "R2.4", FhirContextRequirementsTest::recursiveValidationUsesEachChildsOwnProfiles));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("snapshotGenerationCases")
  void snapshotGenerationRequirements(String requirement, RequirementScenario scenario)
      throws Exception {
    scenario.verify(this);
  }

  static Stream<Arguments> snapshotGenerationCases() {
    return Stream.of(
        requirement(
            "R3.1",
            FhirContextRequirementsTest
                ::r3_1_packageStructureDefinitionsWithoutSnapshotsAreWrittenBack),
        requirement(
            "R3.2",
            FhirContextRequirementsTest::r3_2_packageWithoutStructureDefinitionsReturnsWarning));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("contextCloningCases")
  void contextCloningRequirements(String requirement, RequirementScenario scenario)
      throws Exception {
    scenario.verify(this);
  }

  static Stream<Arguments> contextCloningCases() {
    return Stream.of(
        requirement(
            "R4.1",
            FhirContextRequirementsTest
                ::r1_3_r2_1_r4_1_clonedValidationContextLoadsResolvedProfiles),
        requirement(
            "R4.2", FhirContextRequirementsTest::r4_2_clonedSnapshotContextLoadsResolvedProfiles));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("batchValidationCases")
  void batchValidationRequirements(String requirement, RequirementScenario scenario)
      throws Exception {
    scenario.verify(this);
  }

  static Stream<Arguments> batchValidationCases() {
    return Stream.of(
        requirement("R6.1", FhirContextRequirementsTest::batchReusesRetainedPackageSetTemplate),
        requirement("R6.2", FhirContextRequirementsTest::batchLeaseReturnsIsolatedContexts),
        requirement("R6.3", FhirContextRequirementsTest::batchClosureWaitsForActiveLeases));
  }

  static Arguments requirement(String id, RequirementScenario scenario) {
    return Arguments.of(id, scenario);
  }

  @FunctionalInterface
  interface RequirementScenario {
    void verify(FhirContextRequirementsTest test) throws Exception;
  }

  void r1_1_contextsReportTheirConfiguredFhirRelease() throws Exception {
    var provider = ContextProvider.defaultProvider();
    for (var release : List.of(FhirRelease.asR4(), FhirRelease.asR5())) {
      var configuration = configuration(release);
      try (var validationContext = provider.validationContext(configuration);
          var snapshotContext = provider.snapshotGenerationContext(configuration)) {
        Assertions.assertEquals(release, validationContext.fhirVersion());
        Assertions.assertEquals(release, snapshotContext.fhirVersion());
      }
    }
  }

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
              .anyMatch(FhirContextRequirementsTest::isPassportSystemViolation),
          result.messages()::toString);
    } catch (Exception e) {
      Assertions.fail(e.getMessage());
    }
  }

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

  void createdContextsLoadSuppliedPackages() throws Exception {
    var configuration = configuration(FhirRelease.asR4());
    // Transitive resolution uses the offline cache, not the bundled core-package source.
    PackageResolverFactory.withConfiguration(configuration.packageLoading())
        .loadCore(configuration.fhirRelease());
    var provider = ContextProvider.defaultProvider();
    var customPackage = minimalResolvedPackage();
    var resource =
        FhirResource.fromJson(
            """
            {"resourceType":"Identifier","system":"https://example.org/incorrect-system","value":"passport-123"}
            """);
    var packageRoot =
        createPackageWithStructureDefinition(
            true, "http://fhir.de/StructureDefinition/identifier-reisepassnummer");

    try (var validation =
            provider.validationContext(
                configuration, List.of(customPackage.packagePath().toString()));
        var snapshot =
            provider.snapshotGenerationContext(
                configuration, new PackageResolution(List.of(customPackage)))) {
      var validationResult =
          validation.validate(resource, List.of(ProfileCanonical.fromCanonical(CUSTOM_PROFILE)));
      Assertions.assertTrue(
          validationResult.messages().stream()
              .anyMatch(FhirContextRequirementsTest::isPassportSystemViolation),
          validationResult.messages()::toString);
      var snapshotResult = snapshot.generateSnapshots(packageRoot);
      Assertions.assertFalse(
          snapshotResult.messages().stream().anyMatch(FhirContextRequirementsTest::isError));
      Assertions.assertTrue(
          Files.readString(packageRoot.resolve("package/Profile-test.json"))
              .contains("\"snapshot\""));
    }
  }

  void closedContextsReleaseSharedTerminologyCaches() throws Exception {
    var configuration = configuration(FhirRelease.asR4());
    var provider = ContextProvider.defaultProvider();
    var ownedCache = new AtomicReference<Path>();
    try (var factory = mockStatic(ValidationEngineFactory.class, CALLS_REAL_METHODS)) {
      factory
          .when(() -> ValidationEngineFactory.createNewCoreEngine(configuration))
          .thenAnswer(invocation -> captureCache(invocation.callRealMethod(), ownedCache));
      try (var source = provider.validationContext(configuration);
          var clone = provider.clone(source)) {
        var cache = ownedCache.get();
        Assertions.assertNotNull(cache);
        Assertions.assertTrue(Files.isDirectory(cache));
        source.close();
        Assertions.assertTrue(Files.isDirectory(cache));
        var result =
            clone.validate(
                FhirResource.fromJson("{\"resourceType\":\"Patient\",\"active\":true}"), List.of());
        Assertions.assertFalse(
            result.messages().stream().anyMatch(FhirContextRequirementsTest::isError));
        clone.close();
        Assertions.assertFalse(Files.exists(cache));
      }
      try (var source = provider.snapshotGenerationContext(configuration);
          var clone = provider.clone(source)) {
        var cache = ownedCache.get();
        Assertions.assertNotNull(cache);
        Assertions.assertTrue(Files.isDirectory(cache));
        source.close();
        Assertions.assertTrue(Files.isDirectory(cache));
        clone.close();
        Assertions.assertFalse(Files.exists(cache));
      }
    }
  }

  static ValidationEngineFactory.EngineSession captureCache(
      Object result, AtomicReference<Path> ownedCache) {
    var session = (ValidationEngineFactory.EngineSession) result;
    ownedCache.set(session.managedTerminologyCachePath());
    return session;
  }

  void recursiveValidationUsesEachChildsOwnProfiles() throws Exception {
    var defaults = configuration(FhirRelease.asR4());
    var policy = defaults.validationPolicy();
    var configuration =
        new ContextConfiguration(
            defaults.fhirRelease(),
            defaults.locale(),
            defaults.displayBehavior(),
            defaults.packageLoading(),
            defaults.terminology(),
            new ValidationPolicyConfiguration(
                policy.validationLevelPolicy(),
                policy.unknownCodeSystemsPolicy(),
                policy.exampleCodeSystemsUsagePolicy(),
                policy.exampleUrlsUsagePolicy(),
                policy.exampleRestReferencesPolicy(),
                ValidationPolicyConfiguration.RecursiveModePolicy.ALLOWED,
                policy.anyExtensionPolicy()));
    var packagePath = Path.of("src/test/resources/packages/recursive-profile#1.0.0");
    var provider = ContextProvider.defaultProvider();
    try (var context = provider.validationContext(configuration, List.of(packagePath.toString()))) {
      var strictResource = embeddedPatients(false);
      var invalidResult = context.validate(strictResource, List.of());
      var errors =
          invalidResult.messages().stream().filter(FhirContextRequirementsTest::isError).toList();
      Assertions.assertTrue(
          errors.stream().anyMatch(FhirContextRequirementsTest::isStrictChildMessage),
          errors::toString);
      Assertions.assertFalse(
          errors.stream().anyMatch(FhirContextRequirementsTest::isLooseChildMessage),
          errors::toString);
      var validResult = context.validate(embeddedPatients(true), List.of());
      Assertions.assertFalse(
          validResult.messages().stream().anyMatch(FhirContextRequirementsTest::isError),
          validResult.messages()::toString);
    }
  }

  static boolean isStrictChildMessage(ResultMessage message) {
    return message.messageContent().startsWith("Parameters.parameter[0].resource:");
  }

  static boolean isLooseChildMessage(ResultMessage message) {
    return message.messageContent().startsWith("Parameters.parameter[1].resource:");
  }

  static FhirResource embeddedPatients(boolean strictActive) {
    return FhirResource.fromJson(
        """
        {"resourceType":"Parameters","parameter":[
          {"name":"strict","resource":{"resourceType":"Patient","id":"strict-patient",
            "meta":{"profile":["http://example.org/StructureDefinition/recursive-patient|1.0.0"]},"active":%s}},
          {"name":"loose","resource":{"resourceType":"Patient","id":"loose-patient",
            "meta":{"profile":["http://example.org/StructureDefinition/recursive-patient-loose|1.0.0"]},"active":false}}
        ]}
        """
            .formatted(strictActive));
  }

  static boolean isError(ResultMessage message) {
    return message.severity() == IssueSeverity.ERROR || message.severity() == IssueSeverity.FATAL;
  }

  static boolean isPassportSystemViolation(ResultMessage message) {
    return isError(message)
        && message.messageContent().contains("http://hl7.org/fhir/sid/passport-DEU");
  }

  void batchReusesRetainedPackageSetTemplate() throws Exception {
    var configuration = configuration(FhirRelease.asR4());
    var provider = mock(ContextProvider.class);
    var template = mock(ValidationContext.class);
    var packages = List.of("guide#1.0.0");
    when(provider.validationContext(configuration, packages)).thenReturn(template);
    try (var batch = BatchValidationContextProvider.defaultProvider(provider, configuration, 2)) {
      try (var _ = batch.acquire(packages);
          var _ = batch.acquire(packages)) {
        verify(provider, times(1)).validationContext(configuration, packages);
        verify(template, never()).close();
      }
      try (var _ = batch.acquire(packages)) {
        verify(provider, times(1)).validationContext(configuration, packages);
        verify(template, never()).close();
      }
    }
    verify(template).close();
  }

  void batchLeaseReturnsIsolatedContexts() throws Exception {
    var configuration = configuration(FhirRelease.asR4());
    var provider = mock(ContextProvider.class);
    var template = mock(ValidationContext.class);
    var firstContext = mock(ValidationContext.class);
    var secondContext = mock(ValidationContext.class);
    var packages = List.of("guide#1.0.0");
    when(provider.validationContext(configuration, packages)).thenReturn(template);
    when(template.cloneContext(List.of())).thenReturn(firstContext, secondContext);
    try (var batch = BatchValidationContextProvider.defaultProvider(provider, configuration, 2);
        var lease = batch.acquire(packages);
        var first = lease.newIsolatedContext();
        var second = lease.newIsolatedContext()) {
      Assertions.assertSame(firstContext, first);
      Assertions.assertSame(secondContext, second);
      Assertions.assertNotSame(template, first);
      Assertions.assertNotSame(template, second);
      Assertions.assertNotSame(first, second);
      verify(template, times(2)).cloneContext(List.of());
    }
    verify(firstContext).close();
    verify(secondContext).close();
    verify(template).close();
  }

  void batchClosureWaitsForActiveLeases() throws Exception {
    var configuration = configuration(FhirRelease.asR4());
    var provider = mock(ContextProvider.class);
    var template = mock(ValidationContext.class);
    var packages = List.of("guide#1.0.0");
    when(provider.validationContext(configuration, packages)).thenReturn(template);
    try (var batch = BatchValidationContextProvider.defaultProvider(provider, configuration, 1);
        var executor = Executors.newSingleThreadExecutor();
        var first = batch.acquire(packages);
        var second = batch.acquire(packages)) {
      var started = new CountDownLatch(1);
      var closing = executor.submit(() -> closeBatch(batch, started));
      Assertions.assertTrue(started.await(5, TimeUnit.SECONDS));
      Assertions.assertThrows(
          TimeoutException.class, () -> closing.get(100, TimeUnit.MILLISECONDS));
      verify(template, never()).close();
      first.close();
      Assertions.assertThrows(
          TimeoutException.class, () -> closing.get(100, TimeUnit.MILLISECONDS));
      verify(template, never()).close();
      second.close();
      closing.get(5, TimeUnit.SECONDS);
      verify(template).close();
    }
  }

  static void closeBatch(BatchValidationContextProvider batch, CountDownLatch started) {
    started.countDown();
    batch.close();
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
