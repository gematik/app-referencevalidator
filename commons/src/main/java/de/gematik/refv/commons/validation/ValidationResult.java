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
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hl7.fhir.r4.model.OperationOutcome;

import java.util.Collection;

@AllArgsConstructor
@Data
public class ValidationResult {

    private ca.uhn.fhir.validation.ValidationResult intermediateHapiValidationOutcome;
    private Collection<SingleValidationMessage> filteredValidationMessages;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Valid: ");
        result.append(isValid());
        result.append(". Messages: \r\n");
        for (var item :
                getFilteredValidationMessages()) {
            result.append(item.getSeverity().getCode()).append(": ").append(item.getMessage()).append("\r\n");
        }
        return result.toString();
    }

    public boolean isValid() {
        return filteredValidationMessages.stream().noneMatch(m -> m.getSeverity() == ResultSeverityEnum.ERROR || m.getSeverity() == ResultSeverityEnum.FATAL);
    }
}
