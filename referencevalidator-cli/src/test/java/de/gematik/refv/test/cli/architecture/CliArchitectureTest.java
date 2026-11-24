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
package de.gematik.refv.test.cli.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architectural rules for the CLI (boundary) module, enforcing the ADR "Architecture Design with
 * Boundary-Control-Entity (BCE) Pattern": the boundary isolates protocol concerns and must not
 * bypass the control layer to reach the HL7 engine directly.
 */
class CliArchitectureTest {

  private static JavaClasses cliClasses;

  @BeforeAll
  static void importClasses() {
    cliClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(location -> !location.contains("test"))
            .importPackages("de.gematik.refv.cli");
  }

  @DisplayName("The CLI boundary must not depend on the HL7 implementation directly")
  @Test
  void cliShouldNotDependOnHl7Directly() {
    final ArchRule rule =
        ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("de.gematik.refv.cli..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.hl7.fhir..", "ca.uhn.hapi..");
    rule.check(cliClasses);
  }

  @DisplayName(
      "The CLI report package must only depend on the public config/context API, not on adapters")
  @Test
  void reportPackageShouldNotDependOnOtherClasses() {
    final ArchRule rule =
        ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("de.gematik.refv.cli.report..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.gematik.refv.lib.snapshot..", "de.gematik.refv.lib.package_resolver..");
    rule.check(cliClasses);
  }

  @DisplayName("Commands must reside in the commands package and extend BaseCommand")
  @Test
  void commandsShouldExtendBaseCommand() {
    final ArchRule rule =
        ArchRuleDefinition.classes()
            .that()
            .resideInAPackage("de.gematik.refv.cli.commands..")
            .and()
            .haveSimpleNameEndingWith("Command")
            .should()
            .beAssignableTo("de.gematik.refv.cli.BaseCommand");
    rule.check(cliClasses);
  }
}
