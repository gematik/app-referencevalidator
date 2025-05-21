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
package de.gematik.refv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipFile;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.yaml.snakeyaml.Yaml;

public class Plugin {
    private static final String CONFIG_FILE = "config.yaml";
    private final ZipFile zipFile;

    private Plugin(ZipFile zipFile) {
        this.zipFile = zipFile;
    }

    @SneakyThrows
    public String getId() {
        try(InputStream packagesConfigFile = getResource(CONFIG_FILE)) {
            Yaml yaml = new Yaml();
            Map<Object, Object> document = yaml.load(packagesConfigFile);
            return document.get("id").toString();
        } finally {
            zipFile.close();
        }
    }

    public static Plugin createFromZipFile(@NonNull ZipFile zipFile) {
        return new Plugin(zipFile);
    }

    @SneakyThrows
    public InputStream getResource(@NonNull String path) {
        try (InputStream is = new FileInputStream(zipFile.getName());
             ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(is)) {
            ZipArchiveEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().contains(path)) {
                    return createEntryInputStream(zipInputStream);
                }
            }
        } finally {
            zipFile.close();
        }
        return null;
    }

    private InputStream createEntryInputStream(@NonNull ZipArchiveInputStream zipInputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }
}
