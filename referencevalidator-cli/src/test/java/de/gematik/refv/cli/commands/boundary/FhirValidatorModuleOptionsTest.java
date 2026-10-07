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
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.valmodule.boundary.ModuleLoader;
import java.nio.file.Path;
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
}
