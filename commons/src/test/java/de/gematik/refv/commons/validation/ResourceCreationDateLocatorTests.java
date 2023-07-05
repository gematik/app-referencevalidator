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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class ResourceCreationDateLocatorTests {
    ResourceCreationDateLocator locator = new ResourceCreationDateLocator(FhirContext.forR4());

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
