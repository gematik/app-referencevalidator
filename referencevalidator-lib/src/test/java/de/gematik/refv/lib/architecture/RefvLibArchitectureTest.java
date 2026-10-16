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
package de.gematik.refv.lib.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architectural rules for the refv-lib module, enforcing the ADR "Architectural Testing for
 * Dependency Direction": the HL7 engine is isolated in adapter dependencies and never leaks into
 * the format, report, valmodule or snapshot dependencies.
 */
class RefvLibArchitectureTest {

  private static JavaClasses libClasses;

  @BeforeAll
  static void importClasses() {
    libClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(location -> !location.contains("test"))
            .importPackages("de.gematik.refv.lib");
  }

  @DisplayName(
      "The config_parser, fhir_context, report, valmodule, validation and snapshot dependencies must not depend on the HL7 implementation")
  @Test
  void hl7ShouldBeIsolatedInAdapterPackages() {
    final ArchRule rule =
        ArchRuleDefinition.noClasses()
            .that()
            .resideInAnyPackage(
                "de.gematik.refv.lib.config_parser.boundary..",
                "de.gematik.refv.lib.report.boundary..",
                "de.gematik.refv.lib.fhir_context.boundary..",
                "de.gematik.refv.lib.validation.boundary..",
                "de.gematik.refv.lib.valmodule.boundary..",
                "de.gematik.refv.lib.snapshot.boundary..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.hl7.fhir..", "ca.uhn.hapi..");
    rule.check(libClasses);
  }

  @DisplayName(
      "The format package must be self-contained (no dependency on other lib dependencies)")
  @Test
  void formatPackageShouldBeSelfContained() {
    final ArchRule rule =
        ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("de.gematik.refv.lib.config_parser..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.gematik.refv.lib.package_resolver..",
                "de.gematik.refv.lib.fhir_context..",
                "de.gematik.refv.lib.snapshot..",
                "de.gematik.refv.lib.validation..",
                "de.gematik.refv.lib.valmodule..");
    rule.check(libClasses);
  }

  @DisplayName("Domain value objects in 'fhir_context' should be records or sealed interfaces")
  @Test
  void fhircontextValueObjectsShouldBeRecordsOrSealed() {
    final ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("de.gematik.refv.lib.fhir_context..")
            .and()
            .areNotAnonymousClasses()
            .and()
            .areNotMemberClasses()
            .and()
            .haveSimpleNameNotContaining("ValidationModuleIndex")
            .and()
            .areNotEnums()
            .should()
            .beRecords()
            .orShould()
            .beInterfaces()
            .orShould()
            .bePackagePrivate();
    rule.check(libClasses);
  }

  @DisplayName("The 'valmodule' package must not depend on the snapshot package")
  @Test
  void valmoduleShouldNotDependOnSnapshot() {
    final ArchRule rule =
        ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("de.gematik.refv.lib.valmodule..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("de.gematik.refv.lib.snapshot..");
    rule.check(libClasses);
  }
}
