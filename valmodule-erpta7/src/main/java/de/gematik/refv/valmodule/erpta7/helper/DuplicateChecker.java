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
package de.gematik.refv.valmodule.erpta7.helper;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import lombok.NoArgsConstructor;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

@NoArgsConstructor
public class DuplicateChecker {

    private static final String RULE_BDL_7_MESSAGE = "'FullUrl must be unique in a bundle, or else entries with the same fullUrl must have different meta.versionId (except in history bundles)' Failed for fullUrl: ";
    private static final String RULE_BDL_7_CODE = "Rule bdl-7";

    public List<SingleValidationMessage> findDuplicateFullUrls(String resourceBody) {
        var allFullUrls = getAllFullUrls(resourceBody);
        var result = new LinkedList<SingleValidationMessage>();
        for (int i = 0; i < allFullUrls.length-1; i++) {
            if (allFullUrls[i].equals(allFullUrls[i + 1])) {
                SingleValidationMessage validationMessage = new SingleValidationMessage();
                validationMessage.setMessage(RULE_BDL_7_MESSAGE + allFullUrls[i]);
                validationMessage.setSeverity(ResultSeverityEnum.ERROR);
                validationMessage.setMessageId(RULE_BDL_7_CODE);
                result.add(validationMessage);
            }
        }
        return result;
    }

    private String[] getAllFullUrls(String resourceBody) {
        Pattern pattern = Pattern.compile("<entry>\\s*<fullUrl value=\"([^\"]+)\"", Pattern.DOTALL);
        return pattern.matcher(resourceBody).results().map(r -> r.group(1)).sorted().toArray(String[]::new);
    }
}
