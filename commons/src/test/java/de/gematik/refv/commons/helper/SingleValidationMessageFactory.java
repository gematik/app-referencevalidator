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
package de.gematik.refv.commons.helper;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;

import java.util.Collection;
import java.util.List;

public class SingleValidationMessageFactory {

    public static Collection<SingleValidationMessage> getMixedValidationMessages() {
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

        return List.of(m1,m2,m3,m4);
    }
}