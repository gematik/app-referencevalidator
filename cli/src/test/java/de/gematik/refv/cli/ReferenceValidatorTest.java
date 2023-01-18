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
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.valmodule.erp.ErpValidationModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@ExtendWith(MockitoExtension.class)
class ReferenceValidatorTest {

    @Mock
    ValidationModuleFactory factory;

    @Mock
    ErpValidationModule module;


    @SneakyThrows
    @Test
    void testValidationModuleIsCalled() {
        Mockito.lenient().when(factory.createValidationModule(SupportedValidationModule.ERP)).thenReturn(module);

        Path inputFile = Paths.get("src/non-existingfile.xml");
        String moduleCode = SupportedValidationModule.ERP.toString();

        new ReferenceValidator(moduleCode, inputFile, factory).run();

        Mockito.verify(module).validateFile(inputFile);
    }

    @SneakyThrows
    @Test
    void testUnknownModuleInterruptsExecution() {
        Path inputFile = Paths.get("src/non-existingfile.xml");
        String moduleCode = SupportedValidationModule.EAU.toString();

        new ReferenceValidator(moduleCode, inputFile, factory).run();

        Mockito.verifyNoInteractions(factory);
    }
}
