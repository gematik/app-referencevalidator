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

import de.gematik.refv.commons.helper.SimpleValidationModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmptyValuesIT {

    private static SimpleValidationModule validationModule;

    @BeforeAll
    @SneakyThrows
    static void beforeAll() {
        validationModule = SimpleValidationModule.createInstance("simple-packages.yaml");
        validationModule.initialize();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/test/resources/empty-values/invalid/length-zero.xml"
    })
    @SneakyThrows
    void invalidInstances(String path) {
        var result = validationModule.validateFile(path);
        Assertions.assertFalse(result.isValid(), result.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/test/resources/empty-values/valid/line-feed-only.xml",
            "src/test/resources/empty-values/valid/leading-and-trailing-whitespaces.xml",
            "src/test/resources/empty-values/valid/leading-and-trailing-lfs.xml"
    })
    @SneakyThrows
    void validInstances(String path) {
        var result = validationModule.validateFile(path);
        Assertions.assertTrue(result.isValid(), result.toString());
    }

}
