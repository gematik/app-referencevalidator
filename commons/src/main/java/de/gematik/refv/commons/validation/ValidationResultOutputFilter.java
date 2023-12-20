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

import java.util.stream.Collectors;

class ValidationResultOutputFilter {
    public ValidationResult apply(ValidationResult result, ValidationMessagesFilter validationMessagesFilter) {
        if(validationMessagesFilter == ValidationMessagesFilter.KEEP_ALL)
            return result;

        if(validationMessagesFilter == ValidationMessagesFilter.KEEP_ERRORS_AND_WARNINGS_ONLY) {
            var filteredMessages = result.getValidationMessages().stream().filter(m -> m.getSeverity() == ResultSeverityEnum.ERROR || m.getSeverity() == ResultSeverityEnum.FATAL || m.getSeverity() == ResultSeverityEnum.WARNING).collect(Collectors.toList());
            return new ValidationResult(filteredMessages);
        }

        if(validationMessagesFilter == ValidationMessagesFilter.KEEP_ERRORS_ONLY) {
            var filteredMessages = result.getValidationMessages().stream().filter(m -> m.getSeverity() == ResultSeverityEnum.ERROR || m.getSeverity() == ResultSeverityEnum.FATAL).collect(Collectors.toList());
            return new ValidationResult(filteredMessages);
        }

        throw new IllegalArgumentException("Filter " + validationMessagesFilter + " is unsupported");
    }
}
