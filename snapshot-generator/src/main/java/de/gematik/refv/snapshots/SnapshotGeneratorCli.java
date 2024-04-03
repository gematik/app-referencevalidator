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
package de.gematik.refv.snapshots;

import de.gematik.fhir.snapshots.SnapshotGenerator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class SnapshotGeneratorCli {

    @SneakyThrows
    public static void main(String[] args) {
        if(args.length < 2) {
            throw new IllegalArgumentException("Mandatory arguments are missing. (1st mandatory argument: packageFolderPath, 2nd mandatory argument: outputFolderPath)");
        }
        String packageFolderPath = args[0] + File.separator;
        String outputFolderPath = args[1] + File.separator;
        packageFolderPath = packageFolderPath.replace("snapshot-generator/", "");
        outputFolderPath = outputFolderPath.replace("snapshot-generator/", "");

        String decompressDir = "";
        if(args.length > 2)
            decompressDir = args[2];

        SnapshotGenerator snapshotGenerator = new SnapshotGenerator();
        snapshotGenerator.generateSnapshots(packageFolderPath, outputFolderPath, decompressDir);
    }
}
