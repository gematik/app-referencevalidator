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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.commons.validation.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.junit.jupiter.api.Test;

public class UcumValidationSupportTests {

    public static final IValidationSupport.IssueSeverity ISSUE_SEVERITY = IValidationSupport.IssueSeverity.WARNING;
    private ValidationSupportChain validationSupportChain;

    private ValidationSupportContext validationSupportContext;

    public UcumValidationSupportTests() {
        UcumValidationSupport support = new UcumValidationSupport(FhirContext.forR4(), ISSUE_SEVERITY);
        validationSupportChain = new ValidationSupportChain(support);
        validationSupportContext = new ValidationSupportContext(validationSupportChain);
    }
    @Test
    void correctValuesAreAccepted() {
        String code = "mg";
        var result = getResult(code);
        assertNotNull(result.getCode(), "Code has not been validated");
    }

    @Test
    void incorrectValuesAreDeclined() {
        String code = "nonexistingcode";
        var result = getResult(code);
        assertNull(result.getCode(), "Code has been validated, while it shouldn't");
        assertEquals(ISSUE_SEVERITY, result.getSeverity(), "Configured severity level not applied");
    }

    @Test
    void severityLevelCanBeConfigured() {
        String code = "Stück";

        var newIssueSeverity = IValidationSupport.IssueSeverity.ERROR;
        UcumValidationSupport support = new UcumValidationSupport(FhirContext.forR4(), newIssueSeverity);
        var validationSupportChain = new ValidationSupportChain(support);
        var validationSupportContext = new ValidationSupportContext(validationSupportChain);

        var result = validationSupportChain.validateCode(validationSupportContext, null, CommonCodeSystemsTerminologyService.UCUM_CODESYSTEM_URL, code, code, CommonCodeSystemsTerminologyService.UCUM_VALUESET_URL);

        assertNull(result.getCode(), "Code has been validated, while it shouldn't");
        assertEquals(newIssueSeverity, result.getSeverity(), "Configured severity level not applied");
    }

    @Test
    void umlautsAreDeclined() {
        String code = "Stück";
        var result = getResult(code);
        assertNull(result.getCode(), "Code has been validated, while it shouldn't");
        assertEquals(ISSUE_SEVERITY, result.getSeverity(), "Configured severity level not applied");
    }

    private IValidationSupport.CodeValidationResult getResult(String code) {
        return validationSupportChain.validateCode(validationSupportContext, null, CommonCodeSystemsTerminologyService.UCUM_CODESYSTEM_URL, code, code, CommonCodeSystemsTerminologyService.UCUM_VALUESET_URL);
    }
}
