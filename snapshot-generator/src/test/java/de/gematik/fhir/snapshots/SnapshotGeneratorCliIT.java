/*
Copyright (c) 2023 gematik GmbH

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
package de.gematik.fhir.snapshots;

import de.gematik.refv.snapshots.SnapshotGeneratorCli;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotGeneratorCliIT {

    @Test
    @SneakyThrows
    void testSnapshotMainMandatoryArgumentsMissing() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SnapshotGeneratorCli.main(new String[]{}));
    }

    @Test
    @SneakyThrows
    void testSnapshotMain() {
        String srcPackagesDir = "src/test/resources/src-package/";
        String outputSnapshotPackagesDir = "target/generated-snapshots/";
        String excludedPackages = "excluded.package-1.0.0.tgz";
        FileUtils.deleteDirectory(new File(outputSnapshotPackagesDir));

        SnapshotGeneratorCli.main(new String[] {srcPackagesDir, outputSnapshotPackagesDir, "", excludedPackages});

        File generatedSnapshotPackage = new File(outputSnapshotPackagesDir + "minimal.example-1.0.0.tgz");
        assertTrue(generatedSnapshotPackage.exists());
    }
}
