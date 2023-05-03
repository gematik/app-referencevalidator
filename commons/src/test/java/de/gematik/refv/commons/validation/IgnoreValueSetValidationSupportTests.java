package de.gematik.refv.commons.validation;

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
