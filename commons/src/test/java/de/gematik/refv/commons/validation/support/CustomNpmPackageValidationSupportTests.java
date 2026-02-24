/*-
 * #%L
 * commons
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
package de.gematik.refv.commons.validation.support;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.Profile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
class CustomNpmPackageValidationSupportTests {

    private static CustomNpmPackageValidationSupport validationSupport;

    @BeforeAll
    @SneakyThrows
    static void beforeAll() {
        String packagePath = "fhir-package-1.0.0.tgz";
        Function<String, InputStream> packageInputStreamProvider = path -> {
            try {
                return new ByteArrayInputStream(Objects.requireNonNull(CustomNpmPackageValidationSupportTests.class.getClassLoader().getResourceAsStream(path)).readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        validationSupport = CustomNpmPackageValidationSupport.create(FhirContext.forR4(), packagePath, packageInputStreamProvider);
    }

    @Test
    void testFetchStructureDefinition() {
        assertThat(validationSupport.fetchStructureDefinition("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate")).isNotNull();
        assertThat(validationSupport.fetchStructureDefinition("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate|1.0.0")).isNotNull();

        assertThat(validationSupport.fetchStructureDefinition("http://unknown-structure-definition")).isNull();
        assertThat(validationSupport.fetchStructureDefinition("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate|2.0.0")).isNull();
    }

    @Test
    void testFetchStructureDefinitionWithCodeSystemUrl() {
        assertThat(validationSupport.fetchStructureDefinition("https://test.gematik.de/fhir/isik/v3/CodeSystem/test-category-codes")).as("Erronous Request for a CodeSystem didn't result in a null response").isNull();
    }

    @Test
    void testFetchCodeSystem() {
        assertThat(validationSupport.fetchCodeSystem("https://test.gematik.de/fhir/isik/v3/CodeSystem/test-category-codes")).isNotNull();
        assertThat(validationSupport.fetchCodeSystem("https://test.gematik.de/fhir/isik/v3/CodeSystem/test-category-codes|1.0.0")).isNotNull();
        assertThat(validationSupport.isCodeSystemSupported(null,"https://test.gematik.de/fhir/isik/v3/CodeSystem/test-category-codes")).isTrue();
        assertThat(validationSupport.isCodeSystemSupported(null,"https://test.gematik.de/fhir/isik/v3/CodeSystem/test-category-codes|1.0.0")).isTrue();

        assertThat(validationSupport.fetchCodeSystem("http://unknown-code-system")).isNull();
        assertThat(validationSupport.fetchCodeSystem("https://test.gematik.de/fhir/isik/v3/CodeSystem/test-category-codes|2.0.0")).isNull();
        assertThat(validationSupport.isCodeSystemSupported(null,"http://unknown-code-system")).isFalse();
    }

    @Test
    void testFetchValueSet() {
        assertThat(validationSupport.fetchValueSet("http://example.org/fhir/ValueSet/TestValueSet")).isNotNull();
        assertThat(validationSupport.fetchValueSet("http://example.org/fhir/ValueSet/TestValueSet|1.0.0")).isNotNull();
        assertThat(validationSupport.isValueSetSupported(null,"http://example.org/fhir/ValueSet/TestValueSet")).isTrue();
        assertThat(validationSupport.isValueSetSupported(null,"http://example.org/fhir/ValueSet/TestValueSet|1.0.0")).isTrue();

        assertThat(validationSupport.fetchValueSet("http://unknown-value-set")).isNull();
        assertThat(validationSupport.fetchValueSet("http://example.org/fhir/ValueSet/TestValueSet|2.0.0")).isNull();
        assertThat(validationSupport.isValueSetSupported(null,"http://unknown-value-set")).isFalse();
    }

    @Test
    void testGetAllProfiles() {
        var allProfiles = validationSupport.getAllProfiles();
        assertThat(allProfiles)
                .hasSize(1)
                .contains(Profile.parse("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate|1.0.0"));
    }
}
