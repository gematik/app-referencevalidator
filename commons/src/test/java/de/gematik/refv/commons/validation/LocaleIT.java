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
package de.gematik.refv.commons.validation;

import de.gematik.refv.commons.helper.ValidationModuleFactory;
import java.util.Locale;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LocaleIT {

    @ParameterizedTest
    @CsvSource({
            "en-US,src/test/resources/locale/ignored-value-set.xml",
            "de-DE,src/test/resources/locale/ignored-value-set.xml",
            "en_US,src/test/resources/locale/ignored-code-system.xml",
            "de_DE,src/test/resources/locale/ignored-code-system.xml"
    })
    @SneakyThrows
    void validInstances(String locale, String path) {
        Locale.setDefault(Locale.forLanguageTag(locale));

        ValidationModule validationModule = ValidationModuleFactory.createInstance("simple");
        validationModule.initialize();

        var result = validationModule.validateFile(path);
        Assertions.assertTrue(result.isValid(), result.toString());
    }

}
