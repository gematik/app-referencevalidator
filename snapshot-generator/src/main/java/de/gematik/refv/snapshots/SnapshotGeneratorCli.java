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
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Command(
        name = "FHIR Snapshots Package Generator (Command Line Interface)",
        description = "Generates snapshots for FHIR packages and dependencies.",
        mixinStandardHelpOptions = true
)
public class SnapshotGeneratorCli implements Runnable {

    @Parameters(index = "0", description = "The path to the folder containing source FHIR packages.")
    private String packageFolderPath;

    @Parameters(index = "1", description = "The output folder where snapshots will be stored.")
    private String outputFolderPath;

    @Option(
            names = "--packages",
            split = ",",
            description = "Comma-separated list of package names to generate snapshots for. If omitted, all source FHIR packages are processed."
    )
    private final List<String> packageNames = Collections.emptyList();

    @Option(
            names = "--tempDir",
            description = "The temporary directory for decompressing the FHIR packages. Default is system temp directory."
    )
    private String tempDir = "";

    @SneakyThrows
    public static void main(String[] args) {
        var cli = new CommandLine(new SnapshotGeneratorCli()).setCaseInsensitiveEnumValuesAllowed(true);
        if(args.length == 0) {
            try(ByteArrayOutputStream out1 = new ByteArrayOutputStream()) {
                PrintWriter out = new PrintWriter(out1);
                cli.usage(out);
                logWithLineBreak(out1.toString());
            }
        }
        else
            cli.execute(args);
    }

    @Override
    public void run() {
        packageFolderPath += File.separator;
        outputFolderPath += File.separator;
        tempDir = tempDir.isEmpty() ? System.getProperty("java.io.tmpdir") + File.separator : tempDir + File.separator;

        SnapshotGenerator snapshotGenerator = new SnapshotGenerator();

        Collection<String> packagesForSnapshotGeneration = packageNames.stream()
                .filter(pkg -> pkg != null && !pkg.trim().isEmpty())
                .collect(Collectors.toList());

        try {
            if (packagesForSnapshotGeneration.isEmpty()) {
                snapshotGenerator.generateSnapshots(packageFolderPath, outputFolderPath, tempDir);
            } else {
                snapshotGenerator.generateSnapshots(packageFolderPath, outputFolderPath, packagesForSnapshotGeneration, tempDir);
            }
        } catch (IOException e) {
            log.error("Something went wrong", e);
        }

    }

    private static void logWithLineBreak(String output) {
        log.info("\r\n{}", output);
    }
}
