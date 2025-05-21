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

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.OperationOutcome;

@AllArgsConstructor
public class ValidationResultToOperationOutcomeConverter {
    public static final String PROFILE_URL = "https://gematik.de/fhir/refv/StructureDefinition/REFVValidationResult|1.0.0";
    public static final String VALIDITY_EX = "https://gematik.de/fhir/refv/StructureDefinition/REFVResourceValidityEX";
    public static final String VALIDITY_CS = "https://gematik.de/fhir/refv/CodeSystem/REFVResourceValidityCS";

    private final FhirContext fhirContext;

    public OperationOutcome toOperationOutcome(ValidationResult validationResult) {
        var hapiValidationResult = new ca.uhn.fhir.validation.ValidationResult(fhirContext, new ArrayList<>(validationResult.getValidationMessages()));
        var result = (OperationOutcome) hapiValidationResult.toOperationOutcome();
        result.getMeta().addProfile(PROFILE_URL);
        result.getIssue().forEach(issue -> issue.setDetails(null)); // remove the coding using the FHIR R5 draft CodeSystem http://hl7.org/fhir/java-core-messageId, which leads to validation issues using common validators
        String validity = validationResult.isValid() ? "valid" : "invalid";
        result.addExtension(VALIDITY_EX, new Coding(VALIDITY_CS, validity, validity));
        return result;
    }
}
