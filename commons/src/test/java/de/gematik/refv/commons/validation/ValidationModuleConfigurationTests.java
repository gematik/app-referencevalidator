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

import de.gematik.refv.commons.Profile;
import de.gematik.refv.commons.configuration.FhirPackageConfigurationLoader;
import de.gematik.refv.commons.configuration.PackageReference;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.exceptions.UnsupportedPackageException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

class ValidationModuleConfigurationTests {

    static ValidationModuleConfiguration configuration;

    @BeforeAll
    public static void startup() throws IOException {
        FhirPackageConfigurationLoader loader = new FhirPackageConfigurationLoader();
        configuration = loader.getConfiguration();
    }

    @Test
    void testPackageDefinitionForProfileWithVersion() {
        Profile lookupProfile = Profile.parse("https://gematik.de/fhir/StructureDefinition/ErxMedicationDispense|1.1.1");
        var packageDefinition = configuration.getPackageDefinitionForProfile(lookupProfile);
        Assertions.assertTrue(packageDefinition.isPresent());
        Assertions.assertEquals(3, packageDefinition.get().getDependencies().size());
        Assertions.assertLinesMatch(List.of("dav.kbv.sfhir.cs.vs","kbv.ita.erp","de.basisprofil.r4"),packageDefinition.get().getDependencies().stream().map(PackageReference::getPackageName).collect(Collectors.toList()));
    }

    @Test
    void testPackageDefinitionForProfileWithoutVersion() {
        Profile lookupProfile = Profile.parse("https://gematik.de/fhir/StructureDefinition/ErxMedicationDispense");
        var packageDefinition = configuration.getPackageDefinitionForProfile(lookupProfile);
        Assertions.assertTrue(packageDefinition.isPresent());
        Assertions.assertEquals("de.gematik.erezept-workflow.r4-1.0.3-1.tgz", packageDefinition.get().getFilename());
        Assertions.assertEquals(5, packageDefinition.get().getDependencies().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://bla", // Unknown profile
            "https://profile-with-unknown-package|1.0.0", // Unknown package for profile
            "https://profile-with-unknown-package-version|1.0.0", // Unknown package version for profile
    })
    void testPackageDefinitionIsAbsent(String canonical) {
        Profile lookupProfile = Profile.parse(canonical);
        var packageDefinition = configuration.getPackageDefinitionForProfile(lookupProfile);
        Assertions.assertTrue(packageDefinition.isEmpty());
    }

    @Test
    void testGetFilenamesFromPackageDependencies() {
        Profile lookupProfile = Profile.parse("https://gematik.de/fhir/StructureDefinition/ErxMedicationDispense|1.1.1");
        var packageDefinition = configuration.getPackageDefinitionForProfile(lookupProfile);

        assert packageDefinition.isPresent();
        var filenames = configuration.getDistinctFilenamesFromPackageDependencies(packageDefinition.get().getDependencies());

        List<String> expected = List.of("dav.kbv.sfhir.cs.vs-1.0.3-json.tgz",
               "kbv.ita.erp-1.0.2.tgz",
               "de.basisprofil.r4-0.9.13.tgz",
               "kbv.ita.for-1.0.3.tgz",
               "kbv.basis-1.1.3.tgz");
        Assertions.assertEquals(expected.size(), filenames.size());
        Assertions.assertTrue(filenames.containsAll(expected));
        Assertions.assertTrue(expected.containsAll(filenames));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://profile-with-unknown-package-dependency|1.0.0",
            "https://profile-with-unknown-package-version-dependency|1.0.0"})
    void testGetFilenamesFromPackageDependenciesWithNonExistingPackage(String canonical) {
        Profile lookupProfile = Profile.parse(canonical);
        var packageDefinition = configuration.getPackageDefinitionForProfile(lookupProfile);

        assert packageDefinition.isPresent();
        Assertions.assertThrows(UnsupportedPackageException.class, () -> configuration.getDistinctFilenamesFromPackageDependencies(packageDefinition.get().getDependencies()));
    }

}
