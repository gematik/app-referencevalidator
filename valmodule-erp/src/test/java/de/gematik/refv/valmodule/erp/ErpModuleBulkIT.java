/*
 * Copyright (c) 2022 gematik GmbH
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

package de.gematik.refv.valmodule.erp;

import de.gematik.refv.valmodule.erp.helper.TestErpValidationModuleFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;


class ErpModuleBulkIT {
    private static final Path VALID_BASE_DIR = Paths.get("src/test/resources/bulk/valid");
    private static final Path INVALID_BASE_DIR = Paths.get("src/test/resources/bulk/invalid");

    // Keep the instance static to avoid costly multiple instantiation of FHIR context. No impact on test results
    private static final ErpValidationModule validationModule = TestErpValidationModuleFactory.createInstance();

    @ParameterizedTest
    @MethodSource
    @SneakyThrows
    void validateValidFile(Path path) {
        var result = validationModule
                .validateFile(path);
        Assertions.assertTrue(result.isValid(),result.toString());
    }

    private static Stream<Path> validateValidFile() throws IOException {
        return Files.walk(VALID_BASE_DIR).filter(path -> path.toString().endsWith(".xml"));
    }

    @ParameterizedTest
    @MethodSource
    @SneakyThrows
    void validateInvalidFile(String path) {
        var result = validationModule
                .validateFile(path); // Use method with string parameter to test this path as well
        Assertions.assertFalse(result.isValid(),result.toString());
    }

    private static Stream<String> validateInvalidFile() throws IOException {
        return Files.walk(INVALID_BASE_DIR).map(Path::toString).filter(path -> path.endsWith(".xml"));
    }
}
