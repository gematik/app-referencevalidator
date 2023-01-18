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

package de.gematik.refv.commons.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import de.gematik.refv.commons.validation.support.IgnoreMissingValueSetValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

class IgnoreMissingValueSetValidationSupportTests {

    @Test
    void testMissingValueSetsAreIgnored() {
        FhirContext fhirContext = FhirContext.forR4();
        var codeSystemsToIgnore = List.of(new String[] {"http://a","http://b"});
        var support = new IgnoreMissingValueSetValidationSupport(fhirContext, codeSystemsToIgnore);

        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext)
        );
        Assertions.assertTrue(support.isCodeSystemSupported(new ValidationSupportContext(validationSupportChain),"http://a"));
        var result = support.validateCode(new ValidationSupportContext(validationSupportChain), new ConceptValidationOptions(),"http://a","someCode","display","http://valueset.url");
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getCode(),"result.getCode() should be empty, otherwise HAPI doesn't produce any ValidationMessages");
        Assertions.assertEquals(IValidationSupport.IssueSeverity.INFORMATION, result.getSeverity(), "Wrong severity of the code validation result");
    }

    @Test
    void testUnregisteredCodesystemsAreUnsupported() {
        FhirContext fhirContext = FhirContext.forR4();
        var codeSystemsToIgnore = List.of(new String[] {"http://a","http://b"});
        var support = new IgnoreMissingValueSetValidationSupport(fhirContext, codeSystemsToIgnore);

        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext)
        );
        Assertions.assertFalse(support.isCodeSystemSupported(new ValidationSupportContext(validationSupportChain),"http://c"));
        var result = support.validateCode(new ValidationSupportContext(validationSupportChain), new ConceptValidationOptions(),"http://c","someCode","display","http://valueset.url");
        Assertions.assertNull(result);
    }

    @Test
    void testEmptyCodeSystemsAreNotAllowed() {
        FhirContext fhirContext = FhirContext.forR4();
        var codeSystemsToIgnore = new LinkedList<String>();
        codeSystemsToIgnore.add("http://a");
        codeSystemsToIgnore.add(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IgnoreMissingValueSetValidationSupport(fhirContext, codeSystemsToIgnore));
    }

}
