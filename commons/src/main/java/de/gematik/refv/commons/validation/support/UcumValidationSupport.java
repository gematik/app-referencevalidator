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
package de.gematik.refv.commons.validation.support;

import static org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService.UCUM_CODESYSTEM_URL;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.util.ClasspathUtil;
import java.io.InputStream;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.fhir.ucum.UcumEssenceService;
import org.fhir.ucum.UcumException;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;

@Slf4j
public class UcumValidationSupport implements IValidationSupport {

    private FhirContext fhirContext;

    private IssueSeverity issueSeverity;

    public UcumValidationSupport(FhirContext theFhirContext, IssueSeverity issueSeverity) {
        this.fhirContext = theFhirContext;
        this.issueSeverity = issueSeverity;
    }

    @Override
    public boolean isCodeSystemSupported(ValidationSupportContext theValidationSupportContext, String theSystem) {
        return UCUM_CODESYSTEM_URL.equals(theSystem);
    }

    @Override
    public boolean isValueSetSupported(ValidationSupportContext theValidationSupportContext, String theValueSetUrl) {
        return CommonCodeSystemsTerminologyService.UCUM_VALUESET_URL.equals(theValueSetUrl);
    }

    @Override
    public FhirContext getFhirContext() {
        return this.fhirContext;
    }

    // Necessary to override, so that different ValueSets using UCUM as CodeSystem can be validated
    @Override
    public LookupCodeResult lookupCode(ValidationSupportContext theValidationSupportContext, String theSystem, String theCode, String theDisplayLanguage) {
        if (theSystem.equals(UCUM_CODESYSTEM_URL)) {
            // Cf. CommonCodeSystemsTerminologyService.lookupUcumCode

            InputStream input = ClasspathUtil.loadResourceAsStream("/ucum-essence.xml");
            String outcome = null;
            try {
                UcumEssenceService svc = new UcumEssenceService(input);
                outcome = svc.analyse(theCode);
            } catch (UcumException e) {
                log.debug("Failed to parse UCUM code {}: {}", theCode, e.getMessage()); // Deviation from CommonCodeSystemsTerminologyService.lookupUcumCode --> log.level <- DEBUG
            } finally {
                ClasspathUtil.close(input);
            }
            LookupCodeResult retVal = new LookupCodeResult();
            retVal.setSearchedForCode(theCode);
            retVal.setSearchedForSystem(UCUM_CODESYSTEM_URL);
            if (outcome != null) {
                retVal.setFound(true);
                retVal.setCodeDisplay(outcome);
            }
            return retVal;
        }
        return null;
    }

    @Override
    public CodeValidationResult validateCode(@Nonnull ValidationSupportContext theValidationSupportContext, @Nonnull ConceptValidationOptions theOptions, String theCodeSystem, String theCode, String theDisplay, String theValueSetUrl) {
        LookupCodeResult lookupResult = lookupCode(theValidationSupportContext, theCodeSystem, theCode, null);
        if(lookupResult.isFound())
            return new CodeValidationResult().setCode(theCode).setDisplay(lookupResult.getCodeDisplay());

        // The presence of only the following two attributes is important to make sure the code validation will result in a Warning or an Error
        return new CodeValidationResult()
                .setMessage("Validation failed")
                .setSeverity(issueSeverity);
    }
}
