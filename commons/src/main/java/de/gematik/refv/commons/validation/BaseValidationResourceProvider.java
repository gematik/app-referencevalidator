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

package de.gematik.refv.commons.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.function.Function;

public class BaseValidationResourceProvider implements ValidationModuleResourceProvider {

    private static final String CONFIG_FILE = "config.yaml";

    private final Function<String, InputStream> resourceLoaderFunction;

    public BaseValidationResourceProvider(Function<String, InputStream> resourceLoaderFunction) {
        this.resourceLoaderFunction = resourceLoaderFunction;
    }

    @Override
    public InputStream getPackage(String path) {
        return getResource("package/" + path);
    }

    @Override
    public InputStream getResource(String path) {
        return resourceLoaderFunction.apply(path);
    }

    @Override
    @SneakyThrows
    public ValidationModuleConfiguration getConfiguration() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ValidationModuleConfiguration validationModuleConfiguration;
        InputStream packagesConfigFile = getResource(CONFIG_FILE);
        validationModuleConfiguration = mapper.readValue(packagesConfigFile, ValidationModuleConfiguration.class);
        return validationModuleConfiguration;
    }
}
