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
import de.gematik.refv.commons.configuration.FhirPackageConfigurationLoader;
import de.gematik.refv.commons.configuration.PackageReference;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionSnapshotComponent;
import org.junit.jupiter.api.Test;

class ModuleSnapshotGeneratorTest {

    public static final String CONFIG_FILE = "packages-snapshot.yaml";

    @Test
    void shouldLoadPackages() throws IOException {
        FhirPackageConfigurationLoader loader = new FhirPackageConfigurationLoader(CONFIG_FILE);
        ValidationModuleConfiguration configuration = loader.getConfiguration();

        ModuleSnapshotGenerator snapshotGenerator = new ModuleSnapshotGenerator(FhirContext.forR4());
        List<PackageResources> packageResources =
                snapshotGenerator.generateConformanceResources(configuration).collect(Collectors.toList());

        assertEquals(2, packageResources.size());
        assertEquals(packageResources.get(0).getReference(), new PackageReference("kbv.basis", "1.1.3"));
        assertEquals(packageResources.get(1).getReference(), new PackageReference("de.basisprofil.r4", "0.9.13"));
        packageResources.forEach(ModuleSnapshotGeneratorTest::validatePackageResources);
    }

    private static void validatePackageResources(final PackageResources resources) {
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
