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
package de.gematik.refv.commons.validation.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import de.gematik.refv.commons.validation.support.IgnoreCodeSystemValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

class IgnoreCodeSystemValidationSupportTests {

    private FhirContext fhirContext = FhirContext.forR4();

    @Test
    void testConfiguredCodeSystemsToIgnoreDoNotProduceErrors() {
        final String systemA = "http://a";
        final String systemB = "http://b";
        var codeSystemsToIgnore = List.of(new String[] {systemA, systemB});
        var support = new IgnoreCodeSystemValidationSupport(fhirContext, codeSystemsToIgnore);

        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext)
        );
        ValidationSupportContext context = new ValidationSupportContext(validationSupportChain);

        Assertions.assertTrue(support.isCodeSystemSupported(context, systemA));

        var result = support.validateCode(context, new ConceptValidationOptions(), systemA,"someCode","display","http://valueset.url");
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getCode(),"result.getCode() should be empty, otherwise HAPI doesn't produce any ValidationMessages");
        Assertions.assertEquals(IValidationSupport.IssueSeverity.INFORMATION, result.getSeverity(), "Wrong severity of the code validation result");

        IValidationSupport.LookupCodeResult lookupCodeResult = support.lookupCode(context, systemA, "a-code", "en-US");
        Assertions.assertNotNull(lookupCodeResult, "Code lookup for an ignored code system is null while it should not be");
        Assertions.assertTrue(lookupCodeResult.isFound(), "Code lookup for an ignored code system is unsuccessful while it should be");
    }

    @Test
    void testUnregisteredCodesystemsAreUnsupported() {
        final String systemA = "http://a";
        final String systemB = "http://b";
        final String systemC = "http://c";
        var codeSystemsToIgnore = List.of(new String[] {systemA, systemB});
        var support = new IgnoreCodeSystemValidationSupport(fhirContext, codeSystemsToIgnore);

        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext)
        );
        ValidationSupportContext context = new ValidationSupportContext(validationSupportChain);

        Assertions.assertFalse(support.isCodeSystemSupported(context, systemC));

        var result = support.validateCode(context, new ConceptValidationOptions(), systemC,"someCode","display","http://valueset.url");
        Assertions.assertNull(result);

        Assertions.assertNull(support.lookupCode(context, systemC, "c-code", "en-US"), "Code lookup for a code of a not registered system is successful while it shouldn't be");
    }

    @Test
    void testEmptyCodeSystemsAreNotAllowed() {
        var codeSystemsToIgnore = new LinkedList<String>();
        codeSystemsToIgnore.add("http://a");
        codeSystemsToIgnore.add(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IgnoreCodeSystemValidationSupport(fhirContext, codeSystemsToIgnore));
    }

}
