package de.gematik.refv.snapshots.helper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
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

public class NpmLocalPackageValidationSupport extends PrePopulatedValidationSupport {

    /**
     * Constructor
     */
    public NpmLocalPackageValidationSupport(@Nonnull FhirContext theFhirContext) {
        super(theFhirContext);
    }

    /**
     * Load an NPM package using a classpath specification, e.g. <code>/path/to/resource/my_package.tgz</code>. The
     * classpath spec can optionally be prefixed with the string <code>classpath:</code>
     *
     * @throws InternalErrorException If the classpath file can't be found
     */
    public void loadPackageFromPath(String dirPath) throws IOException {
        //load package from absoulte path here
        try (InputStream is = new FileInputStream(dirPath)) {
            NpmPackage pkg = NpmPackage.fromPackage(is);
            if (pkg.getFolders().containsKey("package")) {
                loadResourcesFromPackage(pkg);
                loadBinariesFromPackage(pkg);
            }
        }
    }

    private void loadResourcesFromPackage(NpmPackage thePackage) {
        NpmPackage.NpmPackageFolder packageFolder = thePackage.getFolders().get("package");

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


