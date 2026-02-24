/*-
 * #%L
 * referencevalidator-lib
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
package de.gematik.refv;

import de.gematik.refv.commons.validation.ValidationModule;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PluginWithMultipleConfigurationsIT {

    private static ValidationModule pluginModule;

    @SneakyThrows
    @BeforeAll
    static void beforeAll() {
        Plugin plugin = Plugin.createFromFolder(Path.of("src/test/resources/plugins/plugin-with-multiple-fhirpkg-lists").toAbsolutePath());
        pluginModule = new ValidationModuleFactory().createValidationModuleFromPlugin(plugin);
    }

    @Test
    @SneakyThrows
    void testConfigurationIsParsedCorrectly() {
        Assertions.assertEquals("multiple-fhirpkg-lists-test", pluginModule.getId());
        Assertions.assertNotNull(pluginModule.getConfiguration());
    }

    @ParameterizedTest()
    @ValueSource(strings = {
            "src/test/resources/plugins/plugin-with-multiple-fhirpkg-lists-testfiles/patient-1-0-0-valid.json",
            "src/test/resources/plugins/plugin-with-multiple-fhirpkg-lists-testfiles/patient-2-0-0-valid.json"
    })
    @SneakyThrows
    void testValidResources(String resourcePath) {
        var result = pluginModule.validateFile(resourcePath);
        Assertions.assertTrue(result.isValid());
    }

    @ParameterizedTest()
    @ValueSource(strings = {
            "src/test/resources/plugins/plugin-with-multiple-fhirpkg-lists-testfiles/patient-1-0-0-after-validity-period.json",
            "src/test/resources/plugins/plugin-with-multiple-fhirpkg-lists-testfiles/patient-1-0-0-before-validity-period.json",
            "src/test/resources/plugins/plugin-with-multiple-fhirpkg-lists-testfiles/patient-2-0-0-before-validity-period.json"
    })
    @SneakyThrows
    void testInvalidResources(String resourcePath) {
        var result = pluginModule.validateFile(resourcePath);
        Assertions.assertFalse(result.isValid());
    }
}
