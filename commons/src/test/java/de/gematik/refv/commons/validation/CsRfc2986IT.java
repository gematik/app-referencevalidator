/*
Copyright (c) 2022-2024 gematik GmbH

Licensed under the Apache License, Version 2.0 (the License);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package de.gematik.refv.commons.validation;

import de.gematik.refv.commons.helper.ValidationModuleFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CsRfc2986IT {
    private static ValidationModule validationModule;

    @BeforeAll
    @SneakyThrows
    static void beforeAll() {
        validationModule = ValidationModuleFactory.createInstance("simple");
        validationModule.initialize();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/test/resources/cs-rfc3986/invalid/wrong-uri-as-value.xml",
    })
    @SneakyThrows
    void testIdentifiersWithRfc3986CodeSystemAndNonURIValuesAreInvalid(String path) {
        var result = validationModule.validateFile(path);
        Assertions.assertFalse(result.isValid(), result.toString());
        Assertions.assertTrue(result.getValidationMessages().stream().anyMatch(m -> m.getMessageId().equals("TYPE_SPECIFIC_CHECKS_DT_IDENTIFIER_IETF_SYSTEM_VALUE")), "Expected error not found");
    }
}
