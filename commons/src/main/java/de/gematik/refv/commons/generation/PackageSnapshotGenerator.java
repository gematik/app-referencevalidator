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
import ca.uhn.fhir.context.support.ValidationSupportContext;
import de.gematik.refv.commons.configuration.PackageDefinition;
import de.gematik.refv.commons.configuration.PackageReference;
import de.gematik.refv.commons.validation.support.FixedSnapshotGeneratingValidationSupport;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generator of conformance resources for a {@link PackageReference}.
 */
public class PackageSnapshotGenerator {
    private static Logger log = LoggerFactory.getLogger(PackageSnapshotGenerator.class);
    private ValidationSupportContext supportContext;
    private ValidationSupportChain supportChain;
    private Map<PackageReference, PackageDefinition> knownPackages;
    private Map<PackageReference, IValidationSupport> packageSupport;

    /**
     * Creates a new instance with an existing chain.
     *
     * @param chain chain of validation support modules
     * @param knownPackages all packages that are configured
     * @param packageSupport the support for all packages
     */
    public PackageSnapshotGenerator(ValidationSupportChain chain,
            Map<PackageReference, PackageDefinition> knownPackages,
            Map<PackageReference, IValidationSupport> packageSupport) {
        this.supportChain = chain;
        this.supportContext = new ValidationSupportContext(this.supportChain);
        this.knownPackages = knownPackages;
        this.packageSupport = packageSupport;
    }


    /**
     * Creates a new instance.
     *
     * @param fhirContext current fhir context
     * @param knownPackages all packages that are configured
     * @param packageSupport the support for all packages
     */
    public PackageSnapshotGenerator(FhirContext fhirContext,
            Map<PackageReference, PackageDefinition> knownPackages,
            Map<PackageReference, IValidationSupport> packageSupport) {
        this.supportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext),
                new FixedSnapshotGeneratingValidationSupport(fhirContext)
        );
        this.supportContext = new ValidationSupportContext(this.supportChain);
        this.knownPackages = knownPackages;
        this.packageSupport = packageSupport;
    }


    /**
     * Loads all conformance resources with added snapshot elements for the given package.
     *
     * @param reference package to load resources for
     * @return the resources of the package
     */
    public PackageResources generateResourcesWithSnapshots(PackageReference reference) {
        log.info("Generate snapshots for {}", reference);
        IValidationSupport packageSupport = this.packageSupport.get(reference);
        this.prepareSupportChain(reference, packageSupport);

        List<IBaseResource> resources = packageSupport.fetchAllConformanceResources();
        if (resources == null) {
            log.error("No resources found in package {}", reference);
            return new PackageResources(reference, Collections.emptyList());
        }

        List<MetadataResource> metadataResourceStream =
                resources.stream().map(this::generateSnapshot).collect(Collectors.toList());
        this.removePackageFromSupportChain(reference, packageSupport);

        return new PackageResources(reference, metadataResourceStream);
    }

    private void prepareSupportChain(PackageReference reference, IValidationSupport packageSupport) {
        this.supportChain.addValidationSupport(packageSupport);
        PackageDefinition packageDefinition = this.knownPackages.get(reference);
        if (packageDefinition.getDependencies() == null) {
            return;
        }

        for (PackageReference dependency : packageDefinition.getDependencies()) {
            IValidationSupport dependentSupport = this.packageSupport.get(dependency);
            if (dependentSupport == null) {
                log.error("Dependency {} for package {} not found", dependency, reference);
                continue;
            }
            this.supportChain.addValidationSupport(dependentSupport);
        }
    }

    private void removePackageFromSupportChain(PackageReference reference, IValidationSupport packageSupport) {
        this.supportChain.removeValidationSupport(packageSupport);
        PackageDefinition packageDefinition = this.knownPackages.get(reference);
        if (packageDefinition == null) {
            return;
        }

        List<PackageReference> dependencies = packageDefinition.getDependencies();
        if (dependencies == null) {
            return;
        }

        dependencies.stream().map(this.packageSupport::get)
                .filter(Objects::nonNull)
                .forEach(this.supportChain::removeValidationSupport);
    }

    private MetadataResource generateSnapshot(IBaseResource resource) {
        IBaseResource snapshotResource;
        if (resource instanceof StructureDefinition) {
            snapshotResource = this.supportChain.generateSnapshot(this.supportContext, resource, null, null, null);
        } else {
            snapshotResource = resource;
        }

        if (snapshotResource == null) {
            throw new RuntimeException("Could not generate snapshot for " + ((MetadataResource) resource).getUrl());
        }

        return (MetadataResource) snapshotResource;
    }

}
