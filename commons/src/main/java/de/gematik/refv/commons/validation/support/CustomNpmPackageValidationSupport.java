package de.gematik.refv.commons.validation.support;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.npm.NpmPackage;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class CustomNpmPackageValidationSupport extends PrePopulatedValidationSupport {

    private static final String PACKAGE = "package";

    /**
     * Constructor
     */
    public CustomNpmPackageValidationSupport(@Nonnull FhirContext theFhirContext) {
        super(theFhirContext);
    }

    /**
     * Load an NPM package from a folder of a file system
     * @param dirPath Folder containing extracted NPM package contents
     * @throws IOException
     */
    public void loadPackageFromPath(String dirPath) throws IOException {
        loadPackageFromInputStream(new FileInputStream(dirPath));
    }

    public void loadPackageFromInputStream(InputStream is) throws IOException{
        try(is) {
            NpmPackage pkg = NpmPackage.fromPackage(is);
            if (pkg.getFolders().containsKey(PACKAGE)) {
                loadResourcesFromPackage(pkg);
                loadBinariesFromPackage(pkg);
            }
        }
    }

    private void loadResourcesFromPackage(NpmPackage thePackage) {
        NpmPackage.NpmPackageFolder packageFolder = thePackage.getFolders().get(PACKAGE);

        for (String nextFile : packageFolder.listFiles()) {
            if (nextFile.toLowerCase(Locale.US).endsWith(".json")) {
                String input = new String(packageFolder.getContent().get(nextFile), StandardCharsets.UTF_8);
                IBaseResource resource = getFhirContext().newJsonParser().parseResource(input);
                super.addResource(resource);
            }
        }
    }

    private void loadBinariesFromPackage(NpmPackage thePackage) throws IOException {
        List<String> binaries = thePackage.list("other");
        for (String binaryName : binaries) {
            addBinary(TextFile.streamToBytes(thePackage.load("other", binaryName)), binaryName);
        }
    }
}
