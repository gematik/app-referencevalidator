/*-
 * #%L
 * commons
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.commons.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import java.io.InputStream;
import java.util.function.Function;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseValidationResourceProvider implements ValidationModuleResourceProvider {
    private static final String CONFIG_FILE = "config.yaml";
    private final Function<String, InputStream> resourceLoaderFunction;
    @java.lang.SuppressWarnings({"squid:S3077"}) // Reason for usage of volatile https://stackoverflow.com/a/7855774
    private volatile ValidationModuleConfiguration cachedConfiguration;

    private final Object lockObject = new Object();

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
        if(cachedConfiguration == null) {
            // Double-checked locking pattern, cf. https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
            synchronized (lockObject) {
                if(cachedConfiguration == null) {
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    try (InputStream packagesConfigFile = getResource(CONFIG_FILE)) {
                        cachedConfiguration = mapper.readValue(packagesConfigFile, ValidationModuleConfiguration.class);
                    }
                }
            }
        }
        return cachedConfiguration;
    }
}
