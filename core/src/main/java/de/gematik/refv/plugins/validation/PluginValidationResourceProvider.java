/*-
 * #%L
 * referencevalidator-lib
 * %%
 * Copyright (C) 2025 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.plugins.validation;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.validation.BaseValidationResourceProvider;
import de.gematik.refv.plugins.configuration.v1.ValidationModuleConfigurationFactory;
import de.gematik.refv.plugins.configuration.v2.PluginDefinition;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class PluginValidationResourceProvider extends BaseValidationResourceProvider {

    private static final String CONFIG_FILE = "config.yaml";
    private final Function<String, InputStream> resourceLoaderFunction;
    private final FhirContext fhirContext;
    @java.lang.SuppressWarnings({"squid:S3077"}) // Reason for usage of volatile https://stackoverflow.com/a/7855774
    private volatile ValidationModuleConfiguration cachedConfiguration;
    private final Object lockObject = new Object();

    public PluginValidationResourceProvider(Function<String, InputStream> resourceLoaderFunction, FhirContext fhirContext) {
        super(resourceLoaderFunction);
        this.resourceLoaderFunction = resourceLoaderFunction;
        this.fhirContext = fhirContext;
    }

    @Override
    @SneakyThrows
    public ValidationModuleConfiguration getConfiguration() {
        if(cachedConfiguration == null) {
            // Double-checked locking pattern, cf. https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
            synchronized (lockObject) {
                if (cachedConfiguration == null) {
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    try (InputStream pluginDefinitionFile = getResource(CONFIG_FILE)) {
                        String yamlContent = new String(pluginDefinitionFile.readAllBytes());

                        var jsonMap = mapper.readValue(yamlContent, Map.class);
                        var specVersion = jsonMap.get("configSpecVersion");
                        if(specVersion == null)
                            throw new IllegalArgumentException("Could not determine config file spec version");

                        if(specVersion.toString().startsWith("1.")) {
                            var pluginDefinitionV1 = mapper.readValue(yamlContent, de.gematik.refv.plugins.configuration.v1.PluginDefinition.class);
                            cachedConfiguration = ValidationModuleConfigurationFactory.createFrom(pluginDefinitionV1, fhirContext, resourceLoaderFunction);
                        }
                        else if(specVersion.toString().startsWith("2.")) {
                            var pluginDefinitionV2 = mapper.readValue(yamlContent, PluginDefinition.class);
                            cachedConfiguration = de.gematik.refv.plugins.configuration.v2.ValidationModuleConfigurationFactory.createFrom(pluginDefinitionV2, fhirContext, resourceLoaderFunction);
                        }
                        else
                            throw new IllegalArgumentException("Unsupported config file spec version: " + specVersion);
                    }
                }
            }
        }
        return cachedConfiguration;
    }
}
