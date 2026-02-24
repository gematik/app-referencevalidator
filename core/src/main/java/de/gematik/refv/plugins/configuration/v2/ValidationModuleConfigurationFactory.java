/*-
 * #%L
 * de.gematik.refv:referencevalidator-lib
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
package de.gematik.refv.plugins.configuration.v2;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.configuration.DependencyList;
import de.gematik.refv.commons.configuration.ProfileConfiguration;
import de.gematik.refv.commons.configuration.SupportedProfileVersions;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.plugins.configuration.FhirPackage;
import de.gematik.refv.plugins.configuration.ValidationModuleConfigurationSupportedProfilesBuilder;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class ValidationModuleConfigurationFactory {

    public static ValidationModuleConfiguration createFrom(
            PluginDefinition pluginDefinition,
            FhirContext context,
            Function<String, InputStream> resourceLoaderFunction)
            throws IOException {
        var supportedProfilesBuilder =
                new ValidationModuleConfigurationSupportedProfilesBuilder(context, resourceLoaderFunction);

        var builder = ValidationModuleConfiguration.builder();
        builder
                .id(pluginDefinition.getId())
                .acceptedEncodings(
                        pluginDefinition.getValidation().getAcceptedEncodings() != null
                                ? pluginDefinition.getValidation().getAcceptedEncodings()
                                : new ArrayList<>())
                .anyExtensionsAllowed(pluginDefinition.getValidation().isAnyExtensionsAllowed())
                .errorOnUnknownProfile(pluginDefinition.getValidation().isErrorOnUnknownProfile())
                .version(pluginDefinition.getVersion())
                .ignoredCodeSystems(
                        pluginDefinition.getValidation().getIgnoredCodeSystems() != null
                                ? pluginDefinition.getValidation().getIgnoredCodeSystems()
                                : new ArrayList<>())
                .ignoredValueSets(
                        pluginDefinition.getValidation().getIgnoredValueSets() != null
                                ? pluginDefinition.getValidation().getIgnoredValueSets()
                                : new ArrayList<>());

        builder.dependencyLists(createDependencyListsSection(pluginDefinition));
        builder.supportedProfiles(
                createSupportedProfilesSection(pluginDefinition, supportedProfilesBuilder));

        return builder.build();
    }

    private static Map<String, SupportedProfileVersions> createSupportedProfilesSection(
            PluginDefinition pluginDefinition,
            ValidationModuleConfigurationSupportedProfilesBuilder supportedProfilesBuilder)
            throws IOException {
        // We do not use SupportedProfileVersions class here, as it is immutable and cannot be
        // iteratively populated in a loop
        Map<String, Map<String, ProfileConfiguration>> supportedProfiles = new HashMap<>();

        for (de.gematik.refv.plugins.configuration.DependencyList dl :
                pluginDefinition.getValidation().getDependencyLists()) {

            var sp =
                    supportedProfilesBuilder.buildSupportedProfilesMap(
                            dl.getFhirPackage(), dl.getCreationDateLocators());

            for (var entry : sp.entrySet()) {
                addNewSupportedProfilesToMap(entry.getKey(), entry.getValue(), supportedProfiles);
            }
        }
        return supportedProfiles.entrySet().stream()
                .collect(
                        Collectors.toMap(Map.Entry::getKey, e -> new SupportedProfileVersions(e.getValue())));
    }

    private static void addNewSupportedProfilesToMap(String profile, Map<String, ProfileConfiguration> newProfileVersions, Map<String, Map<String, ProfileConfiguration>> supportedProfiles) {
        supportedProfiles.compute(profile,
                (p, existingProfileVersions) -> {
                    if (existingProfileVersions == null) {
                        return newProfileVersions;
                    }
                    // Merge existing and new profile versions
                    mergeProfileVersions(existingProfileVersions, newProfileVersions, profile);
                    return existingProfileVersions;
                });
    }

    private static void mergeProfileVersions(Map<String, ProfileConfiguration> existingProfileVersions, Map<String, ProfileConfiguration> newProfileVersions, String profile) {
        for (var newProfileVersion : newProfileVersions.entrySet()) {
            if (!existingProfileVersions.containsKey(newProfileVersion.getKey())) {
                existingProfileVersions.put(newProfileVersion.getKey(), newProfileVersion.getValue());
                continue;
            }

            var existingProfileConfiguration = existingProfileVersions.get(newProfileVersion.getKey());
            var combinedDependencyList = new ArrayList<>(existingProfileConfiguration.getDependencyLists());
            combinedDependencyList.addAll(newProfileVersion.getValue().getDependencyLists());

            // Make sure, creationDateLocator is the same for all dependency lists
            if (!Objects.equals(newProfileVersion.getValue().getCreationDateLocator(), existingProfileConfiguration.getCreationDateLocator()))
                throw new IllegalArgumentException("Multiple creation date locators defined for profile: " + profile + ". Configuration for version-less canonicals can not be created");
            // creationDateLocator is identical for all dependency lists and can be overridden
            existingProfileVersions.put(newProfileVersion.getKey(), new ProfileConfiguration(combinedDependencyList, newProfileVersion.getValue().getCreationDateLocator()));
        }
    }

    private static Map<String, DependencyList> createDependencyListsSection(
            PluginDefinition pluginDefinition) {
        Map<String, DependencyList> dependencyLists = new HashMap<>();
        pluginDefinition.getValidation().getDependencyLists().stream()
                .forEach(
                        dl -> {
                            DependencyList dependencyList = createDependencyListSectionPart(dl);
                            dependencyLists.put(dl.getFhirPackage().toNameAndVersion(), dependencyList);
                        });
        return dependencyLists;
    }

    private static DependencyList createDependencyListSectionPart(
            de.gematik.refv.plugins.configuration.DependencyList dl) {
        List<String> dependencies = new ArrayList<>();
        dependencies.add(dl.getFhirPackage().toFilename());
        if (dl.getDependencies() != null) {
            dependencies.addAll(
                    dl.getDependencies().stream()
                            .map(FhirPackage::toFilename)
                            .collect(Collectors.toList()));
        }
        List<String> distinctDependencies =
                dependencies.stream().distinct().collect(Collectors.toList());
        return new DependencyList(
                dl.getValidFrom(),
                dl.getValidTill(),
                distinctDependencies,
                dl.getValidationMessageTransformations() != null
                        ? dl.getValidationMessageTransformations()
                        : new ArrayList<>());
    }
}
