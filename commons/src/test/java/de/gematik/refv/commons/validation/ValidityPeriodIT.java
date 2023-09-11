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
import de.gematik.refv.commons.helper.SimpleValidationModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Execution(ExecutionMode.CONCURRENT)
class ValidityPeriodIT {

    private static SimpleValidationModule validationModule;

    @BeforeAll
    @SneakyThrows
    static void beforeAll() {
        validationModule = SimpleValidationModule.createInstance("simple-packages.yaml");
        validationModule.initialize();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/test/resources/date-validity/valid/patient-within-first-part-of-validity-period.xml",
            "src/test/resources/date-validity/valid/patient-with-last-day-of-validity-period.xml",
            "src/test/resources/date-validity/valid/patient-within-second-part-of-validity-period.xml",
            "src/test/resources/date-validity/valid/patient-with-first-day-of-validity-period.xml",
            "src/test/resources/date-validity/valid/patient-within-first-part-of-validity-period-birthdate-without-day.xml"
    })
    @SneakyThrows
    void testValidScenarios(String path) {
        var result = validationModule.validateFile(path);
        Assertions.assertTrue(result.isValid(), result.toString());
    }

    @Test
    @SneakyThrows
    void testResourceWithCreationDateAfterValidityPeriodIsValidIfValidationOFTimePeriodIsDisabled() {
        var validationOptions = ValidationOptions.getDefaults();
        validationOptions.setProfileValidityPeriodCheckStrategy(ProfileValidityPeriodCheckStrategy.IGNORE);
        var result = validationModule.validateFile("src/test/resources/date-validity/invalid/patient-after-validity-period-finished.xml", validationOptions);
        Assertions.assertTrue(result.isValid(), result.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/test/resources/date-validity/invalid/patient-without-creation-date.xml",
            "src/test/resources/date-validity/invalid/patient-after-validity-period-finished.xml",
            "src/test/resources/date-validity/invalid/patient-before-validity-period-started.xml",
            "src/test/resources/date-validity/invalid/patient-within-second-part-of-validity-period-but-without-gender.xml",
            "src/test/resources/date-validity/invalid/patient-with-incorrect-birthdate.xml"
    })
    @SneakyThrows
    void testInvalidScenarios(String path) {
        var result = validationModule.validateFile(path);
        Assertions.assertFalse(result.isValid(), result.toString());
    }

    @Test
    @SneakyThrows
    void testResourceWithoutCreationDateIsInValidEvenIfValidationOFTimePeriodIsDisabled() {
        var validationOptions = ValidationOptions.getDefaults();
        validationOptions.setProfileValidityPeriodCheckStrategy(ProfileValidityPeriodCheckStrategy.IGNORE);
        var result = validationModule.validateFile("src/test/resources/date-validity/invalid/patient-without-creation-date.xml");
        Assertions.assertFalse(result.isValid(), result.toString());
    }

    @Test
    @SneakyThrows
    void testLatestDependencyListIsAppliedIfValidationOfTimePeriodIsDisabled() {
        var validationOptions = ValidationOptions.getDefaults();
        validationOptions.setProfileValidityPeriodCheckStrategy(ProfileValidityPeriodCheckStrategy.IGNORE);
        validationOptions.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
        var result = validationModule.validateFile("src/test/resources/date-validity/invalid/patient-before-validity-period-started.xml", validationOptions);
        Assertions.assertFalse(result.isValid(), result.toString());
        var warningExists = result.getValidationMessages().stream().anyMatch(m -> m.getSeverity().equals(ResultSeverityEnum.WARNING) && m.getMessageId().equals(GenericValidator.WARN_REFV_VALIDITY_PERIOD_CHECK_DISABLED));
        Assertions.assertTrue(warningExists, "No warning has been issued about deactivated validity period check");
    }

}
