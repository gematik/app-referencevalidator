package de.gematik.refv.snapshots;

import de.gematik.refv.snapshots.helper.TARGZ;
import de.gematik.refv.snapshots.support.DirectoryComparator;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SnapshotGeneratorTests {

    private static final String CORRECT_PACKAGE = "src/test/resources/package/minimal.example-1.0.0-correct.tgz";
    private static final String OUTPUT_SNAPSHOT_PACKAGES_DIR = "target/generated-snapshots/";
    private static final String SRC_PACKAGES_DIR = "src/test/resources/src-package/";
    public static final String EXCLUDED_PACKAGE = "excluded.package-1.0.0.tgz";
    private static SnapshotGenerator snapshotGenerator;

    @SneakyThrows
    private static String getDecompressDir() {
        return Paths.get(Objects.requireNonNull(SnapshotGenerator.class.getResource("/")).toURI()).getParent().toString() + "/decompressed-packages/";
    }

    @BeforeAll
    static void setUp() {
        snapshotGenerator = new SnapshotGenerator(new LinkedList<>());
    }

    @BeforeEach
    @SneakyThrows
    void beforeTest() {
        FileUtils.deleteDirectory(new File(OUTPUT_SNAPSHOT_PACKAGES_DIR));
    }

    @Test
    @SneakyThrows
    void testGenerateSnapshotsEqual() {
        File correctSnapshotPackage = new File(CORRECT_PACKAGE);
        snapshotGenerator.generateSnapshots(SRC_PACKAGES_DIR, OUTPUT_SNAPSHOT_PACKAGES_DIR, getDecompressDir());
        File generatedSnapshotPackage = new File(OUTPUT_SNAPSHOT_PACKAGES_DIR + "minimal.example-1.0.0.tgz");
        assertTrue(comparePackages(correctSnapshotPackage, generatedSnapshotPackage));
    }

    @Test
    @SneakyThrows
    void testPackagesCanBeExcluded() {
        List<String> excludedPackages = new ArrayList<>();
        excludedPackages.add(EXCLUDED_PACKAGE);
        var snapshotGenerator = new SnapshotGenerator(excludedPackages);
        snapshotGenerator.generateSnapshots(SRC_PACKAGES_DIR, OUTPUT_SNAPSHOT_PACKAGES_DIR, getDecompressDir());
        assertFalse(new File(OUTPUT_SNAPSHOT_PACKAGES_DIR + "excluded.package-1.0.0.tgz").exists());
    }

    @Test
    @SneakyThrows
    void testNoExceptionOnDependenciesWithUpperCase() {
        List<String> excludedPackages = new ArrayList<>();
        var snapshotGenerator = new SnapshotGenerator(excludedPackages);
        snapshotGenerator.generateSnapshots("src/test/resources/src-package-upper-case/", OUTPUT_SNAPSHOT_PACKAGES_DIR, getDecompressDir());
        File generatedSnapshotPackage = new File(OUTPUT_SNAPSHOT_PACKAGES_DIR + "dependencies-in-upper-case-1.0.0.tgz");
        assertTrue(generatedSnapshotPackage.exists(), "No snapshots generated for package with upper-case dependencies");
    }

    @Test
    @SneakyThrows
    void testCleanUpDirectoryWorksBeforeSnapshotGeneration() {
        String path = Paths.get(Objects.requireNonNull(SnapshotGenerator.class.getResource("/")).toURI()).getParent().toString() + "/decompressed-packages/minimal.example-1.0.0/";
        File file = new File(path + "test");
        if(!file.exists()) {
            file.mkdir();
        }
        snapshotGenerator.generateSnapshots(SRC_PACKAGES_DIR, OUTPUT_SNAPSHOT_PACKAGES_DIR, getDecompressDir());
        assertFalse(file.exists());
    }

    private boolean comparePackages(File correctSnapshotPackage, File generatedSnapshotPackage) throws IOException {
        TARGZ.decompress(correctSnapshotPackage.getPath(), new File(OUTPUT_SNAPSHOT_PACKAGES_DIR + "correct"));
        TARGZ.decompress(generatedSnapshotPackage.getPath(), new File(OUTPUT_SNAPSHOT_PACKAGES_DIR + "generated" + "-equal"));
        Path correctPackageDecompressedPath = Path.of(OUTPUT_SNAPSHOT_PACKAGES_DIR + "correct");
        Path generatedPackageDecompressedPath = Path.of(OUTPUT_SNAPSHOT_PACKAGES_DIR + "generated" + "-equal");
        return DirectoryComparator.directoryContentEquals(correctPackageDecompressedPath, generatedPackageDecompressedPath);
    }
}
