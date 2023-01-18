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
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.gematik.refv.commons.configuration.ValidationMessageTransformation;
import org.hl7.fhir.utilities.i18n.I18nConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

class SeverityLevelTransformatorTests {

    private final SeverityLevelTransformer engine = new SeverityLevelTransformer();

    @Test
    void testTransformations() {
        var m1 = new SingleValidationMessage();
        m1.setSeverity(ResultSeverityEnum.ERROR);
        m1.setMessage("Error message");
        var m2 = new SingleValidationMessage();
        m2.setSeverity(ResultSeverityEnum.ERROR);
        m2.setMessage("Error message");
        var m3 = new SingleValidationMessage();
        m3.setSeverity(ResultSeverityEnum.WARNING);
        m3.setMessage("Warning message");
        var m4 = new SingleValidationMessage();
        m4.setSeverity(ResultSeverityEnum.INFORMATION);
        m4.setMessage("Warning message");
        var inputMessages = List.of(m1,m2,m3,m4);

        var t1 = new ValidationMessageTransformation("error","information","Error mes");
        var t2 = new ValidationMessageTransformation("warning","information","Warning mes");
        var notApplyingTransformation = new ValidationMessageTransformation("information","error","not applying");
        var transformations = List.of(t1,t2, notApplyingTransformation);

        var transformedMessages = engine.applyTransformations(inputMessages, transformations);

        Assertions.assertEquals(inputMessages.size(), transformedMessages.size());
        Assertions.assertTrue(transformedMessages.stream().allMatch(m -> m.getSeverity().equals(ResultSeverityEnum.INFORMATION)));
    }

    @Test
    void testNotFoundValueSetsRuleWorks() {
        var m1 = new SingleValidationMessage();
        m1.setSeverity(ResultSeverityEnum.WARNING);
        m1.setMessage("ValueSet https://fhir.kbv.de/ValueSet/KBV_VS_SFHIR_KBV_STATUSKENNZEICHEN not found by validator");
        m1.setMessageId(I18nConstants.TERMINOLOGY_TX_VALUESET_NOTFOUND);
        var inputMessages = List.of(m1);

        var transformedMessages = engine.applyTransformations(inputMessages, new LinkedList<>());
        Assertions.assertEquals(inputMessages.size(), transformedMessages.size());
        Assertions.assertTrue(transformedMessages.stream().allMatch(m -> m.getSeverity().equals(ResultSeverityEnum.ERROR)));
    }

    @Test
    void testWrongFullUrlRuleWorks() {
        var m1 = new SingleValidationMessage();
        m1.setSeverity(ResultSeverityEnum.WARNING);
        m1.setMessage("Entry 5 matches the reference Coverage/4cddf31d-5558-47f1-9bb4-a86329dcafa4 by type and id but it's fullUrl http://pvs.praxis.local/Coverage/4cddf31d-5558-47f1-9bb4-a86329dcafa4 does not match the full target URL http://pvs.praxis.local/Coverage/4cddf31d-5558-47f1-9bb4-a86329dcafa4 by Bundle resolution rules");
        m1.setMessageId(I18nConstants.BUNDLE_BUNDLE_POSSIBLE_MATCH_WRONG_FU);
        var inputMessages = List.of(m1);

        var transformedMessages = engine.applyTransformations(inputMessages, new LinkedList<>());
        Assertions.assertEquals(inputMessages.size(), transformedMessages.size());
        Assertions.assertTrue(transformedMessages.stream().allMatch(m -> m.getSeverity().equals(ResultSeverityEnum.ERROR)));
    }
}
