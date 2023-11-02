package de.gematik.refv.snapshots;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotMainTests {

    @Test
    @SneakyThrows
    void testSnapshotMainMandatoryArgumentsMissing() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SnapshotMain.main(new String[]{}));
    }

    @Test
    @SneakyThrows
    void testSnapshotMain() {
        String SRC_PACKAGES_DIR = "src/test/resources/src-package/";
        String OUTPUT_SNAPSHOT_PACKAGES_DIR = "target/generated-snapshots/";
        String EXCLUDED_PACKAGE = "excluded.package-1.0.0.tgz";
        FileUtils.deleteDirectory(new File(OUTPUT_SNAPSHOT_PACKAGES_DIR));

        SnapshotMain.main(new String[] {SRC_PACKAGES_DIR, OUTPUT_SNAPSHOT_PACKAGES_DIR, EXCLUDED_PACKAGE});

        File generatedSnapshotPackage = new File(OUTPUT_SNAPSHOT_PACKAGES_DIR + "minimal.example-1.0.0.tgz");
        assertTrue(generatedSnapshotPackage.exists());
    }
}
