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

import de.gematik.refv.commons.Profile;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.helper.ValidationModuleFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

class ReferencedProfileLocatorXMLTests {

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
    void testProfileInCorrectResourceIsExtractedCorrectly(String canonical){
        configuration = new ValidationModuleConfiguration();
        Profile profile = Profile.parse(canonical);
        String resource =String.format(
                "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                        + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                        + "    <meta>\n"
                        + "        <versionId value=\"v2\"/>"
                        + "        <profile value=\"%s\"/>\n"
                        + "        \n"
                        + "    </meta>\n"
                        + "</Bundle>", profile.getCanonical());
        Optional<Profile> profileOptional = locator.locate(resource, configuration);
        Assertions.assertTrue(profileOptional.isPresent());
        Assertions.assertEquals(profile, profileOptional.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Meta absent
            "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                    + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                    + "</Bundle>",

            // Meta present, profile absent
            "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                    + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                    + "    <meta></meta>\n"
                    + "</Bundle>",

            // Profile present, value absent
            "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                    + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                    + "    <meta>\n"
                    + "        <profile/>\n"
                    + "        \n"
                    + "    </meta>\n"
                    + "</Bundle>",


            // Profile present, value empty
            "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                    + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                    + "    <meta>\n"
                    + "        <profile value=\"\"/>\n"
                    + "        \n"
                    + "    </meta>\n"
                    + "</Bundle>",
    })
    void testNoProfile(String resource) {
        Optional<Profile> profileOptional = locator.locate(resource, configuration);
        Assertions.assertTrue(profileOptional.isEmpty());
    }

    @Test
    void testNotXmlResource() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> locator.locate("not an xml resource", configuration));
    }

    @Test
    void testMultipleSupportedProfiles() {
        String resource =
                "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                        + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                        + "    <meta>\n"
                        + "        <versionId value=\"v2\"/>"
                        + "        <profile value=\"https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle\"/>\n"
                        + "        <profile value=\"http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate\"/>\n"
                        + "        \n"
                        + "    </meta>\n"
                        + "</Bundle>";
        Assertions.assertDoesNotThrow(() -> locator.locate(resource, simpleValidationModule.getConfiguration()));
    }

    @Test
    void testMultipleSupportedProfilesFindsCorrectProfile() {
        String resource =
                "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                        + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                        + "    <meta>\n"
                        + "        <versionId value=\"v2\"/>"
                        + "        <profile value=\"http://unknown.profile1.de\"/>\n"
                        + "        <profile value=\"http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate\"/>\n"
                        + "        <profile value=\"http://unknown.profile2.de\"/>\n"
                        + "        \n"
                        + "    </meta>\n"
                        + "</Bundle>";
        Optional<Profile> profileOptional = locator.locate(resource, simpleValidationModule.getConfiguration());
        profileOptional.ifPresent(profile -> Assertions.assertEquals("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate", profile.getCanonical()));
    }
}
