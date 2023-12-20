package de.gematik.refv.commons.validation.support;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class CustomNpmPackageValidationSupportTests {

    @Test
    void testLoadPackageFromPath() throws IOException {
        CustomNpmPackageValidationSupport validationSupport = new CustomNpmPackageValidationSupport(FhirContext.forR4());

        validationSupport.loadPackageFromPath("src/test/resources/simple/package/simplevalidationmodule.test-1.0.1.tgz");

        assertTrue(Objects.requireNonNull(validationSupport.fetchAllConformanceResources()).toString().contains("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate"));
    }
}
