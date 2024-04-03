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
package de.gematik.refv.valmodule.erp;

import de.gematik.refv.SupportedValidationModule;
import de.gematik.refv.ValidationModuleFactory;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.validation.ProfileValidityPeriodCheckStrategy;
import de.gematik.refv.commons.validation.ValidationMessagesFilter;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.commons.validation.ValidationOptions;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class PerformanceIT
{
    private static final String TEST_FILE  = "Beispiel_1.xml";

    private static final String EXPECTED_VALIDATION_TIME_LIMIT_IN_MS_PROPERTY  = "expected-validation-time-in-ms";


    // The value depends on the machine where the tests are executed. It is configured in the CI pipeline
    private static final int EXPECTED_VALIDATION_TIME_LIMIT_IN_MS = System.getProperty(EXPECTED_VALIDATION_TIME_LIMIT_IN_MS_PROPERTY) != null ? Integer.parseInt(System.getProperty(EXPECTED_VALIDATION_TIME_LIMIT_IN_MS_PROPERTY)) : 65;

    /**
     * There is an additional check on memory consumption of this test in the maven build pipeline (cf. maven-failsafe-plugin configuration)
     * The test should crash if the validation procedure consumes more memory than expected and an OutOfMemoryError occurs.
     */
    @Test
    void testValidationTimeIsBelowLimit() throws ValidationModuleInitializationException, IOException {
        log.info("JVM Xmx: " + Runtime.getRuntime().maxMemory());
        log.info("Expected maximal heap usage: " + System.getProperty("max-heap-size"));
        log.info("Expected validation time: " + EXPECTED_VALIDATION_TIME_LIMIT_IN_MS + "ms");

        ValidationOptions options = ValidationOptions.getDefaults();
        options.setProfileValidityPeriodCheckStrategy(ProfileValidityPeriodCheckStrategy.IGNORE);
        options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
        ValidationModule erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERP);
        var lastValidationResults = new LimitedQueue<Long>(50);
        String resource = new String(PerformanceIT.class.getClassLoader().getResourceAsStream(TEST_FILE).readAllBytes(), StandardCharsets.UTF_8);

        long averageValidationTime = 1000;

        for(int i = 0; i < 1000; i++) {
                var time = System.currentTimeMillis();
                erpModule.validateString(resource, options);
                lastValidationResults.add(System.currentTimeMillis() - time);
            averageValidationTime = lastValidationResults.stream().reduce((long) 0, Long::sum) / lastValidationResults.size();
            log.debug("Validation Nr. " + i + ": average validation time: " + averageValidationTime);
        }

        assertThat(averageValidationTime).isLessThan(EXPECTED_VALIDATION_TIME_LIMIT_IN_MS);
    }

    public static class LimitedQueue<E> extends LinkedList<E> {

        private final int limit;

        public LimitedQueue(int limit) {
            this.limit = limit;
        }

        @Override
        public boolean add(E o) {
            boolean added = super.add(o);
            while (added && size() > limit) {
                super.remove();
            }
            return added;
        }
    }
}
