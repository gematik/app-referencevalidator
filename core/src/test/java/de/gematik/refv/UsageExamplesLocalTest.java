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

package de.gematik.refv;

import de.gematik.refv.commons.validation.ProfileValidityPeriodCheckStrategy;
import de.gematik.refv.commons.validation.ValidationMessagesFilter;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.commons.validation.ValidationOptions;
import de.gematik.refv.commons.validation.ValidationResult;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.zip.ZipFile;

/**
 * Usage examples used in README.md
 */
class UsageExamplesLocalTest {

    @SneakyThrows
    @Test
    void validateFile() {
        ValidationModule erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERP);
        ValidationResult result = erpModule.validateFile("src/test/resources/SimpleMedicationRequest.xml");
        System.out.println(result.isValid());
        System.out.println(result.getValidationMessages());
    }

    @SneakyThrows
    @Test
    void validateString() {
        ValidationModule erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERP);
        String fhirRessource = "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                + "    <meta>\n"
                + "        <profile value=\"https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle|1.0.1\"/>\n"
                + "        \n"
                + "    </meta>\n"
                + "</Bundle>";
        ValidationResult result = erpModule.validateString(fhirRessource);
        System.out.println(result.isValid());
        System.out.println(result.getValidationMessages());
    }

    @SneakyThrows
    @Test
    void setOptions() {
        ValidationModule erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERP);
        ValidationOptions options = ValidationOptions.getDefaults();
        options.setProfileValidityPeriodCheckStrategy(ProfileValidityPeriodCheckStrategy.IGNORE);
        options.setProfiles(List.of("https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle|1.0.1"));
        options.setAcceptedEncodings(List.of("xml", "json"));
        options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
        ValidationResult result = erpModule.validateFile("src/test/resources/SimpleMedicationRequest.xml", options);
        System.out.println(result.isValid());
        System.out.println(result.getValidationMessages());
    }

    @SneakyThrows
    @Test
    void validateUsingPlugin() {
        Plugin plugin = Plugin.createFromZipFile(new ZipFile("src/test/resources/plugins/minimal-plugin.zip"));
        var pluginModule = new ValidationModuleFactory().createValidationModuleFromPlugin(plugin);
        String fhirRessource = "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                + "    <meta>\n"
                + "        <profile value=\"https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle|1.0.1\"/>\n"
                + "        \n"
                + "    </meta>\n"
                + "</Bundle>";
        var result = pluginModule.validateString(fhirRessource);
        System.out.println(result.isValid());
    }
}
