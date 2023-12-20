/*
 * Copyright (c) 2023 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.refv.commons.validation;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationResultOutputFilterTest {
    private ValidationResultOutputFilter filter;

    @BeforeEach
    public void setUp() {
        filter = new ValidationResultOutputFilter();
    }

    @Test
    void testApplyWithNoneFilter() {
        ValidationResult inputResult = new ValidationResult(Arrays.asList(
                createValidationMessage("Error message", ResultSeverityEnum.ERROR),
                createValidationMessage("Warning message", ResultSeverityEnum.WARNING),
                createValidationMessage("Info message", ResultSeverityEnum.WARNING)
        ));

        ValidationResult result = filter.apply(inputResult, ValidationMessagesFilter.KEEP_ALL);

        assertEquals(inputResult, result, "Result should remain unchanged with NONE filter");
    }

    @Test
    void testApplyWithErrorsAndWarningsOnlyFilter() {
        SingleValidationMessage fatalMessage = createValidationMessage("Fatal message", ResultSeverityEnum.FATAL);
        SingleValidationMessage errorMessage = createValidationMessage("Error message", ResultSeverityEnum.ERROR);
        SingleValidationMessage warningMessage = createValidationMessage("Warning message", ResultSeverityEnum.WARNING);
        SingleValidationMessage infoMessage = createValidationMessage("Info message", ResultSeverityEnum.INFORMATION);
        ValidationResult inputResult = new ValidationResult(Arrays.asList(
                fatalMessage,
                errorMessage,
                warningMessage,
                infoMessage
        ));

        ValidationResult result = filter.apply(inputResult, ValidationMessagesFilter.KEEP_ERRORS_AND_WARNINGS_ONLY);

        List<SingleValidationMessage> expectedMessages = Arrays.asList(
                fatalMessage,
                errorMessage,
                warningMessage
        );

        assertEquals(expectedMessages, result.getValidationMessages(), "Result should contain errors and warnings only");
    }

    @Test
    void testApplyWithErrorsOnlyFilter() {
        SingleValidationMessage fatalMessage = createValidationMessage("Fatal message", ResultSeverityEnum.FATAL);
        SingleValidationMessage errorMessage = createValidationMessage("Error message", ResultSeverityEnum.ERROR);
        SingleValidationMessage warningMessage = createValidationMessage("Warning message", ResultSeverityEnum.WARNING);
        SingleValidationMessage infoMessage = createValidationMessage("Info message", ResultSeverityEnum.INFORMATION);
        ValidationResult inputResult = new ValidationResult(Arrays.asList(
                fatalMessage,
                errorMessage,
                warningMessage,
                infoMessage
        ));

        ValidationResult result = filter.apply(inputResult, ValidationMessagesFilter.KEEP_ERRORS_ONLY);

        List<SingleValidationMessage> expectedMessages = Arrays.asList(
                fatalMessage,
                errorMessage
        );

        assertEquals(expectedMessages, result.getValidationMessages(), "Result should contain errors only");
    }

    private SingleValidationMessage createValidationMessage(String message, ResultSeverityEnum severity) {
        SingleValidationMessage m = new SingleValidationMessage();
        m.setMessage(message);
        m.setSeverity(severity);
        return m;
    }
}
