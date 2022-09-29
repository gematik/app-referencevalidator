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

package de.gematik.refv.commons.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.InputStream;


/**
 * Modified from <a href="https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/util/FhirPackagePropertiesHelper.java">https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/util/FhirPackagePropertiesHelper.java</a>
 * Copyright 2022 Deutscher Apothekerverband (DAV), Apache License, Version 2.0
 */
@AllArgsConstructor
public class FhirPackageConfigurationLoader {

    private String configurationFilename;

    public FhirPackageConfigurationLoader() {
        configurationFilename = "packages.yaml";
    }

    public ValidationModuleConfiguration getConfiguration() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ValidationModuleConfiguration validationModuleConfiguration;
        InputStream packagesConfigFile = FhirPackageConfigurationLoader.class.getClassLoader().getResourceAsStream(configurationFilename);
        if(packagesConfigFile == null)
            throw new IOException("File not found: " + configurationFilename);

        validationModuleConfiguration = mapper.readValue(packagesConfigFile, ValidationModuleConfiguration.class);
        return validationModuleConfiguration;
    }

}
