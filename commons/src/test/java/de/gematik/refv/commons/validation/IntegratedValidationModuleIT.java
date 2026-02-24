/*-
 * #%L
 * commons
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * *******
 * 
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.commons.validation;

import ca.uhn.fhir.util.ClasspathUtil;
import de.gematik.refv.commons.helper.ValidationModuleFactory;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

class IntegratedValidationModuleIT {

    public static final String VALID_FILE = "simplevalidationmodule.test.patient.valid.xml";
    public static final String VALID_FILE_JSON = "simplevalidationmodule.test.patient.valid.json";
    public static final String INVALID_FILE = "simplevalidationmodule.test.patient.invalid.xml";
    private static final ValidationModule module = ValidationModuleFactory.createInstance("simple");

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

    @ParameterizedTest()
    @ValueSource(
        strings = {
                "simplevalidationmodule.test.patient-1-0-0.valid.without-profile-version.xml",
                "simplevalidationmodule.test.patient-2-0-0.valid.without-profile-version.xml"
        }
    )
    @SneakyThrows
    void testValidIfNoProfileVersionIsGivenInMetaProfileAndMultipleDependencyListsAreDefined(String filename) {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + filename);
        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8));
        Assertions.assertTrue(result.isValid());
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

    /**
     * An XML-comment in front of a resource leads to anomalous behavior while validating terminologies.
     * E.g. if some code doesn't come from the "preferred" ValueSet, misleading validation messages occur.
     * This seems to be a bug in the current version of HAPI (6.6.2)
     * A comment-stripping mechanism in the reference validator ensures, that these messages get suppressed.
     */
    @Test
    @SneakyThrows
    void testXmlCommentsDoNotProduceMisleadingValidationIssues() {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + "simplevalidationmodule.test.patient.valid.with-xml-comments.xml");

        ValidationOptions options = ValidationOptions.getDefaults();
        options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8), options);
        Assertions.assertFalse(result.getValidationMessages().stream().anyMatch(m -> m.getMessageId().equals("Terminology_TX_NoValid_3_CC")), "Misleading validation messages detected while there should be none");
    }

    @Test
    @SneakyThrows
    void testProfileFilterWhichDoesntMatchAnyMetaProfileReference() {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + VALID_FILE);

        ValidationOptions options = ValidationOptions.getDefaults();
        options.setProfileFilterRegex("non-matching-pattern");
        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8), options);
        Assertions.assertFalse(result.isValid(),"Resource is valid while the profile filter doesn't match any meta.profile reference");
    }

    @Test
    @SneakyThrows
    void testProfileFilterWhichMatcheAtLeastOneMetaProfileReference() {
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + VALID_FILE);

        ValidationOptions options = ValidationOptions.getDefaults();
        options.setProfileFilterRegex("patient|birthdate\\|1\\.0\\.\\d");
        var result = module.validateString(IOUtils.toString(input, StandardCharsets.UTF_8), options);
        Assertions.assertTrue(result.isValid(),"Resource is invalid while the profile filter matches one meta.profile reference");
    }

}
