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
import de.gematik.refv.commons.configuration.ValidationMessageTransformation;
import lombok.NonNull;

import java.util.LinkedList;
import java.util.List;

public class SeverityLevelTransformator {

    public List<SingleValidationMessage> applyTransformations(
            List<SingleValidationMessage> input,
            @NonNull
            List<ValidationMessageTransformation> transformations
    ) {
        LinkedList<SingleValidationMessage> filteredMessages = new LinkedList<>();
        for (SingleValidationMessage message:
                input) {

            var transformation = transformations.stream().filter(t -> t.getSeverityLevelFrom().equals(message.getSeverity().getCode()) &&
                    message.getMessage().contains(t.getLocatorString())
            ).findFirst();
            if(transformation.isEmpty()) {
                filteredMessages.add(message);
                continue;
            }

            message.setSeverity(ResultSeverityEnum.fromCode(transformation.get().getSeverityLevelTo()));
            filteredMessages.add(message);
        }
        return filteredMessages;
    }
}
