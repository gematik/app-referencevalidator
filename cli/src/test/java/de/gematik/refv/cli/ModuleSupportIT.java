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

package de.gematik.refv.cli;


import de.gematik.refv.cli.support.TestAppender;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModuleSupportIT {

    @SneakyThrows
    @ParameterizedTest
    @ValueSource( strings = {
            "erp src/test/resources/erp-test.xml",
            "eau src/test/resources/eau-test.xml",
            "core src/test/resources/core-test.xml --profile http://fhir.org/StructureDefinition/Parameters"
    })
    @Execution(ExecutionMode.CONCURRENT)
    void testValidCliInput(String input){
        TestAppender appender = getTestAppender();

        String [] args = input.split(" ");
        ReferenceValidator.main(args);

        boolean isValid = false;
        for (LoggingEvent event : appender.getLogs()) {
            if(event.getMessage().toString().contains("Valid: true"))
                isValid = true;
        }
        Assertions.assertTrue(isValid);
    }

    private static TestAppender getTestAppender() {
        Logger logger = Logger.getRootLogger();
        TestAppender appender = new TestAppender();
        logger.addAppender(appender);
        return appender;
    }
}
