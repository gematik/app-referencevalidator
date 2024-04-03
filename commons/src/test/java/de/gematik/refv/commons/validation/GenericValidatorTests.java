/*
Copyright (c) 2022-2024 gematik GmbH

Licensed under the Apache License, Version 2.0 (the License);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package de.gematik.refv.commons.validation;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.configuration.ProfileConfiguration;
import de.gematik.refv.commons.configuration.SupportedProfileVersions;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class GenericValidatorTests {

    private GenericValidator genericValidator;

    @BeforeEach
    public void beforeEach() {
        genericValidator = new GenericValidator(
                FhirContext.forR4()
        );
    }

    @Test
    @SneakyThrows
    void testUnknownDependencyListThrowsException() {
        var configuration = new ValidationModuleConfiguration();
        configuration.getSupportedProfiles().put("https://bla.bla",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.2", new ProfileConfiguration(List.of("unknownDependencyList"), null));
        }}));
        var resourceProvider = new ConfigurationOnlyResourceProvider(configuration);
        var input = "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                + "    <meta>\n"
                + "        <profile value=\"https://bla.bla|1.0.2\"/>\n"
                + "        \n"
                + "    </meta>\n"
                + "</Bundle>";

        assertThatThrownBy(() -> genericValidator.validate(input, resourceProvider)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Some of the dependency lists are undefined: unknownDependencyList");
    }

    static class ConfigurationOnlyResourceProvider implements ValidationModuleResourceProvider{

        private final ValidationModuleConfiguration configuration;

        ConfigurationOnlyResourceProvider(ValidationModuleConfiguration configuration) {
            this.configuration = configuration;
        }
        @Override
        public InputStream getResource(String path) {
            return null;
        }

        @Override
        public InputStream getPackage(String path) {
            return null;
        }

        @Override
        public ValidationModuleConfiguration getConfiguration() {
            return configuration;
        }
    }
}
