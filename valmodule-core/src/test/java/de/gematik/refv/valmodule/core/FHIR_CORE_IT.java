/*-
 * #%L
 * valmodule-core
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
package de.gematik.refv.valmodule.core;

import de.gematik.refv.commons.validation.ValidationOptions;
import de.gematik.refv.valmodule.base.helper.BaseProfileIntegrationTest;
import de.gematik.refv.valmodule.base.helper.ValidFolderDetector;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class FHIR_CORE_IT extends BaseProfileIntegrationTest {

    private static final String DIR = "FHIR_CORE";

    @BeforeAll
    void beforeAll() {
        super.createValidationModule("core");
    }

    @SneakyThrows
    protected void validateFile(Path path) {
        ValidationOptions validationOptions = ValidationOptions.getDefaults();

        Pattern pattern = Pattern.compile("(\\w+)[/\\\\](in)?valid");
        Matcher matcher = pattern.matcher(path.toString());

        if(!matcher.find())
            throw new IllegalStateException("Could not determine resource type from the file path " + path);

        var resourceType = matcher.group(1);
        String profile = "http://hl7.org/fhir/StructureDefinition/" + resourceType;
        validationOptions.setProfiles(List.of(profile));
        var result = validationModule.validateFile(path, validationOptions);
        boolean isValid = ValidFolderDetector.isInValidFolder(path);
        Assertions.assertEquals(isValid, result.isValid(), result.toString());
    }

    @TestFactory
    Stream<DynamicTest> testValidation() {
        return super.testValidationBase(DIR);
    }
}
