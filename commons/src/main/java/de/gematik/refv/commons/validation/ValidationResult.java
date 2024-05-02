/*
Copyright (c) 2022-2024 gematik GmbH

Licensed under the Apache License, Version 2.0 (the License);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package de.gematik.refv.commons.validation;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class ValidationResult {
    @Getter
    private final Collection<SingleValidationMessage> validationMessages;
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Valid: ");
        result.append(isValid());
        if(!getValidationMessages().isEmpty()) {
            result.append(". Messages: \r\n");
            for (var item :
                    getValidationMessages()) {
                result.append(item);
                result.append("\r\n");
            }
        }
        return result.toString();
    }

    public boolean isValid() {
        return getValidationMessages().stream().noneMatch(m -> m.getSeverity() == ResultSeverityEnum.ERROR || m.getSeverity() == ResultSeverityEnum.FATAL);
    }

    public static ValidationResult createInstance(ResultSeverityEnum severity, String messageId, String diagnostics) {
        SingleValidationMessage m = new SingleValidationMessage();
        m.setSeverity(severity);
        m.setMessageId(messageId);
        m.setMessage(diagnostics);
        return new ValidationResult(List.of(m));
    }
}
