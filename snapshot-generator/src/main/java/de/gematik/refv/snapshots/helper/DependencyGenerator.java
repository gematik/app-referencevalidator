package de.gematik.refv.snapshots.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.refv.commons.configuration.PackageReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor
@Getter
public class DependencyGenerator {

    public List<String> generateListOfDependenciesFor(String filename, String packageFolderPath) throws IOException {
        List<String> dependencyList = getDependencyList(filename, packageFolderPath);
        if(!dependencyList.isEmpty()) {
            return dependencyList;
        } else {
            throw new IllegalArgumentException(String.format("Could not generate dependencies for %s", filename));
        }
    }

    private List<String> getDependencyList(String filename, String packageFolderPath) throws IOException {
        List<String> dependencyList = new ArrayList<>();
        dependencyList.add(filename);

        boolean newDependenciesAdded = true;

        while (newDependenciesAdded) {
            List<String> currentDependencyList = new ArrayList<>(dependencyList);
            newDependenciesAdded = false;

            for (String s : currentDependencyList) {
                int initialSize = dependencyList.size();
                getDependenciesFromPackageJson(packageFolderPath, dependencyList, s);
                if (dependencyList.size() > initialSize) {
                    newDependenciesAdded = true;
                }
            }
        }
        return dependencyList;
    }

    private void getDependenciesFromPackageJson(String packageFolderPath, List<String> dependencyList, String filename) throws IOException {
        List<PackageReference> additionalRefs = getDependenciesFor(packageFolderPath + File.separator + filename);
        for (PackageReference pr : additionalRefs) {
            String packageFileName = String.format("%s-%s.tgz", pr.getPackageName(), pr.getPackageVersion());
            if (!dependencyList.contains(packageFileName)) {
                dependencyList.add(packageFileName);
            }
        }
    }

    private List<PackageReference> getDependenciesFor(String tgzFilePath) throws IOException {
        List<PackageReference> dependencies = new ArrayList<>();
        try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(
                new GzipCompressorInputStream(
                        new BufferedInputStream(
                                new FileInputStream(tgzFilePath))))) {
            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                if (entry.getName().equals("package/package.json")) {

                    String jsonContent = new String(tarInputStream.readAllBytes());
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(jsonContent);
                    JsonNode dependenciesNode = jsonNode.get("dependencies");

                    dependenciesNode.fields().forEachRemaining(dependencyEntry -> {
                        String packageName = dependencyEntry.getKey();
                        String version = dependencyEntry.getValue().asText();
                        if (!"hl7.fhir.r4.core".equals(packageName)) {
                            dependencies.add(new PackageReference(packageName, version));
                        }
                    });
                    break;
                }
            }
        }
        return dependencies;
    }
}
