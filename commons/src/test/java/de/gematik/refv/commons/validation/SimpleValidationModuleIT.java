/*
 * Copyright (c) 2023 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.refv.commons.validation;

import ca.uhn.fhir.util.ClasspathUtil;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.helper.SimpleValidationModule;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class SimpleValidationModuleIT {

    public static final String VALID_FILE = "simplevalidationmodule.test.patient.valid.xml";
    public static final String INVALID_FILE = "simplevalidationmodule.test.patient.invalid.xml";
    public static final String INVALID_FILE_FOR_PATCH = "simplevalidationmodule.test.patch.patient.invalid.xml";
    private static final SimpleValidationModule module = SimpleValidationModule.createCachingInstance();

    @Test
    @SneakyThrows
    void testValidInput() {
        var m = SimpleValidationModule.createNonCachingInstance(); // Create non caching instance to test another code path
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + VALID_FILE);

        var result = m.validateString(IOUtils.toString(input, StandardCharsets.UTF_8));
        Assertions.assertTrue(result.isValid());
    }

    @Test
    @SneakyThrows
    void testInvalidInput() {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + INVALID_FILE);

        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8));
        Assertions.assertFalse(result.isValid());
    }

    @Test
    @SneakyThrows
    void testPatchWithInvalidInput() {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + INVALID_FILE_FOR_PATCH);

        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8));
        Assertions.assertFalse(result.isValid());
    }

    @Test
    void testValidationInitializationException() {
        Assertions.assertThrows(ValidationModuleInitializationException.class, () -> SimpleValidationModule.createInstance("non-existing-yaml.yaml", ProfileCacheStrategy.CACHE_PROFILES));
    }

}
