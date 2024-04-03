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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class PluginTests {

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
        String source = "src/test/resources/plugins/minimal-plugin.zip";
        String copy = "src/test/resources/plugins/copy-plugin.zip";
        Files.copy(Path.of(source), Path.of(copy), StandardCopyOption.REPLACE_EXISTING);

        var plugin = Plugin.createFromZipFile(new ZipFile(copy));
        assertNotNull(plugin.getResource("package/simplevalidationmodule.test-1.0.1.tgz"));
        assertTrue(isZipFileDeletable(copy));
    }

    boolean isZipFileDeletable(String path) {
        try {
            Files.delete(Path.of(path));
            return true;
        } catch (IOException e) {
            log.error("The zipFile could not be deleted.", e);
            return false;
        }
    }
}
