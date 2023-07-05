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
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import de.gematik.refv.commons.validation.support.IgnoreValueSetValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

class IgnoreValueSetValidationSupportTests {

    @Test
    void testMissingValueSetsAreIgnored() {
        String theValueSetUrl = "http://hl7.org/fhir/ValueSet/languages";
        FhirContext fhirContext = FhirContext.forR4();
        var valueSetsToIgnore = List.of(new String[] {theValueSetUrl,"http://b"});
        var support = new IgnoreValueSetValidationSupport(fhirContext, valueSetsToIgnore);

        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext)
        );
        ValidationSupportContext theValidationSupportContext = new ValidationSupportContext(validationSupportChain);
        Assertions.assertTrue(support.isValueSetSupported(theValidationSupportContext,theValueSetUrl));
        var resource = theValidationSupportContext.getRootValidationSupport().fetchValueSet(theValueSetUrl);
        assert resource != null;
        var result = support.validateCodeInValueSet(new ValidationSupportContext(validationSupportChain), new ConceptValidationOptions(),"http://a","someCode","display", resource);
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getCode(),"result.getCode() should be empty, otherwise HAPI doesn't produce any ValidationMessages");
        Assertions.assertEquals(IValidationSupport.IssueSeverity.INFORMATION, result.getSeverity(), "Wrong severity of the code validation result");
    }

    @Test
    void testUnregisteredValueSetsAreUnsupported() {
        FhirContext fhirContext = FhirContext.forR4();
        var valueSetsToIgnore = List.of(new String[] {"http://a","http://b"});
        var support = new IgnoreValueSetValidationSupport(fhirContext, valueSetsToIgnore);

        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext)
        );
        Assertions.assertFalse(support.isValueSetSupported(new ValidationSupportContext(validationSupportChain),"http://c"));
    }

    @Test
    void testEmptyValueSetsAreUnsupported() {
        FhirContext fhirContext = FhirContext.forR4();
        var valueSetsToIgnore = List.of(new String[] {"http://a","http://b"});
        var support = new IgnoreValueSetValidationSupport(fhirContext, valueSetsToIgnore);

        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext)
        );
        Assertions.assertFalse(support.isValueSetSupported(new ValidationSupportContext(validationSupportChain),null));
    }

    @Test
    void testEmptyValueSetsAreNotAllowed() {
        FhirContext fhirContext = FhirContext.forR4();
        var valueSetsToIgnore = new LinkedList<String>();
        valueSetsToIgnore.add("http://a");
        valueSetsToIgnore.add(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IgnoreValueSetValidationSupport(fhirContext, valueSetsToIgnore));
    }
}
