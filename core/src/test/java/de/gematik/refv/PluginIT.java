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

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.zip.ZipFile;

class PluginIT {
    @Test
    @SneakyThrows
    void testValidationUsingPlugin() {
        Plugin plugin = Plugin.createFromZipFile(new ZipFile("src/test/resources/plugins/minimal-plugin.zip"));
        var pluginModule = new ValidationModuleFactory().createValidationModuleFromPlugin(plugin);
        Assertions.assertEquals("minimal", pluginModule.getId());
        Assertions.assertNotNull(pluginModule.getConfiguration());

        var result = pluginModule.validateFile("src/test/resources/plugins/simplevalidationmodule.test.patient.valid.json");
        Assertions.assertTrue(result.isValid());
    }
}