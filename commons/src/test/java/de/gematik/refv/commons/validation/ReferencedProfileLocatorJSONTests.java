/*
 * Copyright (c) 2024 gematik GmbH
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


import de.gematik.refv.commons.Profile;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.helper.ValidationModuleFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

class ReferencedProfileLocatorJSONTests {
    public static final String CODE = "simple";
    ReferencedProfileLocator locator = new ReferencedProfileLocator();
    ValidationModuleConfiguration configuration;

    static ValidationModule simpleValidationModule;

    @BeforeAll
    static void  beforeAll() {
        simpleValidationModule = ValidationModuleFactory.createInstance(CODE);
    }
    @ParameterizedTest
    @ValueSource(strings = {
            "https://bla.bla|1.0.2",
            "https://bla.bla"
    })
    void testProfileInCorrectResourceIsExtractedCorrectly(String canonical) {
        configuration = new ValidationModuleConfiguration();
        Profile profile = Profile.parse(canonical);
        String resource = String.format(
                "{\n"
                        + "\"resourceType\": \"Bundle\",\n"
                        + "\"id\": \"123456789\",\n"
                        + " \"meta\": {\n"
                        + "     \"profile\": [\n"
                        + "         \"%s\"\n"
                        + "    ],\n"
                        + "  }\n"
                        + "}", profile.getCanonical());
        Optional<Profile> profileOptional = locator.locate(resource, configuration);
        Assertions.assertTrue(profileOptional.isPresent());
        Assertions.assertEquals(profile, profileOptional.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Meta absent
            "{\n"
                    + "}",


            // Meta present, profile absent
            "{\n"
                    + " \"meta\": {\n"
                    + "     }\n"
                    + "}",


            // Profile present, value absent
            "{\n"
                    + " \"meta\": {\n"
                    + "     \"profile\": [\n"
                    + "    ]\n"
                    + "  }\n"
                    + "}",


            //Profile present, not a JsonArray, value empty
            "{\n"
                    + " \"meta\": {\n"
                    + "     \"profile\": \"\"\n"
                    + "  }\n"
                    + "}",


            // Profile present, value empty
            "{\n"
                    + " \"meta\": {\n"
                    + "     \"profile\": [\n"
                    + "         \"\"\n"
                    + "    ]\n"
                    + "  }\n"
                    + "}",
    })
    void testNoProfile(String resource) {
        Optional<Profile> profileOptional = locator.locate(resource, configuration);
        Assertions.assertTrue(profileOptional.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            //Meta field corrupted `}` missing
            "{\n"
                    + " \"meta\": {\n"
                    + "}",


            // Profile field corrupted `]` missing
            "{\n"
                    + " \"meta\": {\n"
                    + "     \"profile\": [\n"
                    + "  }\n"
                    + "}",
    })
    void corruptedJsonResource(String resource) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> locator.locate(resource, configuration));
    }

    @Test
    void testNotJsonResource() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> locator.locate("not a json resource", configuration));
    }

    @Test
    void testMultipleSupportedProfiles() {
        String resource =
                "{\n"
                        + "\"resourceType\": \"Bundle\",\n"
                        + "\"id\": \"123456789\",\n"
                        + " \"meta\": {\n"
                        + "     \"profile\": [\n"
                        + "         \"https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle\",\n"
                        + "         \"http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate\"\n"
                        + "    ],\n"
                        + "  }\n"
                        + "}";
        Assertions.assertDoesNotThrow(() -> locator.locate(resource, simpleValidationModule.getConfiguration()));
    }

    @Test
    void testMultipleSupportedProfilesFindsCorrectProfile() {
        String resource =
                "{\n"
                        + "\"resourceType\": \"Patient\",\n"
                        + "\"id\": \"123456789\",\n"
                        + " \"meta\": {\n"
                        + "     \"profile\": [\n"
                        + "         \"http://unknown.profile1.de\",\n"
                        + "         \"http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate\",\n"
                        + "         \"http://unknown.profile2.de\"\n"
                        + "    ],\n"
                        + "  }\n"
                        + "}";
        Optional<Profile> profileOptional = locator.locate(resource, simpleValidationModule.getConfiguration());
        profileOptional.ifPresent(profile -> Assertions.assertEquals("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate", profile.getCanonical()));
    }
}
