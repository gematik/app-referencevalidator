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
package de.gematik.refv.commons.validation;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import java.time.LocalDate;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Execution(ExecutionMode.SAME_THREAD)
class ResourceCreationDateLocatorTests {
    ResourceCreationDateLocator locator = new ResourceCreationDateLocator(FhirContext.forR4());

    static TimeZone systemTimeZone;
    @BeforeAll
    static void beforeAll() {
        systemTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void afterEach() {
        TimeZone.setDefault(systemTimeZone);
    }

    @CsvSource({
            "Asia/Taipei,1980-01-01,1980,01,01", // Asia/Taipei is UTC+8 in winter
            "Europe/Berlin,1980-01-01,1980,01,01", // Europe/Berlin is UTC+1 in winter
            "America/Nome,1980-01-01,1980,01,01", // America/Nome is UTC-9 in winter

            "Asia/Taipei,1980-01,1980,01,01",
            "Europe/Berlin,1980-01,1980,01,01",
            "America/Nome,1980-01,1980,01,01",

            "Asia/Taipei,1980,1980,01,01",
            "Europe/Berlin,1980,1980,01,01",
            "America/Nome,1980,1980,01,01",

            "Asia/Taipei,1980-01-01T10:00:00+05:00,1980,01,01",
            "Europe/Berlin,1980-01-01T10:00:00+05:00,1980,01,01",
            "America/Nome,1980-01-01T10:00:00+05:00,1980,01,01",

            "Asia/Taipei,1980-01-01T00:00:00+01:00,1980,01,01",
            "Europe/Berlin,1980-01-01T00:00:00+01:00,1980,01,01",
            "America/Nome,1980-01-01T00:00:00+01:00,1980,01,01",

            "Asia/Taipei,1980-01-01T00:00:00,1980,01,01",
            "Europe/Berlin,1980-01-01T00:00:00,1980,01,01",
            "America/Nome,1980-01-01T00:00:00,1980,01,01",

            "Asia/Taipei,2023-06-30T22:00:00+00:00,2023,07,01",
            "Europe/Berlin,2023-06-30T22:00:00+00:00,2023,07,01",
            "America/Nome,2023-06-30T22:00:00+00:00,2023,07,01",
    }
    )
    @ParameterizedTest
    void testCreationDateIsInterpretedInGermanTimeIndependentlyOfLocalMachineTimezone(String timezone, String date, int year, int month, int day) {
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));

        String resource = String.format(
                "<Patient xmlns=\"http://hl7.org/fhir\">\n"
                        + "    <birthDate value=\"%s\" />\n"
                        + "</Patient>", date);
        var result = locator.findCreationDateIn(resource, "birthDate");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(LocalDate.of(year, month,day), result.get());
    }

    @Test
    void testMissingDateElementIsSupported() {
        String resource =
                "<Patient xmlns=\"http://hl7.org/fhir\">\n"
                        + "</Patient>";
        var result = locator.findCreationDateIn(resource, "birthDate");
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testInvalidFormatLeadsToException() {
        String resource =
                "<Patient xmlns=\"http://hl7.org/fhir\">\n"
                        + "    <birthDate value=\"1980-01-01Z\" />\n"
                        + "</Patient>";
        Assertions.assertThrows(DataFormatException.class, () -> locator.findCreationDateIn(resource, "birthDate"));
    }

    @Test
    void testMultipleValuesLeadToException() {
        String resource =
                "<Bundle><entry><resource>" +
                "<Patient xmlns=\"http://hl7.org/fhir\">\n"
                        + "    <birthDate value=\"1980-01-01\" />\n"
                        + "</Patient>" +
                        "</resource></entry><entry><resource>" +
                        "<Patient xmlns=\"http://hl7.org/fhir\">\n"
                        + "    <birthDate value=\"1981-01-01\" />\n"
                        + "</Patient>" +
                        "</resource></entry></Bundle>";

        Assertions.assertThrows(IllegalArgumentException.class, () -> locator.findCreationDateIn(resource, "entry.resource.birthDate"));
    }
}
