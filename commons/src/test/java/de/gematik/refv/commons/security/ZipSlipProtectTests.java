package de.gematik.refv.commons.security;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

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

