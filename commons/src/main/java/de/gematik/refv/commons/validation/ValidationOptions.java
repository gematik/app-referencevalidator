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

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class ValidationOptions {

    private List<String> profiles = new LinkedList<>();
    private List<String> acceptedEncodings = new LinkedList<>();
    private ProfileValidityPeriodCheckStrategy profileValidityPeriodCheckStrategy;
    private ValidationMessagesFilter validationMessagesFilter;

    private ValidationOptions() {
    }

    public static ValidationOptions getDefaults() {
        ValidationOptions options = new ValidationOptions();
        options.setProfileValidityPeriodCheckStrategy(ProfileValidityPeriodCheckStrategy.VALIDATE);
        options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ERRORS_ONLY);
        return options;
    }
}
