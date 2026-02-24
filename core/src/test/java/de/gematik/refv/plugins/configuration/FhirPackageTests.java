/*-
 * #%L
 * referencevalidator-lib
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
package de.gematik.refv.plugins.configuration;


import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FhirPackageTests {

    @Test
    @SneakyThrows
    void testNameAndVersionStringIsParsedCorrectly() {
        String test = "test-dependency#1.0.0";
        String expected = "test-dependency-1.0.0.tgz";
        FhirPackage fhirPackage = new FhirPackage(test);

        assertThat(fhirPackage.toFilename()).as("Incorrect file name after parsing FHIR package name '%s'", test).isEqualTo(expected);
        assertThat(fhirPackage.toNameAndVersion()).as("Incorrect name-version-string after parsing of FHIR package '%s'", test).isEqualTo(test);
    }

    @Test
    @SneakyThrows
    void testInvalidNameAndVersionStringThrowsException() {
        String test = "test-dependency-1.0.0";
        assertThatThrownBy(() -> new FhirPackage(test)).isInstanceOf(MalformedPackageDeclarationException.class);
    }
}
