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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv;

import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import java.io.File;
import java.util.zip.ZipFile;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidationModuleFactoryTest {

    @Test
    @SneakyThrows
    void testErpModuleCanBeInstantiated() {
        var erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERP);
        Assertions.assertEquals(SupportedValidationModule.ERP.toString(), erpModule.getId());
        Assertions.assertNotNull(erpModule.getConfiguration());
    }

    @Test
    @SneakyThrows
    void testCorrectExceptionIsThrownIfPluginIsCorrupted() {
        assertThrows(ValidationModuleInitializationException.class, () -> new ValidationModuleFactory().createValidationModuleFromPlugin(Plugin.createFromZipFile(new ZipFile("src/test/resources/plugins/not-a-plugin.zip"))));
    }

    @Test
    @SneakyThrows
    void testCreateValidationModuleFromPlugin() {
        File pluginZip = new File("src/test/resources/plugins/minimal-plugin.zip");
        Plugin plugin = Plugin.createFromZipFile(new ZipFile(pluginZip));
        var pluginModule = new ValidationModuleFactory().createValidationModuleFromPlugin(plugin);
        Assertions.assertEquals("minimal", pluginModule.getId());
        Assertions.assertNotNull(pluginModule.getConfiguration());
    }
}
