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
package de.gematik.refv.plugins.configuration.v1;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.configuration.DependencyList;
import de.gematik.refv.commons.configuration.SupportedProfileVersions;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.plugins.configuration.FhirPackage;
import de.gematik.refv.plugins.configuration.ValidationModuleConfigurationSupportedProfilesBuilder;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class ValidationModuleConfigurationFactory {

    public static ValidationModuleConfiguration createFrom(
            de.gematik.refv.plugins.configuration.v1.PluginDefinition pluginDefinition,
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

        List<String> dependencies = new ArrayList<>();
        dependencies.add(pluginDefinition.getValidation().getFhirPackage().toFilename());
        if (pluginDefinition.getValidation().getDependencies() != null)
            dependencies.addAll(
                    pluginDefinition.getValidation().getDependencies().stream()
                            .map(FhirPackage::toFilename)
                            .collect(Collectors.toList()));
        List<String> distinctDependencies =
                dependencies.stream().distinct().collect(Collectors.toList());

        Map<String, DependencyList> dependencyLists = new HashMap<>();
        DependencyList dependencyList =
                new DependencyList(
                        "",
                        "",
                        distinctDependencies,
                        pluginDefinition.getValidation().getValidationMessageTransformations() != null
                                ? pluginDefinition.getValidation().getValidationMessageTransformations()
                                : new ArrayList<>());
        dependencyLists.put(
                pluginDefinition.getValidation().getFhirPackage().toNameAndVersion(), dependencyList);

        builder.dependencyLists(dependencyLists);

        var supportedProfiles =
                supportedProfilesBuilder.buildSupportedProfilesMap(
                        pluginDefinition.getValidation().getFhirPackage(), new LinkedList<>());

        var supportedProfilesSection =
                supportedProfiles.entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey, e -> new SupportedProfileVersions(e.getValue())));

        builder.supportedProfiles(supportedProfilesSection);

        return builder.build();
    }
}
