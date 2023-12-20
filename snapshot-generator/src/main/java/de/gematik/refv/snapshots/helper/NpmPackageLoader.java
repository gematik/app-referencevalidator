package de.gematik.refv.snapshots.helper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import de.gematik.refv.commons.validation.support.CustomNpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;

import java.io.IOException;
import java.util.Collection;

public class NpmPackageLoader {

    public IValidationSupport loadPackagesAndCreatePrePopulatedValidationSupport(FhirContext ctx,
                                                                                 Collection<String> packageFilesToLoad,
                                                                                 String dirPath) throws IOException {
        var result = new ValidationSupportChain();

        for(String p : packageFilesToLoad) {
            CustomNpmPackageValidationSupport npmPackageSupport = new CustomNpmPackageValidationSupport(ctx);
            npmPackageSupport.loadPackageFromPath(dirPath + p);
            result.addValidationSupport(npmPackageSupport);
        }

        return result;
    }
}

