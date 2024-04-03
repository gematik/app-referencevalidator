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
import ca.uhn.fhir.parser.DataFormatException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TimeZone;

class ResourceCreationDateLocatorTests {
    ResourceCreationDateLocator locator = new ResourceCreationDateLocator(FhirContext.forR4());

    static TimeZone systemTimeZone;
    @BeforeAll
    static void beforeAll() {
        systemTimeZone = TimeZone.getDefault();
        // Simulate reference validator running on a misconfigured machine.
        // This is potentially dangerous, because the setting may influence other tests running in parallel, but no better solution has been found yet
        TimeZone.setDefault(TimeZone.getTimeZone("America/Thule"));
    }

    @AfterAll
    static void afterAll() {
        TimeZone.setDefault(systemTimeZone);
    }

    @Test
    void testDateFormatIsSupported() {
        String resource =
                "<Patient xmlns=\"http://hl7.org/fhir\">\n"
                        + "    <birthDate value=\"1980-01-01\" />\n"
                        + "</Patient>";
        var result = locator.findCreationDateIn(resource, "birthDate");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(LocalDate.of(1980, 01,01), result.get());
    }

    @Test
    void testDateTimeFormatIsSupported() {
        String resource =
                "<Patient xmlns=\"http://hl7.org/fhir\">\n"
                        + "    <birthDate value=\"1980-01-01T10:00:00+05:00\" />\n"
                        + "</Patient>";
        var result = locator.findCreationDateIn(resource, "birthDate");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(LocalDate.of(1980, 01,01), result.get());
    }

    @Test
    void testDateTimeIsConvertedToGermanTimeZone() {
        String resource =
                "<Patient xmlns=\"http://hl7.org/fhir\">\n"
                        + "    <birthDate value=\"2023-06-30T22:00:00+00:00\" />\n" // corresponds to 2023-07-01 in Germany (UTC+1 in winter or UTC+2 in summer)
                        + "</Patient>";

        var result = locator.findCreationDateIn(resource, "birthDate");

        Assertions.assertTrue(result.isPresent());
        LocalDate expectedGermanDate = LocalDate.of(2023, 07, 01);
        Assertions.assertEquals(expectedGermanDate, result.get());
    }

    @Test
    void testMissingDateElementIsSupported() {
        System.out.println(ZoneId.systemDefault());

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
