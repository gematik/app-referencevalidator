/*-
 * #%L
 * valmodule-erpta7
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.valmodule.erpta7.helper;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class DuplicateCheckerTests {

    private final DuplicateChecker duplicateChecker = new DuplicateChecker();

    @Test
    void testFindDuplicateFullUrls() {
        SingleValidationMessage validationMessage = new SingleValidationMessage();
        validationMessage.setMessage("'FullUrl must be unique in a bundle, or else entries with the same fullUrl must have different meta.versionId (except in history bundles)' Failed for fullUrl: " + "urn:uuid:dfdee8be-e6d3-42bb-ba30-a2a0addf8ee9");
        validationMessage.setSeverity(ResultSeverityEnum.ERROR);
        validationMessage.setMessageId("Rule bdl-7");

        String testResourceBody = "<Bundle>\n" +
                "    <entry>\n" +
                "        <fullUrl value=\"urn:uuid:dfdee8be-e6d3-42bb-ba30-a2a0addf8ee9\"/>\n" +
                "    </entry>\n" +
                "    <entry>\n" +
                "        <fullUrl value=\"urn:uuid:dfdee8be-e6d3-42bb-ba30-a2a0addf8ee9\"/>\n" +
                "    </entry>\n" +
                "</Bundle>";
        var messages = duplicateChecker.findDuplicateFullUrls(testResourceBody);

        assertThat(messages)
                .hasSize(1)
                .contains(validationMessage);
    }

    @Test
    @SneakyThrows
    void testNotFindDuplicateFullUrls() {
        String testResourceBody = "<Bundle>\n" +
                "    <entry>\n" +
                "        <fullUrl value=\"urn:uuid:dfdee8be-e6d3-42bb-ba30-a2a0addf8ee9\"/>\n" +
                "    </entry>\n" +
                "    <entry>\n" +
                "        <fullUrl value=\"urn:uuid:2f4b554e-af65-4c4e-adad-71381df65f21\"/>\n" +
                "    </entry>\n" +
                "</Bundle>";
        var messages = duplicateChecker.findDuplicateFullUrls(testResourceBody);
        assertThat(messages).isEmpty();
    }
}
