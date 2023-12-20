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
        String srcPackagesDir = "src/test/resources/src-package/";
        String outputSnapshotPackagesDir = "target/generated-snapshots/";
        String excludedPackages = "excluded.package-1.0.0.tgz";
        FileUtils.deleteDirectory(new File(outputSnapshotPackagesDir));

        SnapshotMain.main(new String[] {srcPackagesDir, outputSnapshotPackagesDir, "", excludedPackages});

        File generatedSnapshotPackage = new File(outputSnapshotPackagesDir + "minimal.example-1.0.0.tgz");
        assertTrue(generatedSnapshotPackage.exists());
    }
}
