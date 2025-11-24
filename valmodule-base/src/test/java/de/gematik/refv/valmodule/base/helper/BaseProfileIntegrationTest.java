/*-
 * #%L
 * valmodule-base
 * %%
 * Copyright (C) 2025 gematik GmbH
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
package de.gematik.refv.valmodule.base.helper;

import de.gematik.refv.commons.validation.IntegratedValidationModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseProfileIntegrationTest {

    protected IntegratedValidationModule validationModule;

    protected void createValidationModule(String module) {
        this.validationModule = TestConfigurationBasedValidationModuleFactory.createInstance(module);
    }

    @SneakyThrows
    protected void validateFile(Path path) {
        var result = validationModule.validateFile(path);
        boolean isValid = ValidFolderDetector.isInValidFolder(path);
        Assertions.assertEquals(isValid, result.isValid(),result.toString());
    }

    @SneakyThrows
    protected Stream<DynamicTest> testValidationBase(String folder) {
        return testValidationBase(folder, "xml");
    }

    @SneakyThrows
    protected Stream<DynamicTest> testValidationBase(String folder, String fileExtension) {
        return Files.walk(Paths.get(String.format("src/test/resources/%s",folder))).filter(path -> path.toString().endsWith(String.format(".%s",fileExtension))).map(
                f -> DynamicTest.dynamicTest(f.toString(),
                        () -> validateFile(f)));
    }
}
