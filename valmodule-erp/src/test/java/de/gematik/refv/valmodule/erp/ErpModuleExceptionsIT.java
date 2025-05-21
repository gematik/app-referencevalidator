/*-
 * #%L
 * valmodule-erp
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
package de.gematik.refv.valmodule.erp;

import de.gematik.refv.commons.exceptions.UnsupportedProfileException;
import de.gematik.refv.commons.validation.IntegratedValidationModule;
import de.gematik.refv.valmodule.base.helper.TestConfigurationBasedValidationModuleFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ErpModuleExceptionsIT {
    private final IntegratedValidationModule validationModule = TestConfigurationBasedValidationModuleFactory.createInstance("erp");

    @ParameterizedTest
    @ValueSource(strings = {
            // Unknown profile
            "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                    + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                    + "    <meta>\n"
                    + "        <profile value=\"https://bla.bla|1.0.2\"/>\n"
                    + "        \n"
                    + "    </meta>\n"
                    + "</Bundle>",

            // Profile without Version
            "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                    + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                    + "    <meta>\n"
                    + "        <profile value=\"https://fhir.kbv.de/StructureDefinition/KBV_PR_ERP_Bundle\"/>\n"
                    + "        \n"
                    + "    </meta>\n"
                    + "</Bundle>"

    })
    void testUnsupportedProfileLeadsToException(String resource) {
        Assertions.assertThrows(UnsupportedProfileException.class, () ->
            validationModule.validateString(resource));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // No profile given
            "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                    + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                    + "</Bundle>"
    })
    void testInvalidResourcesLeadToIllegalArgumentException(String resource) {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                validationModule.validateString(resource));
    }
}
