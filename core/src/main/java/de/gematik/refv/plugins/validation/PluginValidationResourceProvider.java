/*
 * Copyright (c) 2024 gematik GmbH
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

package de.gematik.refv.plugins.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.gematik.refv.commons.configuration.DependencyList;
import de.gematik.refv.commons.validation.BaseValidationResourceProvider;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.plugins.configuration.MalformedPackageDeclarationException;
import de.gematik.refv.plugins.configuration.PluginDefinition;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class PluginValidationResourceProvider extends BaseValidationResourceProvider {

    private static final String CONFIG_FILE = "config.yaml";

    public PluginValidationResourceProvider(Function<String, InputStream> resourceLoaderFunction) {
        super(resourceLoaderFunction);
    }

    @Override
    @SneakyThrows
    public ValidationModuleConfiguration getConfiguration() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        PluginDefinition pluginDefinition;
        InputStream pluginDefinitionFile = getResource(CONFIG_FILE);
        pluginDefinition = mapper.readValue(pluginDefinitionFile, PluginDefinition.class);
        return createValidationModuleConfiguration(pluginDefinition);
    }

    private ValidationModuleConfiguration createValidationModuleConfiguration(PluginDefinition pluginDefinition) throws MalformedPackageDeclarationException {
        ValidationModuleConfiguration validationModuleConfiguration = new ValidationModuleConfiguration();
        validationModuleConfiguration.setId(pluginDefinition.getId());
        validationModuleConfiguration.setAcceptedEncodings(pluginDefinition.getValidation().getAcceptedEncodings());
        validationModuleConfiguration.setAnyExtensionsAllowed(pluginDefinition.getValidation().isAnyExtensionsAllowed());
        validationModuleConfiguration.setErrorOnUnknownProfile(pluginDefinition.getValidation().isErrorOnUnknownProfile());
        validationModuleConfiguration.setVersion(pluginDefinition.getVersion());

        validationModuleConfiguration.setIgnoredCodeSystems(
                pluginDefinition.getValidation().getIgnoredCodeSystems() != null ? pluginDefinition.getValidation().getIgnoredCodeSystems() : new ArrayList<>()
        );
        validationModuleConfiguration.setIgnoredValueSets(
                pluginDefinition.getValidation().getIgnoredValueSets() != null ? pluginDefinition.getValidation().getIgnoredValueSets() : new ArrayList<>()
        );
        validationModuleConfiguration.setAcceptedEncodings(new ArrayList<>());

        List<String> dependencies = new ArrayList<>();
        dependencies.add(pluginDefinition.getValidationFhirPackageAsFilename());
        dependencies.addAll(pluginDefinition.getValidationDependenciesAsFilenames());
        List<String> distinctDependencies = dependencies.stream()
                .distinct()
                .collect(Collectors.toList());

        Map<String, DependencyList> dependencyLists = new HashMap<>();
        DependencyList dependencyList = new DependencyList("", "", distinctDependencies, pluginDefinition.getValidation().getValidationMessageTransformations());
        dependencyLists.put(pluginDefinition.getValidation().getFhirPackage(), dependencyList);
        validationModuleConfiguration.setDependencyLists(dependencyLists);

        return validationModuleConfiguration;
    }
}
