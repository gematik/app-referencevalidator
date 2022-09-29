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

package de.gematik.refv;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidationModuleFactoryTest {

    @Test
    void testUnknownModuleThrowsIllegalArgumentException() {
        var factory = new ValidationModuleFactory();
        Assertions.assertThrows( IllegalArgumentException.class, () ->
        factory.createValidationModule(SupportedValidationModule.EAU));
    }

    @Test
    @SneakyThrows
    void testErpModuleCanBeInstantiated() {
        var erpModule = new ValidationModuleFactory().createValidationModule(SupportedValidationModule.ERP);
        Assertions.assertEquals(SupportedValidationModule.ERP.toString(), erpModule.getId());
        Assertions.assertNotNull(erpModule.getConfiguration());
    }
}
