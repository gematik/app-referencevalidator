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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipFile;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class PluginTests {

    @Test
    @SneakyThrows
    void testCreateFromFolder() {
        Plugin plugin = Plugin.createFromFolder(Path.of("src/test/resources/plugins/plugin-with-multiple-fhirpkg-lists").toAbsolutePath());
        Assertions.assertEquals("multiple-fhirpkg-lists-test", plugin.getId());
        assertNotNull(plugin.getResource("package/simplevalidationmodule.test-1.0.1.tgz"));
        assertNotNull(plugin.getResource("config.yaml"));
        assertNull(plugin.getResource("non-existing.file"));
    }

    @Test
    @SneakyThrows
    void testCreateFromZipFile() {
        var plugin = Plugin.createFromZipFile(new ZipFile("src/test/resources/plugins/minimal-plugin.zip"));
        assertEquals("minimal", plugin.getId());
        assertNotNull(plugin.getResource("package/simplevalidationmodule.test-1.0.1.tgz"));
        assertNotNull(plugin.getResource("config.yaml"));
        assertNull(plugin.getResource("non-existing.file"));
    }

    @Test
    @SneakyThrows
    void testZipFileClosed() {
        Path source = Path.of("src/test/resources/plugins/minimal-plugin.zip");
        Path copy = Path.of("target/PluginTests/testZipFileClosed.zip");
        if(!Files.exists(copy.getParent()))
            Files.createDirectories(copy.getParent());
        Files.copy(source, copy, StandardCopyOption.REPLACE_EXISTING);

        var plugin = Plugin.createFromZipFile(new ZipFile(copy.toFile()));
        assertNotNull(plugin.getResource("package/simplevalidationmodule.test-1.0.1.tgz"));
        assertTrue(isZipFileDeletable(copy));
    }

    boolean isZipFileDeletable(Path path) {
        try {
            Files.delete(path);
            return true;
        } catch (IOException e) {
            log.error("The zipFile could not be deleted.", e);
            return false;
        }
    }
}
