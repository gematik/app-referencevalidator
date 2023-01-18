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

package de.gematik.refv.valmodule.erp;

import de.gematik.refv.valmodule.erp.helper.TestErpValidationModuleFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

class ErpModuleLocalTest {
    private final ErpValidationModule validationModule = TestErpValidationModuleFactory.createInstance();

    @Test
    @SneakyThrows
    void testManual() {
        Path path = Paths.get("src/test/resources/KBV_PR_ERP_Bundle/1.0.1/valid/gematik/AOK_NO_109519005_Kostbarkeit_03510090.xml");
        var result = validationModule.validateFile(path);
        Assertions.assertTrue(result.isValid());
    }
}
