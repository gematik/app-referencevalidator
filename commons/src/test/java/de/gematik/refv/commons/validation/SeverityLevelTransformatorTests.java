/*
 * Copyright (c) 2022 gematik GmbH
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
import ca.uhn.fhir.validation.ValidationResult;
import de.gematik.refv.commons.configuration.ValidationMessageTransformation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class SeverityLevelTransformatorTests {

    private final SeverityLevelTransformator engine = new SeverityLevelTransformator();

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
}
