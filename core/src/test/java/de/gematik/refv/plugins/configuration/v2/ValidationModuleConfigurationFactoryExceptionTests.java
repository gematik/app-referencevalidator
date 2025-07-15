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
package de.gematik.refv.plugins.configuration.v2;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.plugins.configuration.CreationDateLocator;
import de.gematik.refv.plugins.configuration.DependencyList;
import de.gematik.refv.plugins.configuration.FhirPackage;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ValidationModuleConfigurationFactoryExceptionTests {

    private static final FhirContext fhirContext = FhirContext.forR4Cached();
    public static final String TEST_PROFILE =
            "http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate";

    @Test
    @SneakyThrows
    void testDifferentCreationDateLocatorsLeadToException() {
        var testPluginDefinition = createTestPluginDefinition();
        assertThatThrownBy(() -> ValidationModuleConfigurationFactory.createFrom(
                testPluginDefinition,
                fhirContext,
                ValidationModuleConfigurationFactoryExceptionTests::mockResourceLoadingFunction)).isInstanceOf(IllegalArgumentException.class);
    }

    private static PluginDefinition createTestPluginDefinition() {
        var dl1 =
                new DependencyList(
                        new FhirPackage("simplevalidationmodule.test#1.0.1"),
                        new LinkedList<>(),
                        new LinkedList<>(),
                        "2025-06-01",
                        "2025-06-31",
                        List.of(new CreationDateLocator(TEST_PROFILE, "Patient.birthDate")));
        var dl2 =
                new DependencyList(
                        new FhirPackage("simplevalidationmodule.test#2.0.0"),
                        new LinkedList<>(),
                        new LinkedList<>(),
                        "2025-07-01",
                        null,
                        List.of(new CreationDateLocator(TEST_PROFILE, "Bundle.timestamp")));
        return new PluginDefinition(
                "2.0.0",
                "test-plugin",
                "1.0.0",
                "author",
                "desc",
                "http://spec.example.org",
                new ValidationSection(List.of(dl1, dl2), false, false, List.of("xml"), List.of(), List.of()),
                null);
    }

    private static InputStream mockResourceLoadingFunction(String resourceName) {
        String path = "plugins/plugin-with-multiple-fhirpkg-lists/package/" + resourceName;
        return ValidationModuleConfigurationFactoryExceptionTests.class
                .getClassLoader()
                .getResourceAsStream(path);
    }
}
