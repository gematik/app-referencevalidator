package de.gematik.refv.snapshots;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import de.gematik.refv.snapshots.helper.DependencyGenerator;
import de.gematik.refv.snapshots.helper.FixedSnapshotGeneratingValidationSupport;
import de.gematik.refv.snapshots.helper.NpmPackageLoader;
import de.gematik.refv.snapshots.helper.TARGZ;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.r4.model.StructureDefinition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class SnapshotGenerator {

    private static final FhirContext fhirContext = FhirContext.forR4();
    private static final JsonFactory jsonfactory = new JsonFactory();
    private final DependencyGenerator dependencyGenerator = new DependencyGenerator();
    private final Map<String, StructureDefinition> currentPatches = new HashMap<>();
    private FixedSnapshotGeneratingValidationSupport snapshotGeneratingValidationSupport;
    private ValidationSupportChain chain;
    private String currentPackageName = "";
    private final List<String> excludedPackages;

    public SnapshotGenerator(List<String> excludedPackages) {
        this.excludedPackages = excludedPackages;
    }

    @SneakyThrows
    private static String getDecompressDir() {
        return Paths.get(Objects.requireNonNull(SnapshotGenerator.class.getResource("/")).toURI()).getParent().toString() + "/decompressed-packages/";
    }

    public void generateSnapshots(String packageFolderPath, String outputFolder) throws IOException {
        File packageFolder = new File(packageFolderPath);
        File[] tgzFiles = packageFolder.listFiles((dir, name) -> name.endsWith(".tgz"));
        if(tgzFiles != null) {
            for (File fhirPackageFile : tgzFiles) {
                log.info("Starting snapshot generation for {}", fhirPackageFile.getName());
                List<String> dependencies = dependencyGenerator.generateListOfDependenciesFor(fhirPackageFile.getName(), packageFolderPath);
                generateSnapshotsAndCompressAsTgz(packageFolderPath, outputFolder, fhirPackageFile.getName(), dependencies);
            }
        } else {
            log.error("No fhir packages found at: {}", packageFolderPath);
        }
    }

    private void generateSnapshotsAndCompressAsTgz(String sourceDir, String outputDir, String filename, List<String> dependencies) throws IOException {
        setupSupportChain(dependencies, sourceDir);
        decompressPackage(sourceDir, filename);
        readStructureDefinitionsFromTgz(sourceDir, filename);
        compressPackage(outputDir);
        log.info("Finished snapshot generation for {}", filename);
    }

    private void setupSupportChain(List<String> dependencies, String sourceDir) throws IOException {
        var npmPackageSupport = new NpmPackageLoader().loadPackagesAndCreatePrePopulatedValidationSupport(fhirContext, dependencies, sourceDir);
        getPatches(dependencies, sourceDir);

        PrePopulatedValidationSupport patchesSupport = new PrePopulatedValidationSupport(fhirContext);

        for (Map.Entry<String, StructureDefinition> entry : currentPatches.entrySet()) {
            log.info("Applying patch for {}", entry.getValue());
            patchesSupport.addResource(entry.getValue());
        }
        IValidationSupport validationSupport = fhirContext.getValidationSupport();
        snapshotGeneratingValidationSupport = new FixedSnapshotGeneratingValidationSupport(fhirContext);
        chain = new ValidationSupportChain(
                patchesSupport,
                npmPackageSupport,
                validationSupport,
                snapshotGeneratingValidationSupport
        );
    }

    private void getPatches(List<String> dependencies, String sourceDir) throws IOException {
        currentPatches.clear();
        for(String currentPackageFilename : dependencies) {
            getPatchesFor(currentPackageFilename.replace(".tgz", ""), sourceDir);
        }
    }

    private void getPatchesFor(String currentPackage, String sourceDir) throws IOException {
        File directory = new File(sourceDir + "patches/" + currentPackage);
        if(directory.exists()){
            File[] directoryListing = directory.listFiles();
            if (directoryListing != null) {
                for (File child : directoryListing) {
                    if (child.getName().endsWith(".json")) {
                        FileInputStream inputStream = new FileInputStream(child);
                        var reader = new InputStreamReader(inputStream);
                        var patch = fhirContext.newJsonParser().parseResource(StructureDefinition.class, reader);
                        currentPatches.put(patch.getUrl(), patch);
                        reader.close();
                    }
                }
            }
        }
    }

    private void decompressPackage(String sourceDir, String fileName) throws IOException {
        currentPackageName = fileName.replaceAll(".tgz", "");
        File decompressDir = new File(getDecompressDir() + currentPackageName);
        FileUtils.deleteDirectory(decompressDir);
        TARGZ.decompress(sourceDir + fileName, decompressDir);
    }

    private void compressPackage(String outputDir) throws IOException {
        if(excludedPackages.contains(currentPackageName + ".tgz")) {
            log.info("The current package '{}' was set to be excluded from the final snapshot packages used for validation.", currentPackageName + ".tgz");
            return;
        }

        Path source = Paths.get(getDecompressDir() + currentPackageName);
        Files.createDirectories(Paths.get(outputDir));
        TARGZ.compress(source, outputDir);
    }

    private void readStructureDefinitionsFromTgz(String sourceDir, String filename) throws IOException {
        if(!excludedPackages.contains(filename)) {
            try (
                    FileInputStream fileInputStream = new FileInputStream(sourceDir + filename);
                    GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fileInputStream);
                    TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream);
                    InputStreamReader inputStreamReader = new InputStreamReader(tarInputStream, StandardCharsets.UTF_8);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
            ) {

                TarArchiveEntry currentEntry = tarInputStream.getNextTarEntry();
                while (currentEntry != null) {

                    String currentEntryName = currentEntry.getName();

                    String jsonString = buildJsonString(bufferedReader);
                    if (hasDifferential(jsonString)) {
                        File destDir = new File(getDecompressDir() + currentPackageName);
                        File newFile = TARGZ.newFile(destDir, currentEntryName);
                        generateSnapshot(jsonString, newFile);
                    }

                    currentEntry = tarInputStream.getNextTarEntry();
                }
            }
        }
    }

    private void generateSnapshot(String jsonString, File newFile) throws IOException {
        StructureDefinition differential = parseResourceFromString(jsonString);

        try (FileWriterWithEncoding fileWriterWithEncoding = new FileWriterWithEncoding(newFile, StandardCharsets.UTF_8)) {

            StructureDefinition snapshot = (StructureDefinition) snapshotGeneratingValidationSupport.generateSnapshot(
                    new ValidationSupportContext(chain), differential, null, null, null);
            logGeneratingSnapshotFor(newFile.getName());
            fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToWriter(snapshot, fileWriterWithEncoding);
        }
    }

    private StructureDefinition parseResourceFromString(String jsonString) {
        IParser newJsonParser = fhirContext.newJsonParser();
        StructureDefinition resource = (StructureDefinition) newJsonParser.parseResource(jsonString);
        return checkForPatch(resource);
    }

    private StructureDefinition checkForPatch(StructureDefinition resource) {
        String key = resource.getUrl();
        return currentPatches.getOrDefault(key, resource);
    }

    private String buildJsonString(BufferedReader bufferedReader) {
        return bufferedReader.lines().collect(Collectors.joining());
    }

    private boolean hasDifferential(String jsonString) {
        try (JsonParser jsonParser = jsonfactory.createParser(jsonString)) {
            jsonParser.nextToken();
            while (jsonParser.hasCurrentToken()) {
                String fieldName = jsonParser.getCurrentName();
                jsonParser.nextToken();

                if ("differential".equals(fieldName)) {
                    return true;
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not parse resource", e);
        }
        return false;
    }

    private void logGeneratingSnapshotFor(String currentFileName) {
        String packageAndProfile = String.format("(%s) %s", currentPackageName, currentFileName.replace("/package/", ""));
        log.info("Generating snapshot for: {}", packageAndProfile);
    }
}
