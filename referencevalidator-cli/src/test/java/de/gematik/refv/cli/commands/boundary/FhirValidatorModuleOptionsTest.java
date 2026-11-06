/*-
 * #%L
 * referencevalidator-cli
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
package de.gematik.refv.cli.commands.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.refv.lib.exceptions.LoadModuleException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.ValidationModuleIndex;
import de.gematik.refv.lib.validation.boundary.ValidationPackageSelector;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.valmodule.boundary.ModuleLoader;
import de.gematik.refv.lib.valmodule.entity.ValidationModule;
import de.gematik.refv.valmodule.api.entity.MessageTransformation;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FhirValidatorModuleOptionsTest {
  @DisplayName("Check that the ignored terminology options are parsed")
  @Test
  void ignoredTerminologyOptionsAreParsedFromTheModuleManifest() throws LoadModuleException {
    final var module =
        ModuleLoader.defaultLoader()
            .forValidation(Path.of("../referencevalidator-lib/src/test/resources/modules"), "isik5")
            .orElseThrow();
    final var options =
        FhirValidatorCommand.withValidationModuleOptions(
            ValidationOptions.defaultConfiguration(), module.configuration());

    assertThat(options.ignoredCodeSystems()).contains("http://loinc.org", "http://snomed.info/sct");
    assertThat(options.ignoredValueSets())
        .contains("https://gematik.de/fhir/isik/ValueSet/ProzedurenCodesSCT");
  }

  @DisplayName("Only transformations from the selected profile package group are applied")
  @Test
  void transformationsFollowTheSelectedPackageGroup() {
    final var earlyTransformation = transformation("EARLY");
    final var lateTransformation = transformation("LATE");
    final var globalTransformation = transformation("GLOBAL");
    final var manifest =
        new ValidationModuleManifest(
            "3.0",
            "example",
            "1.0.0",
            "test",
            "test",
            null,
            false,
            false,
            false,
            List.of(),
            Map.of(
                "early", new PackageGroup(List.of("early#1.0.0"), List.of("early-rules")),
                "late", new PackageGroup(List.of("late#2.0.0"), List.of("late-rules"))),
            Map.of(
                "early-rules", List.of(earlyTransformation),
                "late-rules", List.of(lateTransformation)),
            Map.of(
                "family",
                new ProfileFamily(
                    "http://example.org/fhir/StructureDefinition",
                    "1.0",
                    Map.of(
                        "1.0",
                        new ProfileVersion(
                            List.of(
                                new PackageGroupReference(
                                    "early", LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31)),
                                new PackageGroupReference(
                                    "late", LocalDate.of(2025, 1, 1), null)))),
                    Map.of("Patient", new ProfileDefinition("Patient.birthDate", null)))),
            List.of(),
            List.of());
    final var baseOptions =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE,
            List.of(globalTransformation),
            List.of());
    final var moduleOptions =
        FhirValidatorCommand.withValidationModuleOptions(baseOptions, manifest);
    final var baseWithoutSuppressionRules =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE,
            null,
            null);
    final var moduleOptionsWithoutSuppressionRules =
        FhirValidatorCommand.withValidationModuleOptions(baseWithoutSuppressionRules, manifest);
    final var selector =
        new ValidationPackageSelector(
            new ValidationModuleIndex(new ValidationModule(manifest, Path.of("module.jar"))));

    assertThat(moduleOptions.messageTransformations()).containsExactly(globalTransformation);
    assertThat(moduleOptionsWithoutSuppressionRules.suppressionRules()).isEmpty();
    assertSelectedGroup(
        selector,
        moduleOptions,
        "2023-06-15",
        "early#1.0.0",
        "early",
        "early-rules",
        globalTransformation,
        earlyTransformation);
    assertSelectedGroup(
        selector,
        moduleOptions,
        "2025-06-15",
        "late#2.0.0",
        "late",
        "late-rules",
        globalTransformation,
        lateTransformation);
    assertSelectedGroup(
        selector,
        moduleOptions,
        null,
        "late#2.0.0",
        "late",
        "late-rules",
        globalTransformation,
        lateTransformation);
  }

  private static void assertSelectedGroup(
      ValidationPackageSelector selector,
      ValidationOptions moduleOptions,
      String birthDate,
      String expectedPackage,
      String expectedPackageGroup,
      String expectedTransformationId,
      MessageTransformation globalTransformation,
      MessageTransformation expectedTransformation) {
    final var resource =
        FhirResource.fromJson(
            """
          {"resourceType":"Patient","meta":{"profile":["http://example.org/fhir/StructureDefinition/Patient|1.0"]}%s}
          """
                .formatted(birthDate == null ? "" : ",\"birthDate\":\"%s\"".formatted(birthDate)));
    final var selection = selector.selectForResource(resource, FhirRelease.asR4(), moduleOptions);
    final var effectiveOptions =
        FhirValidatorCommand.withPackageGroupTransformations(
            moduleOptions, selection.messageTransformations());

    assertThat(selection.packages()).containsExactly(expectedPackage);
    assertThat(selection.packageGroup()).hasValue(expectedPackageGroup);
    assertThat(selection.messageTransformationIds()).containsExactly(expectedTransformationId);
    assertThat(effectiveOptions.messageTransformations())
        .containsExactly(globalTransformation, expectedTransformation);
  }

  private static MessageTransformation transformation(String messageId) {
    return new MessageTransformation("error", "warning", "locator", null, messageId);
  }
}
