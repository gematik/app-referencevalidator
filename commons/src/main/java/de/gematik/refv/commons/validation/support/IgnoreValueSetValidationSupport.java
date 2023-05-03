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
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;

public class IgnoreValueSetValidationSupport extends BaseValidationSupport {

    static Logger logger = LoggerFactory.getLogger(IgnoreValueSetValidationSupport.class);

    private final Collection<String> valueSetsToIgnore = new HashSet<>();

    public static final String VALUE_SET_IS_IGNORED_MESSAGE = "Value set has been ignored due to module configuration";

    /**
     * Constructor
     * @param theFhirContext {@link FhirContext}
     */
    public IgnoreValueSetValidationSupport(FhirContext theFhirContext, Collection<String> valueSetsToIgnore) {
        super(theFhirContext);
        for (String valueSetToIgnore :
                valueSetsToIgnore) {
            if(StringUtils.isEmpty(valueSetToIgnore))
                throw new IllegalArgumentException("Empty value sets to ignore are not allowed");
            this.valueSetsToIgnore.add(valueSetToIgnore);
        }
    }

    @Override
    public boolean isValueSetSupported(ValidationSupportContext theValidationSupportContext, String theValueSetUrl) {
        if(theValueSetUrl == null)
            return false;

        var resource = theValidationSupportContext.getRootValidationSupport().fetchValueSet(theValueSetUrl);
        if(resource == null)
            return false;

        var valueSet = (ValueSet)resource;

        return valueSetsToIgnore.contains(valueSet.getUrl());
    }

    @Override
    @Nullable
    public CodeValidationResult validateCodeInValueSet(ValidationSupportContext theValidationSupportContext, ConceptValidationOptions theOptions, String theCodeSystem, String theCode, String theDisplay, @Nonnull IBaseResource theValueSet) {
        var valueSet = (ValueSet)theValueSet;
        if (valueSetsToIgnore.contains(valueSet.getUrl())) {
            CodeValidationResult result = new CodeValidationResult();
            result.setSeverity(IssueSeverity.INFORMATION);
            result.setSeverityCode(ValidationMessage.IssueSeverity.INFORMATION.toCode());
            result.setMessage(VALUE_SET_IS_IGNORED_MESSAGE);
            logger.info("Value set {} has been ignored due to configuration.", valueSet.getUrl());
            return result;
        }

        return null;
    }

}
