package de.gematik.refv.snapshots;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class SnapshotMain {

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

        List<String> excludedPackages = new ArrayList<>();
        if(args.length > 3)
            excludedPackages = Arrays.asList(args[3].split(" "));

        SnapshotGenerator snapshotGenerator = new SnapshotGenerator(excludedPackages);
        snapshotGenerator.generateSnapshots(packageFolderPath, outputFolderPath, decompressDir);
    }
}
