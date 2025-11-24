/*-
 * #%L
 * snapshot-generator
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
package de.gematik.fhir.snapshots;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.refv.snapshots.SnapshotGeneratorCli;
import java.io.File;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

class SnapshotGeneratorCliIT {

    @Test
    @SneakyThrows
    void testSnapshotMain() {
        String srcPackagesDir = "src/test/resources/src-package/";
        String outputSnapshotPackagesDir = "target/generated-snapshots/";
        FileUtils.deleteDirectory(new File(outputSnapshotPackagesDir));

        SnapshotGeneratorCli.main(new String[] {srcPackagesDir, outputSnapshotPackagesDir});

        File generatedSnapshotPackage1 = new File(outputSnapshotPackagesDir + "simplevalidationmodule.test-1.0.0.tgz");
        File generatedSnapshotPackage2 = new File(outputSnapshotPackagesDir + "simplevalidationmodule.test-1.0.1.tgz");

        assertTrue(generatedSnapshotPackage1.exists());
        assertTrue(generatedSnapshotPackage2.exists());
    }

    @Test
    @SneakyThrows
    void testSnapshotMainWithOptionalArguments() {
        String srcPackagesDir = "src/test/resources/src-package/";
        String outputSnapshotPackagesDir = "target/generated-snapshots/";
        String decompressDir = "target/temp-decompress/";
        String packageNames = "simplevalidationmodule.test-1.0.0.tgz";

        FileUtils.deleteDirectory(new File(outputSnapshotPackagesDir));
        FileUtils.deleteDirectory(new File(decompressDir));

        SnapshotGeneratorCli.main(new String[] {
                srcPackagesDir,
                outputSnapshotPackagesDir,
                "--packages=" + packageNames,
                "--tempDir=" + decompressDir
        });

        File generatedSnapshotPackage = new File(outputSnapshotPackagesDir + "simplevalidationmodule.test-1.0.0.tgz");
        assertTrue(generatedSnapshotPackage.exists(), "The snapshot package should be generated");

        File anotherPackage = new File(outputSnapshotPackagesDir + "simplevalidationmodule.test-1.0.1.tgz");
        assertFalse(anotherPackage.exists(), "The dependency package should NOT be generated");

        File decompressDirFile = new File(decompressDir);
        assertTrue(decompressDirFile.exists() && decompressDirFile.isDirectory(), "The decompress directory should exist");
    }

    @Test
    @SneakyThrows
    void testSnapshotMainWithEmptyStringAsPackageNameShouldGenerateAllSnapshotPackages() {
        String srcPackagesDir = "src/test/resources/src-package/";
        String outputSnapshotPackagesDir = "target/generated-snapshots/";
        String packageNames = "";

        FileUtils.deleteDirectory(new File(outputSnapshotPackagesDir));

        SnapshotGeneratorCli.main(new String[] {
                srcPackagesDir,
                outputSnapshotPackagesDir,
                "--packages=" + packageNames
        });

        File generatedSnapshotPackage1 = new File(outputSnapshotPackagesDir + "simplevalidationmodule.test-1.0.0.tgz");
        File generatedSnapshotPackage2 = new File(outputSnapshotPackagesDir + "simplevalidationmodule.test-1.0.1.tgz");

        assertTrue(generatedSnapshotPackage1.exists());
        assertTrue(generatedSnapshotPackage2.exists());
    }

    @Test
    @SneakyThrows
    void testSnapshotMainWithMissingRequiredArguments() {
        String outputSnapshotPackagesDir = "target/generated-snapshots/";
        FileUtils.deleteDirectory(new File(outputSnapshotPackagesDir));

        SnapshotGeneratorCli.main(new String[] {});

        File generatedSnapshotPackage1 = new File(outputSnapshotPackagesDir + "simplevalidationmodule.test-1.0.0.tgz");
        File generatedSnapshotPackage2 = new File(outputSnapshotPackagesDir + "simplevalidationmodule.test-1.0.1.tgz");
        File outputDir = new File(outputSnapshotPackagesDir);

        assertThat(generatedSnapshotPackage1).doesNotExist();
        assertThat(generatedSnapshotPackage2).doesNotExist();
        assertThat(outputDir).doesNotExist();
    }
}
