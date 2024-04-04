/*
Copyright (c) 2022-2024 gematik GmbH

Licensed under the Apache License, Version 2.0 (the License);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package de.gematik.refv;

import de.gematik.refv.commons.validation.ValidationModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.zip.ZipFile;

class PluginIT {

    private static ValidationModule pluginModule;

    @SneakyThrows
    @BeforeAll
    static void beforeAll() {
        Plugin plugin = Plugin.createFromZipFile(new ZipFile("src/test/resources/plugins/minimal-plugin.zip"));
        pluginModule = new ValidationModuleFactory().createValidationModuleFromPlugin(plugin);
        Assertions.assertEquals("minimal", pluginModule.getId());
        Assertions.assertNotNull(pluginModule.getConfiguration());
    }
    @ParameterizedTest()
    @ValueSource(strings = {
            "src/test/resources/plugins/simplevalidationmodule.test-multiple-profiles.patient.valid.json",
            "src/test/resources/plugins/simplevalidationmodule.test.patient.valid.json",
            "src/test/resources/plugins/simplevalidationmodule.test-profile-without-version.patient.valid.json"
    })
    @SneakyThrows
    void testValidationUsingPlugin(String resourcePath) {
        var result = pluginModule.validateFile(resourcePath);
        Assertions.assertTrue(result.isValid());
    }
}
