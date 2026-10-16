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
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import de.gematik.refv.lib.validation.boundary.Validator;
import de.gematik.refv.lib.validation.boundary.ValidatorFactory;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.JsonFhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationRequest;
import de.gematik.refv.lib.validation.entity.XmlFhirResource;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

class FhirContextRegressionTest {

  private static final String CUSTOM_PROFILE =
      "http://fhir.de/StructureDefinition/identifier-reisepassnummer|1.4.0";
  private static final String MINIMAL_PACKAGE_COORDINATES = "minimal.example#1.0.0";

  @TempDir Path temporaryDirectory;

  @Test
  void contextsReportTheirConfiguredFhirRelease() {
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

  @Test
  void validationWithoutProfilesUsesCoreFhirStructures() {
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

  @Test
  void clonedValidationContextLoadsResolvedProfiles() {
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

  @Test
  void clonedContextsPreserveJsonAndXmlResultsSequentiallyAndConcurrently() throws Exception {
    var configuration = configuration(FhirRelease.asR4());
    var provider = ContextProvider.defaultProvider();
    var packages = List.<String>of();
    PackageResolverFactory.withConfiguration(configuration.packageLoading())
        .loadCore(configuration.fhirRelease());
    var missingProfile =
        ProfileCanonical.fromCanonical("https://example.org/StructureDefinition/missing|1.0.0");
    var defaultOptions = ValidationOptions.defaultConfiguration();
    var missingProfileOptions =
        new ValidationOptions(
            missingProfile,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE);
    var cases =
        List.of(
            new ValidationCase(
                new JsonFhirResource(
                    """
                    {"resourceType":"Patient","id":"json-patient","active":true}
                    """
                        .getBytes(StandardCharsets.UTF_8)),
                defaultOptions),
            new ValidationCase(
                new XmlFhirResource(
                    """
                    <Patient xmlns="http://hl7.org/fhir"><id value="xml-patient"/><active value="true"/></Patient>
                    """
                        .getBytes(StandardCharsets.UTF_8)),
                defaultOptions),
            new ValidationCase(
                new JsonFhirResource(
                    """
                    {"resourceType":"Patient","id":"json-invalid-gender","gender":"invalid"}
                    """
                        .getBytes(StandardCharsets.UTF_8)),
                defaultOptions),
            new ValidationCase(
                new XmlFhirResource(
                    """
                    <Patient xmlns="http://hl7.org/fhir"><id value="xml-invalid-gender"/><gender value="invalid"/></Patient>
                    """
                        .getBytes(StandardCharsets.UTF_8)),
                defaultOptions),
            new ValidationCase(
                new JsonFhirResource(
                    """
                    {"resourceType":"Patient","id":"json-missing-profile"}
                    """
                        .getBytes(StandardCharsets.UTF_8)),
                missingProfileOptions));
    var expectedOutcomes =
        cases.stream()
            .map(
                validationCase ->
                    validate(
                        ValidatorFactory.withCustomPackages(configuration, packages),
                        validationCase))
            .toList();

    Assertions.assertNotEquals(expectedOutcomes.getFirst(), expectedOutcomes.get(2));
    Assertions.assertNotNull(expectedOutcomes.getLast().failureMessage());

    try (var batchContexts =
        BatchValidationContextProvider.defaultProvider(provider, configuration, 2)) {
      var sequentialRuns = new ArrayList<ValidationRun>(cases.size());
      for (int i = 0; i < cases.size(); i++) {
        try (var lease = batchContexts.acquire(packages);
            var clone = lease.newIsolatedContext()) {
          var validator = ValidatorFactory.withValidationContext(clone);
          var outcome = validate(validator, cases.get(i));
          Assertions.assertEquals(expectedOutcomes.get(i), outcome);
          sequentialRuns.add(new ValidationRun(i, validator, clone, outcome));
        }
      }
      assertDistinctContextsAndValidators(sequentialRuns);

      final int parallelism = 8;
      final var contextsReady = new CountDownLatch(parallelism);
      final var startValidation = new CountDownLatch(1);
      try (var executor = Executors.newFixedThreadPool(parallelism)) {
        var validations = new ArrayList<java.util.concurrent.Future<ValidationRun>>();
        for (int runIndex = 0; runIndex < parallelism; runIndex++) {
          final int validationIndex = runIndex % cases.size();
          validations.add(
              executor.submit(
                  () -> {
                    try (var lease = batchContexts.acquire(packages);
                        var clone = lease.newIsolatedContext()) {
                      var validator = ValidatorFactory.withValidationContext(clone);
                      contextsReady.countDown();
                      if (!startValidation.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start validation");
                      }
                      return new ValidationRun(
                          validationIndex,
                          validator,
                          clone,
                          validate(validator, cases.get(validationIndex)));
                    }
                  }));
        }

        var allContextsReady = contextsReady.await(30, TimeUnit.SECONDS);
        startValidation.countDown();
        Assertions.assertTrue(allContextsReady, "All validation clones should reach the barrier");

        var concurrentRuns = validations.stream().map(FhirContextRegressionTest::get).toList();
        for (var run : concurrentRuns) {
          Assertions.assertEquals(expectedOutcomes.get(run.validationIndex()), run.outcome());
        }
        assertDistinctContextsAndValidators(concurrentRuns);
      }
    }
  }

  /// Requirement R6.2
  @EnabledIfEnvironmentVariable(named = "RUN_REGRESSION_TESTS", matches = "true")
  @Test
  void clonedContextsPreserveConcurrentResultsForRealIgPackageGroupsAndTerminology()
      throws Exception {
    var configuration = configuration(FhirRelease.asR4());
    var provider = ContextProvider.defaultProvider();
    var erezeptPackageDirectory = Path.of("src/test/resources/packages/erezept/minimal");
    var erezeptPackageArchives =
        List.of(
            erezeptPackageDirectory.resolve("hl7.fhir.r4.core-4.0.1.tgz").toString(),
            erezeptPackageDirectory.resolve("de.basisprofil.r4-1.5.2.tgz").toString(),
            erezeptPackageDirectory.resolve("de.abda.erezeptabgabedatenbasis-1.5.0.tgz").toString(),
            erezeptPackageDirectory.resolve("de.abda.erezeptabgabedaten-1.5.0.tgz").toString());
    var erezeptPackages =
        List.of(
            "hl7.fhir.r4.core#4.0.1",
            "de.basisprofil.r4#1.5.2",
            "de.abda.erezeptabgabedatenbasis#1.5.0",
            "de.abda.erezeptabgabedaten#1.5.0");
    var minimalPackageDirectory = "src/test/resources/packages/minimal.example#1.0.0";
    var minimalPackages = List.of("minimal.example#1.0.0");
    var erezeptProfile =
        ProfileCanonical.fromCanonical(
            "http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/"
                + "DAV-PR-ERP-AbgabedatenBundle|1.5.0");
    var erezeptOptions =
        new ValidationOptions(
            erezeptProfile,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE);
    var pharmacyProfile =
        ProfileCanonical.fromCanonical(
            "http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/"
                + "DAV-PR-ERP-Apotheke|1.5.0");
    var pharmacyOptions =
        new ValidationOptions(
            pharmacyProfile,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE);
    var identifierOptions =
        new ValidationOptions(
            ProfileCanonical.fromCanonical(CUSTOM_PROFILE),
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE);

    var erezeptXml =
        Files.readString(Path.of("src/test/resources/fhir/erezept/PZN_Unfall_eAbgabedaten.xml"));
    var pharmacyXml =
        """
        <Organization xmlns="http://hl7.org/fhir">
          <meta><profile value="http://fhir.abda.de/eRezeptAbgabedaten/StructureDefinition/DAV-PR-ERP-Apotheke|1.5"/></meta>
          <identifier><system value="http://fhir.de/sid/arge-ik/iknr"/><value value="308412345"/></identifier>
          <name value="CIDA-Apotheke"/>
          <address>
            <type value="physical"/>
            <line value="Schottener Weg 5"/>
            <city value="Darmstadt"/>
            <postalCode value="64289"/>
            <country value="D"/>
          </address>
        </Organization>
        """;
    var invalidTerminologyXml =
        pharmacyXml.replace("<country value=\"D\"/>", "<country value=\"ZZ\"/>");
    Assertions.assertNotEquals(pharmacyXml, invalidTerminologyXml);

    var cases =
        List.of(
            new PackageValidationCase(
                erezeptPackages,
                new ValidationCase(
                    new XmlFhirResource(erezeptXml.getBytes(StandardCharsets.UTF_8)),
                    erezeptOptions)),
            new PackageValidationCase(
                erezeptPackages,
                new ValidationCase(
                    new XmlFhirResource(pharmacyXml.getBytes(StandardCharsets.UTF_8)),
                    pharmacyOptions)),
            new PackageValidationCase(
                erezeptPackages,
                new ValidationCase(
                    new XmlFhirResource(invalidTerminologyXml.getBytes(StandardCharsets.UTF_8)),
                    pharmacyOptions)),
            new PackageValidationCase(
                minimalPackages,
                new ValidationCase(
                    FhirResource.fromJson(
                        """
                        {"resourceType":"Identifier","system":"https://example.org/incorrect-system","value":"passport-123"}
                        """),
                    identifierOptions)));
    // The batch cache key and provider expect package coordinates, so seed this test's offline
    // cache
    // from the checked-in archive and directory fixtures first.
    var packageResolver = PackageResolverFactory.withConfiguration(configuration.packageLoading());
    packageResolver.resolveList(erezeptPackageArchives);
    packageResolver.resolveList(List.of(minimalPackageDirectory));
    var expectedOutcomes =
        cases.stream()
            .map(
                validationCase ->
                    validate(
                        ValidatorFactory.withCustomPackages(
                            configuration, validationCase.packages()),
                        validationCase.validationCase()))
            .toList();

    Assertions.assertNotEquals(
        expectedOutcomes.get(1),
        expectedOutcomes.get(2),
        "An unknown country code in the real IG's required local value set must be reported");
    Assertions.assertTrue(
        expectedOutcomes.get(2).messages().stream()
            .anyMatch(message -> message.messageContent().contains("nicht im ValueSet")),
        "The invalid country code must produce a terminology validation message: "
            + expectedOutcomes.get(2).messages());

    try (var batchContexts =
            BatchValidationContextProvider.defaultProvider(provider, configuration, 2);
        var executor = Executors.newFixedThreadPool(5)) {
      final int parallelism = 5;
      var ready = new CountDownLatch(parallelism);
      var startValidation = new CountDownLatch(1);
      var validations = new ArrayList<java.util.concurrent.Future<ValidationRun>>(parallelism);
      for (int runIndex = 0; runIndex < parallelism; runIndex++) {
        final var validationIndex = runIndex % cases.size();
        final var validationCase = cases.get(validationIndex);
        validations.add(
            executor.submit(
                () -> {
                  var reachedBarrier = false;
                  try {
                    try (var lease = batchContexts.acquire(validationCase.packages());
                        var context = lease.newIsolatedContext()) {
                      var validator = ValidatorFactory.withValidationContext(context);
                      ready.countDown();
                      reachedBarrier = true;
                      if (!startValidation.await(60, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start validation");
                      }
                      return new ValidationRun(
                          validationIndex,
                          validator,
                          context,
                          validate(validator, validationCase.validationCase()));
                    }
                  } finally {
                    if (!reachedBarrier) {
                      ready.countDown();
                    }
                  }
                }));
      }

      var allContextsReady = ready.await(60, TimeUnit.SECONDS);
      startValidation.countDown();
      Assertions.assertTrue(
          allContextsReady, "All real-IG validation clones should reach the barrier");

      var concurrentRuns = validations.stream().map(FhirContextRegressionTest::getSlow).toList();
      for (var run : concurrentRuns) {
        Assertions.assertEquals(
            expectedOutcomes.get(run.validationIndex()),
            run.outcome(),
            "Concurrent clone result must match the existing custom-package path");
      }
      assertDistinctContextsAndValidators(concurrentRuns);
    }
  }

  @Test
  void unavailableProfileIsReportedAsValidationFailure() {
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
    var profiles = List.of(missingProfile);

    try (context) {
      Assertions.assertThrows(
          ValidationException.class, () -> context.validate(resource, profiles));
    } catch (Exception e) {
      Assertions.fail(e.getMessage());
    }
  }

  @Test
  void packageStructureDefinitionsWithoutSnapshotsAreWrittenBack() throws Exception {
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

  @Test
  void packageWithoutStructureDefinitionsReturnsWarning() throws Exception {
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

  @Test
  void clonedSnapshotContextLoadsResolvedProfiles() throws Exception {
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

  private static ValidationOutcome validate(Validator validator, ValidationCase validationCase) {
    try (validator) {
      var result =
          validator.validate(
              new ValidationRequest(validationCase.resource()), validationCase.options());
      return new ValidationOutcome(List.copyOf(result.messages()), null);
    } catch (ValidationException exception) {
      return new ValidationOutcome(List.of(), exception.getMessage());
    }
  }

  private static void assertDistinctContextsAndValidators(List<ValidationRun> runs) {
    var contexts = new IdentityHashMap<Object, Boolean>();
    var validators = new IdentityHashMap<Object, Boolean>();
    runs.forEach(
        run -> {
          contexts.put(run.context(), Boolean.TRUE);
          validators.put(run.validator(), Boolean.TRUE);
        });
    Assertions.assertEquals(runs.size(), contexts.size(), "Each run must own its own context");
    Assertions.assertEquals(runs.size(), validators.size(), "Each run must own its own validator");
  }

  private static ValidationRun get(java.util.concurrent.Future<ValidationRun> future) {
    try {
      return future.get(30, TimeUnit.SECONDS);
    } catch (Exception exception) {
      throw new AssertionError("Concurrent validation failed", exception);
    }
  }

  private static ValidationRun getSlow(java.util.concurrent.Future<ValidationRun> future) {
    try {
      return future.get(90, TimeUnit.SECONDS);
    } catch (Exception exception) {
      throw new AssertionError("Concurrent real-IG validation failed", exception);
    }
  }

  private record ValidationCase(FhirResource resource, ValidationOptions options) {}

  private record PackageValidationCase(List<String> packages, ValidationCase validationCase) {}

  private record ValidationOutcome(List<ResultMessage> messages, String failureMessage) {}

  private record ValidationRun(
      int validationIndex,
      Validator validator,
      ValidationContext context,
      ValidationOutcome outcome) {}
}
