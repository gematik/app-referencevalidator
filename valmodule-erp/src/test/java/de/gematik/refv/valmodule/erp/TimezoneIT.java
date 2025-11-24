/*-
 * #%L
 * valmodule-erp
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
package de.gematik.refv.valmodule.erp;

import de.gematik.refv.commons.validation.IntegratedValidationModule;
import de.gematik.refv.valmodule.base.helper.TestConfigurationBasedValidationModuleFactory;
import de.gematik.refv.valmodule.base.helper.ValidFolderDetector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
class TimezoneIT {

    protected IntegratedValidationModule validationModule = TestConfigurationBasedValidationModuleFactory.createInstance("erp");

    private static final String DIR = "Timezones";

    private static final TimeZone originalSystemTimezone = TimeZone.getDefault();
    @AfterAll
    public static void afterAll() {
        TimeZone.setDefault(originalSystemTimezone);
    }

    @TestFactory
    @SneakyThrows
    protected Stream<DynamicTest> testValidation() {

        var allFiles = Files.walk(Paths.get(String.format("src/test/resources/%s",DIR))).filter(path -> path.toString().endsWith(String.format(".%s","xml"))).collect(Collectors.toList());
        var timeZones = List.of(
                "Asia/Taipei"
                ,"Europe/Berlin"
                ,"America/Nome"
        );

        var allDynamicTests = new LinkedList<DynamicTest>();
        for (var file : allFiles) {
            for (var timezone : timeZones) {
                allDynamicTests.add(DynamicTest.dynamicTest(file.toString() + " in " + timezone,
                        () -> validateFile(file, timezone)));
            }
        }
        return allDynamicTests.stream();
    }

    @SneakyThrows
    protected void validateFile(Path path, String timezone) {
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));

        var result = validationModule.validateFile(path);
        boolean isValid = ValidFolderDetector.isInValidFolder(path);
        Assertions.assertEquals(isValid, result.isValid(),String.format("Invalid file in Timezone %s: %s;\r\n%s", timezone, path, result));
    }
}
