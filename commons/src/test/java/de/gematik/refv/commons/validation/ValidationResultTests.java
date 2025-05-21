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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.commons.validation;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import de.gematik.refv.commons.helper.SingleValidationMessageFactory;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ValidationResultTests {

    @ParameterizedTest
    @MethodSource
    void testIsNotValid(ValidationResult result) {
        Assertions.assertFalse(result.isValid());
    }

    @Test
    void testIsValid() {
        var resultInfosAndWarnings = new ValidationResult(SingleValidationMessageFactory.getMixedValidationMessages().stream().filter(m -> m.getSeverity().equals(ResultSeverityEnum.INFORMATION) || m.getSeverity().equals(ResultSeverityEnum.WARNING)).collect(Collectors.toList()));
        Assertions.assertTrue(resultInfosAndWarnings.isValid());

        var resultEmpty = new ValidationResult( new LinkedList<>());
        Assertions.assertTrue(resultEmpty.isValid());
    }

    private static Stream<ValidationResult> testIsNotValid()  {
        var resultMixed = new ValidationResult(SingleValidationMessageFactory.getMixedValidationMessages());
        var resultWithErrorsOnly = new ValidationResult(SingleValidationMessageFactory.getMixedValidationMessages().stream().filter(m -> m.getSeverity().equals(ResultSeverityEnum.ERROR)).collect(Collectors.toList()));
        var resultWithFatalOnly = new ValidationResult(SingleValidationMessageFactory.getMixedValidationMessages().stream().filter(m -> m.getSeverity().equals(ResultSeverityEnum.ERROR)).collect(Collectors.toList()));

        return Stream.of(resultMixed, resultWithErrorsOnly, resultWithFatalOnly);
    }
}
