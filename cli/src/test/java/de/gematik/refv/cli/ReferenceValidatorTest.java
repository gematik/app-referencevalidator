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


import de.gematik.refv.SupportedValidationModule;
import de.gematik.refv.ValidationModuleFactory;
import de.gematik.refv.cli.support.TestAppender;
import de.gematik.refv.valmodule.base.ConfigurationBasedValidationModule;
import lombok.SneakyThrows;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

@ExtendWith(MockitoExtension.class)
class ReferenceValidatorTest {

    @Mock
    ValidationModuleFactory factory;

    @Mock
    ConfigurationBasedValidationModule module;

    @SneakyThrows
    @Test
    void testValidationModuleIsCalled() {
        Mockito.lenient().when(factory.createValidationModule(SupportedValidationModule.ERP)).thenReturn(module);

        Path inputFile = Paths.get("src/non-existingfile.xml");
        String moduleCode = SupportedValidationModule.ERP.toString();

        new ReferenceValidator(moduleCode, inputFile, false, false, factory).run();

        Mockito.verify(module).validateFile(inputFile);
    }

    @SneakyThrows
    @Test
    void testUnknownModuleInterruptsExecution() {
        Path inputFile = Paths.get("src/non-existingfile.xml");

        new ReferenceValidator("unknown-module", inputFile, false, false, factory).run();

        Mockito.verifyNoInteractions(factory);
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource( strings = {
            "-m erp -i src/test/resources/erp-test.xml",
            "-m eau -i src/test/resources/eau-test.xml",
            "-m isik1 -i src/test/resources/isik1-test.json",
            "-m isik2 -i src/test/resources/isik2-test.json",
            "-m isip1 -i src/test/resources/isip1-test.json",
            "-m diga -i src/test/resources/diga-test.xml"
    })
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

    @Test
    void testVerboseMode() {
        TestAppender appender = getTestAppender();

        String input = "-m isik1 -i src/test/resources/isik1-test.json -v";
        String [] args = input.split(" ");
        ReferenceValidator.main(args);


        boolean isValid = appender.getLogs().stream().anyMatch(e -> e.getLevel().equals(Level.DEBUG));
        Assertions.assertTrue(isValid,"No debug messages in verbose mode found");
    }

    @Test
    void testOnlyErrorsInOutput() {
        TestAppender appender = getTestAppender();

        String input = "-m isik1 -i src/test/resources/isik1-test-errors.json -e";
        String [] args = input.split(" ");
        ReferenceValidator.main(args);


        boolean isValid = appender.getLogs().stream().noneMatch(l -> l.getMessage().toString().contains("INFORMATION"));
        Assertions.assertTrue(isValid,"Information message found in output - only errors are allowed");
    }
}
