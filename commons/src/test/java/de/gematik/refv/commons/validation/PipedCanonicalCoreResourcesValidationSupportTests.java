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
import de.gematik.refv.commons.validation.support.PipedCanonicalCoreResourcesValidationSupport;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PipedCanonicalCoreResourcesValidationSupportTests {

    public static final FhirContext CTX = FhirContext.forR4();
    public static final PipedCanonicalCoreResourcesValidationSupport VALIDATION_SUPPORT = new PipedCanonicalCoreResourcesValidationSupport(CTX);

    @Test
    void testPipedCanonicalIsFetchedCorrectly() {
        String canonical = "http://hl7.org/fhir/StructureDefinition/Binary|4.0.1";
        var resource = VALIDATION_SUPPORT.fetchResource(StructureDefinition.class, canonical);
        Assertions.assertNotNull(resource);
        Assertions.assertEquals(canonical, String.format("%s|%s", resource.getUrl(), resource.getVersion()));
    }

    @Test
    void testNonPipedCanonicalsAreIgnored() {
        String canonical = "http://hl7.org/fhir/StructureDefinition/Binary";
        var resource = VALIDATION_SUPPORT.fetchResource(StructureDefinition.class, canonical);
        Assertions.assertNull(resource);
    }
}
