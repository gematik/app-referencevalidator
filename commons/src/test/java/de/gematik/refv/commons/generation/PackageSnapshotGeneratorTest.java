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

package de.gematik.refv.commons.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import de.gematik.refv.commons.configuration.PackageDefinition;
import de.gematik.refv.commons.configuration.PackageReference;
import de.gematik.refv.commons.validation.support.NpmPackageLoader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionSnapshotComponent;
import org.junit.jupiter.api.Test;

class PackageSnapshotGeneratorTest {

    @Test
    void shouldGenerateConformanceResourcesWithSnapshots() throws IOException {
        FhirContext fhirContext = FhirContext.forR4();

        PackageReference kbvBasis = new PackageReference("kbv.basis", "1.1.3");
        PackageReference basisProfil = new PackageReference("de.basisprofil.r4", "0.9.13");

        PackageDefinition kbvPackageDefinition =
                new PackageDefinition("kbv.basis-1.1.3.tgz", List.of(basisProfil), List.of());
        PackageDefinition basisProfilDefinition =
                new PackageDefinition("de.basisprofil.r4-0.9.13.tgz", List.of(), List.of());

        Map<PackageReference, PackageDefinition> knownPackages = Map.of(
                kbvBasis, kbvPackageDefinition,
                basisProfil, basisProfilDefinition
        );

        final NpmPackageLoader loader = new NpmPackageLoader(fhirContext);
        PrePopulatedValidationSupport kbvBasisSupport = loader.loadPackagesAndCreatePrePopulatedValidationSupport(
                List.of(kbvPackageDefinition.getFilename()));
        PrePopulatedValidationSupport basisProfilSupport =
                loader.loadPackagesAndCreatePrePopulatedValidationSupport(List.of(basisProfilDefinition.getFilename()));

        Map<PackageReference, IValidationSupport> packageSupport = Map.of(
                kbvBasis, kbvBasisSupport,
                basisProfil, basisProfilSupport
        );

        PackageSnapshotGenerator snapshotGenerator =
                new PackageSnapshotGenerator(fhirContext, knownPackages, packageSupport);

        PackageResources kbvBasisResources = snapshotGenerator.generateResourcesWithSnapshots(kbvBasis);
        PackageResources basisProfilResources = snapshotGenerator.generateResourcesWithSnapshots(basisProfil);

        validatePackageResources(kbvBasis, kbvBasisResources);
        validatePackageResources(basisProfil, basisProfilResources);
    }

    private static void validatePackageResources(PackageReference reference, PackageResources resources) {
        assertEquals(resources.getReference(), reference);

        for (MetadataResource conformanceResource : resources.getConformanceResources()) {
            if (conformanceResource instanceof StructureDefinition) {
                StructureDefinitionSnapshotComponent snapshot =
                        ((StructureDefinition) conformanceResource).getSnapshot();
                assertNotNull(snapshot);
                assertFalse(snapshot.isEmpty());
            }
        }
    }

}
