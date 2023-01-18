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

package de.gematik.refv.commons.validation.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.common.hapi.validation.support.BaseValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.utilities.validation.ValidationMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Modified from <a href="https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/support/IgnoreMissingValueSetValidationSupport.java">https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/support/IgnoreMissingValueSetValidationSupport.java</a>
 * Copyright 2022 Deutscher Apothekerverband (DAV), Apache License, Version 2.0
 *
 * This validation support module must be placed at the end of a {@link ValidationSupportChain}
 * in order to configure the validator to generate a warning if a resource being validated
 * contains an unknown code system.
 */
public class IgnoreMissingValueSetValidationSupport extends BaseValidationSupport {

    private final Collection<String> codeSystemsToIgnore = new HashSet<>();

    public static final String CODE_SYSTEM_IS_IGNORED_MESSAGE = "Code system has been ignored due to module configuration";

    /**
     * Constructor
     * @param theFhirContext {@link FhirContext}
     */
    public IgnoreMissingValueSetValidationSupport(FhirContext theFhirContext, Collection<String> codeSystemsToIgnore) {
        super(theFhirContext);
        for (String codeSystemToIgnore :
                codeSystemsToIgnore) {
            if(StringUtils.isEmpty(codeSystemToIgnore))
                throw new IllegalArgumentException("Empty code systems to ignore are not allowed");
            this.codeSystemsToIgnore.add(codeSystemToIgnore);
        }
    }

    @Override
    public boolean isCodeSystemSupported(ValidationSupportContext theValidationSupportContext, String theCodeSystem) {
        return theCodeSystem != null && codeSystemsToIgnore.contains(theCodeSystem);
    }

    @Nullable
    @Override
    public CodeValidationResult validateCode(@Nonnull ValidationSupportContext theValidationSupportContext,@Nonnull ConceptValidationOptions theOptions, String theCodeSystem, String theCode, String theDisplay, String theValueSetUrl) {
        if (theCodeSystem != null && codeSystemsToIgnore.contains(theCodeSystem)) {
            CodeValidationResult result = new CodeValidationResult();
            result.setSeverity(IssueSeverity.INFORMATION);
            result.setSeverityCode(ValidationMessage.IssueSeverity.INFORMATION.toCode());
            result.setCodeSystemName(theCodeSystem);
            result.setMessage(CODE_SYSTEM_IS_IGNORED_MESSAGE);
            return result;
        } else return null;
    }

}
