/*-
 * #%L
 * commons
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * *******
 * 
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.commons.validation;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.gematik.refv.commons.configuration.ValidationMessageTransformation;
import de.gematik.refv.commons.validation.support.IgnoreCodeSystemValidationSupport;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.hl7.fhir.utilities.i18n.I18nConstants;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SeverityLevelTransformer {

    public List<SingleValidationMessage> applyTransformations(
            Collection<SingleValidationMessage> input,
            @NonNull
            Collection<ValidationMessageTransformation> transformations
    ) {
        return applyTransformations(input, transformations, List.of());
    }

    public List<SingleValidationMessage> applyTransformations(
            Collection<SingleValidationMessage> input,
            @NonNull
            Collection<ValidationMessageTransformation> transformations,
            Collection<String> ignoredValueSets
    ) {
        LinkedList<SingleValidationMessage> transformedMessages = new LinkedList<>(input);
        escalateUnresolvedValueSetsToError(transformedMessages, ignoredValueSets);
        setIgnoredCodeSystemsToInformation(transformedMessages);

        for (SingleValidationMessage message:
                transformedMessages) {

            var transformation = transformations.stream().filter(t -> t.getSeverityLevelFrom().equals(message.getSeverity().getCode()) &&
                    messageMatchesPattern(message.getMessage(), t.getLocatorString()) &&
                    (
                            t.getMessageId() == null || Strings.CS.equals(t.getMessageId(), message.getMessageId())
                    ) &&
                    (
                            t.getMessageLocationRegex() == null || messageMatchesPattern(message.getLocationString(), t.getMessageLocationRegex())
                    )
            ).findFirst();
            if(transformation.isEmpty()) {
                continue;
            }

            message.setSeverity(ResultSeverityEnum.fromCode(transformation.get().getSeverityLevelTo()));
        }
        return transformedMessages;
    }

    private static boolean messageMatchesPattern(String message, String locator) {
        if(StringUtils.isEmpty(locator))
            return true;

        Pattern pattern = Pattern.compile(locator);
        Matcher matcher = pattern.matcher(message);
        return matcher.find();
    }

    private void setIgnoredCodeSystemsToInformation(LinkedList<SingleValidationMessage> messages) {
        for (SingleValidationMessage message:
                messages) {
            if(I18nConstants.TERMINOLOGY_PASSTHROUGH_TX_MESSAGE.equals(message.getMessageId()) && message.getMessage().contains(IgnoreCodeSystemValidationSupport.CODE_SYSTEM_IS_IGNORED_MESSAGE)) {
                message.setSeverity(ResultSeverityEnum.INFORMATION);
            }
        }
    }

    private void escalateUnresolvedValueSetsToError(LinkedList<SingleValidationMessage> messages, Collection<String> ignoredValueSets) {
        for (SingleValidationMessage message:
                messages) {
            if(I18nConstants.TERMINOLOGY_TX_VALUESET_NOTFOUND.equals(message.getMessageId())) {
                // Caution! message.getMessage() is locale specific
                if(ignoredValueSets.stream().noneMatch(ignoredValueSet -> message.getMessage().contains(ignoredValueSet)))
                    message.setSeverity(ResultSeverityEnum.ERROR);
            }
        }
    }
}
