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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.plugins.configuration.v1;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.configuration.ValidationMessageTransformation;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.plugins.configuration.FhirPackage;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidationModuleConfigurationFactoryTests {

    public static final String TEST_PROFILE =
            "http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate";
    private static final FhirContext fhirContext = FhirContext.forR4Cached();
    private static de.gematik.refv.plugins.configuration.v1.PluginDefinition testPluginDefinition;
    private static ValidationModuleConfiguration config;

    @BeforeAll
    @SneakyThrows
    public static void beforeAll() {
        testPluginDefinition = createTestPluginDefinition();
        config =
                ValidationModuleConfigurationFactory.createFrom(
                        testPluginDefinition,
                        fhirContext,
                        ValidationModuleConfigurationFactoryTests::mockResourceLoadingFunction);
    }

    private static de.gematik.refv.plugins.configuration.v1.PluginDefinition createTestPluginDefinition() {
        return new PluginDefinition(
                "2.0.0",
                "test-plugin",
                "1.0.0",
                "author",
                "desc",
                "http://spec.example.org",
                new ValidationSection(new FhirPackage("simplevalidationmodule.test#1.0.1"),
                        List.of(
                                new FhirPackage("test-package-11-dep#0.1.1"),
                                new FhirPackage("test-package-12-dep#0.1.2")),
                        false, false, List.of("xml"),
                        List.of(
                                new ValidationMessageTransformation(
                                        "ERROR", "INFO", "locator-string", "MESSID", null)),
                        List.of("http://cs.example.org"), List.of("http://vs.example.org")),
                null);
    }

    private static InputStream mockResourceLoadingFunction(String resourceName) {
        String path = "plugins/plugin-with-multiple-fhirpkg-lists/package/" + resourceName;
        return ValidationModuleConfigurationFactoryTests.class
                .getClassLoader()
                .getResourceAsStream(path);
    }

    @Test
    @SneakyThrows
    void testValidConfiguration() {
        assertThat(config.getId()).isEqualTo(testPluginDefinition.getId());
        assertThat(config.getVersion()).isEqualTo(testPluginDefinition.getVersion());
        assertThat(config.getAcceptedEncodings())
                .isEqualTo(testPluginDefinition.getValidation().getAcceptedEncodings());
        assertThat(config.getIgnoredCodeSystems())
                .isEqualTo(testPluginDefinition.getValidation().getIgnoredCodeSystems());
        assertThat(config.getIgnoredValueSets())
                .isEqualTo(testPluginDefinition.getValidation().getIgnoredValueSets());
    }

    @Test
    void testSupportedProfilesArePopulatedCorrectly() {
        assertThat(config.getSupportedProfiles()).isNotEmpty();
        assertThat(config.getSupportedProfiles()).containsKey(TEST_PROFILE);
        var sp1 = config.getSupportedProfiles().get(TEST_PROFILE);
        assertThat(sp1.getProfileVersions()).isNotEmpty();
        assertThat(sp1.getProfileVersions()).containsKey("1.0.0");
        assertThat(sp1.getProfileVersions()).containsKey("0.0.0");
        assertThat(sp1.getProfileVersions().get("1.0.0").getCreationDateLocator())
                .isNull();
    }

    @Test
    void testDependenciesArePopulatedCorrectly() {
        assertThat(config.getDependencyLists()).isNotEmpty();
        assertThat(config.getDependencyLists()).containsKey("simplevalidationmodule.test#1.0.1");
        var dl1 = config.getDependencyLists().get("simplevalidationmodule.test#1.0.1");
        assertThat(dl1.getPackages())
                .isEqualTo(
                        List.of(
                                "simplevalidationmodule.test-1.0.1.tgz",
                                "test-package-11-dep-0.1.1.tgz",
                                "test-package-12-dep-0.1.2.tgz"));
        assertThat(dl1.getValidationMessageTransformations())
                .isEqualTo(
                        testPluginDefinition
                                .getValidation()
                                .getValidationMessageTransformations());
    }
}
