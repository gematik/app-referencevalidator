/*-
 * #%L
 * commons
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
package de.gematik.refv.commons.security;

import static com.ibm.icu.impl.Assert.fail;

import de.gematik.refv.commons.helper.ValidationModuleFactory;
import de.gematik.refv.commons.validation.ValidationModule;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cf. <a href="https://en.wikipedia.org/wiki/XML_external_entity_attack#cite_note-3">XML external entity attack</a>
 * Cf. <a href="https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html">XML External Entity Prevention Cheat Sheet</a>
 *
 */
@Slf4j
public class XXETests {
    private static ValidationModule validationModule;

    @BeforeAll
    @SneakyThrows
    static void beforeAll() {
        validationModule = ValidationModuleFactory.createInstance("simple");
        validationModule.initialize();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/test/resources/security/xxe/doctype-system-url.xml",
            "src/test/resources/security/xxe/doctype-system-file.xml",
            "src/test/resources/security/xxe/doctype-internal-entity.xml",
            "src/test/resources/security/xxe/dtd-element.xml"
    })
    @SneakyThrows
    void DTDisNotAllowed(String path) {
        try {
            var result = validationModule.validateFile(path);
            log.debug(result.toString());
            fail("No exception is thrown");
        } catch (IllegalArgumentException e) {
            log.debug("Exception during validation", e);
            assertCorrectSecurityException(e);
        }
    }

    private void assertCorrectSecurityException(Throwable e) {
        Assertions.assertNotNull(e.getCause(), "No security exception is thrown (cause empty)");
        Assertions.assertInstanceOf(SecurityException.class, e.getCause(), "No security exception is thrown (wrong cause type)");
    }
}
