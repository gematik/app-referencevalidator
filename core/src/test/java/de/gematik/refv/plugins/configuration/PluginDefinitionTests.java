/*-
 * #%L
 * referencevalidator-lib
 * %%
 * Copyright (C) 2025 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.plugins.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class PluginDefinitionTests {


    @Test
    @SneakyThrows
    void testGetValidationDependenciesAsFilenames() {
        String test = "test-dependency-1.0.0.tgz";
        PluginDefinition pluginDefinition = new PluginDefinition();
        PluginDefinitionValidationSection validationSection = new PluginDefinitionValidationSection();
        validationSection.setDependencies(List.of("test-dependency#1.0.0"));
        pluginDefinition.setValidation(validationSection);
        List<String> validationDependenciesAsFilenames = pluginDefinition.getValidationDependenciesAsFilenames();
        assertThat(validationDependenciesAsFilenames).as("Dependency %s not found", test).contains(test);
    }

    @Test
    @SneakyThrows
    void testGetValidationDependenciesAsFilenamesThrowsMalformedPackageDeclarationException() {
        PluginDefinition pluginDefinition = new PluginDefinition();
        PluginDefinitionValidationSection validationSection = new PluginDefinitionValidationSection();
        validationSection.setDependencies(List.of("test-dependency-1.0.0"));
        pluginDefinition.setValidation(validationSection);
        assertThatThrownBy(() -> pluginDefinition.getValidationDependenciesAsFilenames()).isInstanceOf(MalformedPackageDeclarationException.class);
    }

    @Test
    @SneakyThrows
    void getValidationFhirPackageAsFilename() {
        String test = "test-dependency-1.0.0.tgz";
        PluginDefinition pluginDefinition = new PluginDefinition();
        PluginDefinitionValidationSection validationSection = new PluginDefinitionValidationSection();
        validationSection.setFhirPackage("test-dependency#1.0.0");
        pluginDefinition.setValidation(validationSection);
        assertThat(pluginDefinition.getValidationFhirPackageAsFilename()).as("Incorrect validation FHIR package %s", test).isEqualTo(test);
    }

    @Test
    @SneakyThrows
    void getValidationFhirPackageAsFilenameThrowsMalformedPackageDeclarationException() {
        PluginDefinition pluginDefinition = new PluginDefinition();
        PluginDefinitionValidationSection validationSection = new PluginDefinitionValidationSection();
        validationSection.setFhirPackage("test-dependency-1.0.0");
        pluginDefinition.setValidation(validationSection);
        assertThatThrownBy(() -> pluginDefinition.getValidationFhirPackageAsFilename()).isInstanceOf(MalformedPackageDeclarationException.class);
    }
}
