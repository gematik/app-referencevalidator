/*-
 * #%L
 * commons
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
package de.gematik.refv.commons.security;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@Slf4j
class ZipSlipProtectTests {

    private File destDir;

    @AfterEach
    void cleanup() {
        if (destDir != null && destDir.exists()) {
            destDir.delete();
        }
    }

    @Test
    void testNewFile() throws IOException {
        destDir = new File("targetDir");
        destDir.mkdirs();
        String currentEntryName = "subdir/file.txt";

        File result = ZipSlipProtect.newFile(destDir, currentEntryName);

        assertNotNull(result);
        assertEquals(new File("targetDir/subdir/file.txt").getCanonicalPath(), result.getCanonicalPath());
    }

    @Test
    void testNewFileOutsideDirThrowsException() {
        destDir = new File("targetDir");
        destDir.mkdirs();
        String currentEntryName = "../../../../etc/passwd";

        assertThrows(SecurityException.class, () -> ZipSlipProtect.newFile(destDir, currentEntryName));
    }
}

