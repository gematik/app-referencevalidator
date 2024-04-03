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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
public class DuplicateChecker {

    private static final String RULE_BDL_7_MESSAGE = "'FullUrl must be unique in a bundle, or else entries with the same fullUrl must have different meta.versionId (except in history bundles)' Failed for fullUrl: ";
    private static final String RULE_BBUNDLE_MULTIPLE_MATCHES_MESSAGE = "Multiple matches in bundle for reference: ";
    private static final String RULE_BDL_7_CODE = "Rule bdl-7";
    private static final String RULE_BBUNDLE_MULTIPLE_MATCHES_CODE = "Bundle_BUNDLE_MultipleMatches";

    public List<SingleValidationMessage> findDuplicateFullUrls(String resourceBody) {
        Set<String> uniqueFullUrls = new HashSet<>();
        Set<String> duplicateFullUrls = new HashSet<>();
        List<String> allFullUrls = getAllFullUrls(resourceBody);

        for (String fullUrl : allFullUrls) {
            if (!uniqueFullUrls.add(fullUrl)) {
                duplicateFullUrls.add(fullUrl);
            }
        }

        var result = new LinkedList<SingleValidationMessage>();
        for (String fullUrl : duplicateFullUrls) {
            result.add(getValidationMessage(RULE_BDL_7_MESSAGE, RULE_BDL_7_CODE, fullUrl));
        }
        return result;
    }

    public List<SingleValidationMessage> findDuplicateCompositionReferences(String resourceBody) {
        List<String> allFullUrls = getAllFullUrls(resourceBody);
        Pattern pattern = Pattern.compile("<entry>\\s*<reference value=\"(.*?)\"/>\\s*</entry>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(resourceBody);

        List<String> compositionReferences = new ArrayList<>();
        while (matcher.find()) {
            String match = matcher.group(1);
            compositionReferences.add(match);
        }

        var result = new LinkedList<SingleValidationMessage>();
        for (String compositionReference : compositionReferences) {
            int count = 0;
            for (String fullUrl : allFullUrls) {
                if (compositionReference.equals(fullUrl)) {
                    count++;
                }
            }
            if (count > 1) {
                result.add(getValidationMessage(RULE_BBUNDLE_MULTIPLE_MATCHES_MESSAGE, RULE_BBUNDLE_MULTIPLE_MATCHES_CODE, compositionReference));
            }
        }
        return result;
    }

    private SingleValidationMessage getValidationMessage(String message, String messageId, String fullUrl) {
        SingleValidationMessage validationMessage = new SingleValidationMessage();
        validationMessage.setMessage(message + fullUrl);
        validationMessage.setSeverity(ResultSeverityEnum.ERROR);
        validationMessage.setMessageId(messageId);
        return validationMessage;
    }

    private List<String> getAllFullUrls(String resourceBody) {
        List<String> allFullUrls = new ArrayList<>();
        Pattern pattern = Pattern.compile("<entry>\\s*<fullUrl value=\"(.*?)\"/>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(resourceBody);
        while (matcher.find()) {
            String match = matcher.group(1);
            allFullUrls.add(match);
        }
        return allFullUrls;
    }
}
