/*-
 * #%L
 * commons
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
package de.gematik.refv.commons.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.helper.SingleValidationMessageFactory;
import org.junit.jupiter.api.Test;

class ValidationResultToOperationOutcomeConverterTest {

    private final static FhirContext fhirContext;
    private final static ValidationResultToOperationOutcomeConverter converter;

    static {
        fhirContext = FhirContext.forR4();
        converter = new ValidationResultToOperationOutcomeConverter(fhirContext);
    }

    @Test
    void testToOperationOutcomeWithErrors() {
        var mixedValidationMessages = SingleValidationMessageFactory.getMixedValidationMessages();
        var resultAllMessageTypes = new ValidationResult(mixedValidationMessages);

        var returnedOperationOutcome = converter.toOperationOutcome(resultAllMessageTypes);

        var expectedOperationOutcome = fhirContext.newJsonParser().parseResource(ClassLoader.getSystemClassLoader().getResourceAsStream("operationoutcome-invalid.json"));
        assertThat(expectedOperationOutcome).usingRecursiveComparison().ignoringFields("userData").isEqualTo(returnedOperationOutcome);
    }
}
