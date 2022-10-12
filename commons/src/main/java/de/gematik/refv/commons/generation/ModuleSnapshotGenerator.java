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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import de.gematik.refv.commons.configuration.PackageDefinition;
import de.gematik.refv.commons.configuration.PackageDefinitionByVersion;
import de.gematik.refv.commons.configuration.PackageReference;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.validation.support.FixedSnapshotGeneratingValidationSupport;
import de.gematik.refv.commons.validation.support.NpmPackageLoader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;

/**
 * Generator of conformance resources for a {@link ValidationModuleConfiguration}.
 */
public class ModuleSnapshotGenerator {
    private FhirContext fhirContext;

    public ModuleSnapshotGenerator(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    /**
     * Loads all conformance resources with added snapshot elements for each package in the configuration.
     *
     * @param configuration the packages for the module
     * @return a stream of a package reference with all its conformance resources
     */
    public Stream<PackageResources> generateConformanceResources(ValidationModuleConfiguration configuration) {
        Map<PackageReference, PackageDefinition> knownPackages = getKnownPackages(configuration);
        Map<PackageReference, IValidationSupport> packageSupport = this.loadPackages(knownPackages);

        ValidationSupportChain supportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(this.fhirContext),
                // required for expanding value sets
                new InMemoryTerminologyServerValidationSupport(this.fhirContext),
                new FixedSnapshotGeneratingValidationSupport(this.fhirContext)
        );

        PackageSnapshotGenerator snapshotGenerator =
                new PackageSnapshotGenerator(supportChain, knownPackages, packageSupport);

        return knownPackages.keySet().stream().map(snapshotGenerator::generateResourcesWithSnapshots);
    }

    private static Map<PackageReference, PackageDefinition> getKnownPackages(
            ValidationModuleConfiguration configuration) {
        Map<PackageReference, PackageDefinition> knownPackages = new HashMap<>();
        for (Map.Entry<String, PackageDefinitionByVersion> fhirPackage : configuration.getFhirPackages().entrySet()) {
            String packageName = fhirPackage.getKey();
            PackageDefinitionByVersion definitions = fhirPackage.getValue();
            for (Map.Entry<String, PackageDefinition> packageVersion : definitions.getPackageVersions().entrySet()) {
                String version = packageVersion.getKey();
                PackageDefinition packageDefinition = packageVersion.getValue();
                knownPackages.put(new PackageReference(packageName, version), packageDefinition);
            }
        }
        return knownPackages;
    }

    private Map<PackageReference, IValidationSupport> loadPackages(
            Map<PackageReference, PackageDefinition> references) {
        NpmPackageLoader npmPackageLoader = new NpmPackageLoader(this.fhirContext);
        return references.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                reference -> this.loadPackageSupport(npmPackageLoader, reference)
        ));
    }

    private PrePopulatedValidationSupport loadPackageSupport(NpmPackageLoader npmPackageLoader,
            Entry<PackageReference, PackageDefinition> reference) {
        List<String> filename = List.of(reference.getValue().getFilename());
        try {
            return npmPackageLoader.loadPackagesAndCreatePrePopulatedValidationSupport(filename);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

}
