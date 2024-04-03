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
package de.gematik.refv.valmodule.erpta7;

import de.gematik.refv.SupportedValidationModule;
import de.gematik.refv.ValidationModuleFactory;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.validation.ProfileValidityPeriodCheckStrategy;
import de.gematik.refv.commons.validation.ValidationMessagesFilter;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.commons.validation.ValidationOptions;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class PerformanceIT
{
    private static final String ERPTA7_TEST_ZIP_FILE  = "10000.zip";
    private static final String ERPTA7_TEST_FILE  = "10000.xml";
    private static final String ERPTA7_EXPECTED_VALIDATION_TIME_LIMIT_IN_MS_PROPERTY = "erpta7-expected-validation-time-in-ms";

    // The value depends on the machine where the tests are executed. It is configured in the CI pipeline
    private static final int ERPTA7_EXPECTED_VALIDATION_TIME_LIMIT_IN_MS = System.getProperty(ERPTA7_EXPECTED_VALIDATION_TIME_LIMIT_IN_MS_PROPERTY) != null ? Integer.parseInt(System.getProperty(ERPTA7_EXPECTED_VALIDATION_TIME_LIMIT_IN_MS_PROPERTY)) : 30000;

    @Test
    void testErpTa7RechnungBundleValidationTimeIsBelowLimit() throws ValidationModuleInitializationException, IOException {
        log.info("JVM Xmx: " + Runtime.getRuntime().maxMemory());
        log.info("Expected maximal heap usage: " + System.getProperty("max-heap-size"));
        log.info("Expected validation time limit: " + ERPTA7_EXPECTED_VALIDATION_TIME_LIMIT_IN_MS + "ms");

        ValidationOptions options = ValidationOptions.getDefaults();
        options.setProfileValidityPeriodCheckStrategy(ProfileValidityPeriodCheckStrategy.IGNORE);
        options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
        ValidationModule erpta7Module = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERPTA7);

        String resource = extractTestResourceFromZip();

        long start = System.currentTimeMillis();
        erpta7Module.validateString(resource, options);
        long end = System.currentTimeMillis();
        long validationTime = end - start;
        log.info("Total validation time: {}ms", validationTime);

        assertThat(validationTime).isLessThan(ERPTA7_EXPECTED_VALIDATION_TIME_LIMIT_IN_MS);
    }

    private String extractTestResourceFromZip() throws IOException {
        InputStream zipInputStream = PerformanceIT.class.getClassLoader().getResourceAsStream(PerformanceIT.ERPTA7_TEST_ZIP_FILE);
        if (zipInputStream == null) {
            throw new FileNotFoundException("Unable to find the test zip file: " + PerformanceIT.ERPTA7_TEST_ZIP_FILE);
        }
        try (ZipInputStream zipStream = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if (entry.getName().equals(ERPTA7_TEST_FILE)) {
                    byte[] bytes = zipStream.readAllBytes();
                    return new String(bytes, StandardCharsets.UTF_8);
                }
            }
        }
        throw new FileNotFoundException("Test file not found in the zip: " + ERPTA7_TEST_FILE);
    }
}
