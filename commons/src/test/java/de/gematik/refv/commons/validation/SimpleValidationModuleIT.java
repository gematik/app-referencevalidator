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
import java.util.List;

class SimpleValidationModuleIT {

    public static final String VALID_FILE = "simplevalidationmodule.test.patient.valid.xml";
    public static final String VALID_FILE_JSON = "simplevalidationmodule.test.patient.valid.json";
    public static final String INVALID_FILE = "simplevalidationmodule.test.patient.invalid.xml";
    public static final String INVALID_FILE_FOR_PATCH = "simplevalidationmodule.test.patch.patient.invalid.xml";
    private static final SimpleValidationModule module = SimpleValidationModule.createInstance("simple-packages.yaml");

    @Test
    @SneakyThrows
    void testValidInput() {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + VALID_FILE);

        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8));
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
    void testValidateAgainstProfilePassedAsParameter() {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + VALID_FILE);

        String profile = "http://example.gematik.de/fhir/StructureDefinition/patient-patched|1.0.0";
        ValidationOptions options = ValidationOptions.getDefaults();
        options.getProfiles().add(profile);
        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8), options);
        Assertions.assertFalse(result.isValid(), "Resource is valid to the passed profile while it should not be");
    }

    @Test
    @SneakyThrows
    void testPatchWithInvalidInput() {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + INVALID_FILE_FOR_PATCH);

        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8));
        Assertions.assertFalse(result.isValid(), "Profile has not been patched - resource is valid while it shouldn't be");
    }

    @Test
    @SneakyThrows
    void testJsonIsNotAccepted() {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + VALID_FILE_JSON);

        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8));
        Assertions.assertFalse(result.isValid(), "JSON has been accepted while it shouldn't");
    }

    @Test
    @SneakyThrows
    void testJsonIsAcceptedIfValidationOptionIsPassed() {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + VALID_FILE_JSON);

        ValidationOptions options = ValidationOptions.getDefaults();
        options.setAcceptedEncodings(List.of("json"));
        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8), options);
        Assertions.assertTrue(result.isValid(), "JSON has not been accepted while it should be while explicitely set as option");
    }

    @Test
    @SneakyThrows
    void testXmlIsNotAcceptedIfValidationOptionIsPassed() {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + VALID_FILE);

        ValidationOptions options = ValidationOptions.getDefaults();
        options.setAcceptedEncodings(List.of("json"));
        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8), options);
        Assertions.assertFalse(result.isValid(), "XML has been accepted while it shouldn't - explicitely set as option");
    }

    @Test
    void testValidationInitializationException() {
        Assertions.assertThrows(ValidationModuleInitializationException.class, () -> SimpleValidationModule.createInstance("non-existing-yaml.yaml"));
    }

}
