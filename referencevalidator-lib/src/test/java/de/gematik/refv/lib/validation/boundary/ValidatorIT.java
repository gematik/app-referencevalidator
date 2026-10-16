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

import de.gematik.refv.lib.common.ResultPrinter;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.fhir_context.entity.R4Version;
import de.gematik.refv.lib.fhir_context.entity.TerminologyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationModuleIndex;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolverFactory;
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationRequest;
import de.gematik.refv.lib.valmodule.boundary.ModuleLoader;
import de.gematik.refv.lib.valmodule.entity.ValidationModule;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidatorIT {
  private final List<Validator> validators = new ArrayList<>();

  @AfterEach
  void closeValidators() {
    validators.forEach(Validator::close);
  }

  @DisplayName("Performs the validation of a JSON resource using HL7 Core Profile")
  @Test
  void testValidationOfJsonWithCoreProfileWorks() {
    // Given a JSON Resource
    FhirResource fhirResource =
        FhirResource.fromJson(
            Path.of("src/test/resources/fhir/isik/Observation-ISiKKopfumfangMaxExample.json"));
    // and a Validator
    Validator validator =
        track(ValidatorFactory.withCoreDefinitions(ContextConfiguration.defaultConfiguration()));
    // and custom validation options
    ValidationOptions validationOptions =
        new ValidationOptions(
            ProfileCanonical.fromCanonical(
                "http://hl7.org/fhir/StructureDefinition/Observation|"
                    + new R4Version().corePackageVersion()),
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(),
            List.of());
    // When I execute the validation
    final var result =
        Assertions.assertDoesNotThrow(
            () -> validator.validate(new ValidationRequest(fhirResource), validationOptions));
    // Then I expect a successful result
    Assertions.assertNotNull(result);
    ResultPrinter.printMessages(result);
    Assertions.assertTrue(
        result.messages().stream()
            .noneMatch(
                resultMessage ->
                    resultMessage.severity().equals(IssueSeverity.ERROR)
                        || resultMessage.severity().equals(IssueSeverity.FATAL)));
  }

  @DisplayName("Performs the validation of a XML resource using HL7 Core Profile")
  @Test
  void testValidationOfXmlWithCoreProfileWorks() {
    // Given a XML Resource
    FhirResource fhirResource =
        FhirResource.fromXml(
            Path.of("src/test/resources/fhir/erezept/PZN_Unfall_eAbgabedaten.xml"));
    // and a Validator
    Validator validator =
        track(ValidatorFactory.withCoreDefinitions(ContextConfiguration.defaultConfiguration()));
    // and custom validation options
    ValidationOptions validationOptions =
        new ValidationOptions(
            ProfileCanonical.fromCanonical(
                "http://hl7.org/fhir/StructureDefinition/Bundle|"
                    + new R4Version().corePackageVersion()),
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
            List.of(),
            List.of());
    // When I execute the validation
    final var result =
        Assertions.assertDoesNotThrow(
            () -> validator.validate(new ValidationRequest(fhirResource), validationOptions));
    // Then I expect a successful result
    Assertions.assertNotNull(result);
    ResultPrinter.printMessages(result);
    Assertions.assertTrue(
        result.messages().stream()
            .noneMatch(
                resultMessage ->
                    resultMessage.severity().equals(IssueSeverity.ERROR)
                        || resultMessage.severity().equals(IssueSeverity.FATAL)));
  }

  @DisplayName("Performs the validation of a JSON resource using Remote Implementation Guide")
  @Test
  void testValidationOfXmlWithRemoteImplementationGuideWorks() {
    // Given a JSON Resource
    final FhirResource fhirResource =
        FhirResource.fromJson(
            Path.of("src/test/resources/fhir/isik/Observation-ISiKKopfumfangMaxExample.json"));
    // and a Context Configuration
    final var contextConfiguration =
        new ContextConfiguration(
            FhirRelease.asR4(),
            "en-GB",
            DisplayBehaviorConfiguration.defaultConfiguration(),
            new PackageDownloadConfiguration(
                PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED),
            TerminologyConfiguration.defaultConfiguration(),
            ValidationPolicyConfiguration.defaultConfiguration());
    // and custom validation options
    final var validationOptions = ValidationOptions.defaultConfiguration();
    // and when I detect the packages to load
    final var packagesToLoad = List.of("de.gematik.terminology#1.0.9");
    // When a Validator is created
    Validator validator =
        track(ValidatorFactory.withCustomPackages(contextConfiguration, packagesToLoad));
    // When I execute the validation
    final var result =
        Assertions.assertDoesNotThrow(
            () -> validator.validate(new ValidationRequest(fhirResource), validationOptions));
    // Then I expect a successful result
    Assertions.assertNotNull(result);
    ResultPrinter.printMessages(result);
    Assertions.assertTrue(
        result.messages().stream()
            .noneMatch(
                resultMessage ->
                    resultMessage.severity().equals(IssueSeverity.ERROR)
                        || resultMessage.severity().equals(IssueSeverity.FATAL)));
  }

  @DisplayName(
      "Performs the validation of a JSON resource using a Validation Module declaring remote IG, without terminology server")
  @Test
  void testValidationOfJsonWithRemoteIgModuleWorks() {
    // Given a JSON Resource
    final FhirResource fhirResource =
        FhirResource.fromJson(
            Path.of("src/test/resources/fhir/isik/Observation-ISiKKopfumfangMaxExample.json"));
    // and a Context Configuration
    final var contextConfiguration = ContextConfiguration.defaultConfiguration();
    // and custom validation options
    final var validationOptions = ValidationOptions.defaultConfiguration();
    // When I load a Validation Module
    final ModuleLoader moduleLoader = ModuleLoader.defaultLoader();
    final var validationModule =
        moduleLoader.forValidation(Path.of("src/test/resources/modules"), "isik5");
    Assertions.assertTrue(validationModule.isPresent());
    // and when I detect the packages to load
    final var packagesToLoad =
        detectPackagesToLoad(
            contextConfiguration, validationOptions, validationModule.get(), fhirResource);
    // When a Validator is created
    Validator validator =
        track(ValidatorFactory.withCustomPackages(contextConfiguration, packagesToLoad));
    // When I execute the validation
    final var result =
        Assertions.assertDoesNotThrow(
            () ->
                validator.validate(
                    new ValidationRequest(
                        FhirResource.fromJson(
                            Path.of(
                                "src/test/resources/fhir/isik/Observation-ISiKKopfumfangMaxExample.json"))),
                    validationOptions));
    // Then I expect a successful result
    Assertions.assertNotNull(result);
    ResultPrinter.printMessages(result);
    Assertions.assertTrue(
        result.messages().stream()
            .noneMatch(
                resultMessage ->
                    resultMessage.severity().equals(IssueSeverity.ERROR)
                        || resultMessage.severity().equals(IssueSeverity.FATAL)));
  }

  private @NonNull List<String> detectPackagesToLoad(
      @NonNull ContextConfiguration contextConfiguration,
      @NonNull ValidationOptions validationOptions,
      @NonNull ValidationModule validationModule,
      @NonNull FhirResource resource) {
    // Preload Cache
    final var packageResolver =
        PackageResolverFactory.withConfiguration(contextConfiguration.packageLoading());
    packageResolver.loadCore(contextConfiguration.fhirRelease());
    packageResolver.loadModulePackages(validationModule.modulePath());
    // Core packages must always be loaded
    final List<String> packagesToLoad =
        new ArrayList<>(contextConfiguration.fhirRelease().packages());
    final var validationModuleIndex = new ValidationModuleIndex(validationModule);
    final var validationPackageSelector = new ValidationPackageSelector(validationModuleIndex);
    packagesToLoad.addAll(
        validationPackageSelector.getPackagesForResource(
            resource, contextConfiguration.fhirRelease(), validationOptions));

    // Translate all the tgz coordinate packages into remote ones (they are already in cache)
    return packagesToLoad.stream()
        .map(
            s ->
                s.contains(LocalArchive.PACKAGE_SEPARATOR)
                        && s.endsWith(LocalArchive.ARCHIVE_PACKAGE_EXTENSION)
                    ? LocalArchive.parse(s).coordinates()
                    : s)
        .toList();
  }

  private Validator track(Validator validator) {
    validators.add(validator);
    return validator;
  }
}
