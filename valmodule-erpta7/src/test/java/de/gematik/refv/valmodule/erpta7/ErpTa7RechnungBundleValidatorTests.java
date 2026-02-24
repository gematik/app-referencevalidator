/*-
 * #%L
 * valmodule-erpta7
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
package de.gematik.refv.valmodule.erpta7;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import de.gematik.refv.commons.validation.ValidationOptions;
import de.gematik.refv.commons.validation.ValidationResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class ErpTa7RechnungBundleValidatorTests {

    private final ErpTa7RechnungBundleValidator ta7BundleValidator = new ErpTa7RechnungBundleValidator();

    @Test
    @SneakyThrows
    void testValidateBundleConcurrentlyValid() {
        String resourceBody = Files.readString(Paths.get("src/test/resources/GKVSV_PR_TA7_Rechnung_Bundle/1.3/valid/GKVSV_PR_TA7_Rechnung_Bundle-test.xml"));
        ValidationResult result = ta7BundleValidator.validateBundleConcurrently(resourceBody, ValidationOptions.getDefaults());
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @SneakyThrows
    void testValidateBundleConcurrentlyInvalid() {
        String resourceBody = Files.readString(Paths.get("src/test/resources/GKVSV_PR_TA7_Rechnung_Bundle/1.3/invalid/GKVSV_PR_TA7_Rechnung_Bundle-test.xml"));
        ValidationResult result = ta7BundleValidator.validateBundleConcurrently(resourceBody, ValidationOptions.getDefaults());
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @SneakyThrows
    void testMultipleCreationDatesThrowsException() {
        String resourceBody = Files.readString(Paths.get("src/test/resources/exception-multiple-composition-and-multiple-creation-date.xml"));
        ValidationOptions options = ValidationOptions.getDefaults();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ta7BundleValidator.validateBundleConcurrently(resourceBody, options));
    }
}
