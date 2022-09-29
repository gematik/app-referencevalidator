/*
 * Copyright (c) 2022 gematik GmbH
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
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import de.gematik.refv.commons.validation.support.FixedSnapshotGeneratingValidationSupport;
import lombok.SneakyThrows;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FixedSnapshotGeneratingValidationSupportIT {

    private final FhirContext fhirContext = FhirContext.forR4();

    @Test
    @SneakyThrows
    void testNoBufferOverflow() {
        NpmPackageValidationSupport npmPackageSupport = new NpmPackageValidationSupport(fhirContext);
        npmPackageSupport.loadPackageFromClasspath("classpath:package/de.gkvsv.erezeptabrechnungsdaten-1.2.0.tgz");

        ValidationSupportChain validationSupportChain = new ValidationSupportChain(
                npmPackageSupport,
                new DefaultProfileValidationSupport(fhirContext),
                // new SnapshotGeneratingValidationSupport(fhirContext) // (!) default snapshot generator causes bufferoverflow on given resource
                new FixedSnapshotGeneratingValidationSupport(fhirContext)
        );

        var resource = validationSupportChain.fetchResource(StructureDefinition.class, "https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_Sammelrechnung_Bundle");

        Assertions.assertDoesNotThrow(() -> validationSupportChain.generateSnapshot(
                new ValidationSupportContext(validationSupportChain),
                resource,
                "https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_Sammelrechnung_Bundle|1.0.6",
                null,
                "MY PROFILE"
                ));
    }
}
