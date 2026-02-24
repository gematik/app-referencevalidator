/*-
 * #%L
 * commons
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
package de.gematik.refv.commons.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.IValidationContext;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BundleValidationModuleTests {


    @CsvSource({
            "http://example.com/Patient/123,123",
            "urn:test:blabla,123", // Non-URL fullUrl
            "Patient/456,123", // relative fullUrl
            ",123", // empty fullUrl
            "http://example.com/Patient/123,", // empty id
    })
    @ParameterizedTest
    void validateResourceWithValidFullUrlAndId(String fullUrl, String id) {
        Bundle bundle = new Bundle();
        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        entry.setFullUrl(fullUrl);
        entry.setResource(new Patient().setId(id));
        bundle.addEntry(entry);

        IValidationContext<IBaseResource> context = createValidationContext(bundle);
        new BundleValidationModule().validateResource(context);

        assertTrue(context.toResult().getMessages().isEmpty(), "No validation messages should be present for valid fullUrl and id: " + context.toResult().toString());
    }

    @Test
    void validateResourceWithFullUrlNotEndingWithId() {
        Bundle bundle = new Bundle();
        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        entry.setFullUrl("http://example.com/Patient/456");
        entry.setResource(new Patient().setId("123"));
        bundle.addEntry(entry);

        IValidationContext<IBaseResource> context = createValidationContext(bundle);
        new BundleValidationModule().validateResource(context);

        assertEquals(1, context.toResult().getMessages().size(), "One validation message should be present for fullUrl not ending with id");
        assertEquals(ResultSeverityEnum.WARNING, context.toResult().getMessages().get(0).getSeverity(), "Validation message should be a warning");
    }

    @Test
    void validateResourceWithNonBundleResource() {
        Patient patient = new Patient();
        IValidationContext<IBaseResource> context = createValidationContext(patient);
        new BundleValidationModule().validateResource(context);

        assertTrue(context.toResult().getMessages().isEmpty(), "No validation messages should be present for non-bundle resource");
    }

    private IValidationContext<IBaseResource> createValidationContext(IBaseResource resource) {
        return ValidationContext.forResource(FhirContext.forR4Cached(), resource, null);
    }
}
