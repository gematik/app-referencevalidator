/*-
 * #%L
 * valmodule-erpta7
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
package de.gematik.refv.valmodule.erpta7;

import de.gematik.refv.commons.validation.IntegratedValidationResourceProvider;
import de.gematik.refv.valmodule.base.helper.ValidFolderDetector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
class GKVSV_PR_TA7_Rechnung_BundleIT {

    private static final String DIR = "GKVSV_PR_TA7_Rechnung_Bundle";
    static ErpTa7ValidationModule erpTa7ValidationModule;

    @BeforeAll
    @SneakyThrows
    static void beforeAll() {
        IntegratedValidationResourceProvider resourceProvider = new IntegratedValidationResourceProvider("erpta7");
        erpTa7ValidationModule = new ErpTa7ValidationModule(resourceProvider);
        erpTa7ValidationModule.initialize();
    }


    @TestFactory
    @SneakyThrows
    Stream<DynamicTest> testValidation() {
        return Files.walk(Paths.get(String.format("src/test/resources/%s",DIR))).filter(path -> path.toString().endsWith(String.format(".%s","xml"))).map(
                f -> DynamicTest.dynamicTest(f.toString(),
                        () -> validateFile(f)));
    }

    @SneakyThrows
    protected void validateFile(Path path) {
        var result = erpTa7ValidationModule.validateFile(path);
        boolean isValid = ValidFolderDetector.isInValidFolder(path);
        Assertions.assertEquals(isValid, result.isValid(),result.toString());
    }
}
