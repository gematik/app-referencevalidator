/*-
 * #%L
 * commons
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.commons.validation;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.refv.commons.exceptions.UnsupportedProfileException;
import de.gematik.refv.commons.helper.ValidationModuleFactory;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@Slf4j
class ProfileForValidationSelectionIT {

    private static ValidationModule validationModule;
    private static ValidationOptions options;

    @BeforeAll
    @SneakyThrows
    static void beforeAll() {
        validationModule = ValidationModuleFactory.createInstance("simple");
        validationModule.initialize();
        options = ValidationOptions.getDefaults();
        options.setAcceptedEncodings(List.of("xml","json"));
    }

    @Nested
    @DisplayName("No profile is passed as parameter ")
    class ValidationWithoutProfilePassedAsParameter {

        @Nested
        @DisplayName("meta.profile references at least one profile")
        class ValidationWithReferencedMetaProfile {
            @Test
            @SneakyThrows
            @DisplayName("the profile is one and supported --> valid")
            void testProfileIsReferencedAndSupported() {
                var result = validationModule.validateFile("src/test/resources/profile-for-validation-selection/meta-profile-with-one-supported-profile.json", options);
                assertThat(result.isValid()).isTrue();
            }

            @Test
            @SneakyThrows
            @DisplayName("multiple supported profiles --> valid")
            void testMultipleReferencedProfilesAndAllSupported() {
                var result = validationModule.validateFile("src/test/resources/profile-for-validation-selection/meta-profile-with-multuple-supported-profiles.json", options);
                Assertions.assertTrue(result.isValid());
            }

            @Test
            @SneakyThrows
            @DisplayName("the profile is one and unsupported --> UnsupportedProfileException")
            void testUnknownProfile() {
                Assertions.assertThrows(UnsupportedProfileException.class, () -> validationModule.validateFile("src/test/resources/profile-for-validation-selection/meta-profile-with-one-unsupported-profile.json", options));
            }

            @Test
            @SneakyThrows
            @DisplayName("one supported and one unsupported profile --> valid")
            void testUnknownAndSupportedProfile() {
                var result = validationModule.validateFile("src/test/resources/profile-for-validation-selection/meta-profile-with-multuple-supported-and-unsupported-profiles.json", options);
                log.info("Validation result: " + result.toString());
                assertThat(result.isValid()).isTrue();
            }
        }

        @Nested
        @DisplayName("meta.profile is absent")
        class ValidationWithoutReferencedMetaProfile {
            @Test
            @SneakyThrows
            @DisplayName("--> IllegalArgumentException")
            void testNoProfile() {
                Assertions.assertThrows(IllegalArgumentException.class, () -> validationModule.validateFile("src/test/resources/profile-for-validation-selection/no-meta-profile.json", options));
            }
        }

        @Nested
        @DisplayName("meta.profile is absent on the top level, but exists in an inner resource")
        class ValidationWithoutReferencedMetaProfileButWithMetaProfileInInnerRessource {
            @Test
            @SneakyThrows
            @DisplayName("--> IllegalArgumentException")
            void testNoProfileOnTopLevelButInInnerResource() {
                Assertions.assertThrows(IllegalArgumentException.class, () -> validationModule.validateFile("src/test/resources/profile-for-validation-selection/no-meta-profile-on-top-level-but-in-inner-resource.json", options));
            }
        }

    }

    @Nested
    @DisplayName("A profile has been passed as parameter ")
    class ValidationWithProfilePassedAsParameter {
        @Nested
        @DisplayName("meta.profile references at least one profile")
        class ValidationWithReferencedMetaProfile {
            @Test
            @SneakyThrows
            @DisplayName("passed profile is referenced and supported --> valid")
            void testProfilePassedAsParameterAndReferencedAndIsSupported() {
                var options = ValidationOptions.getDefaults();
                options.setProfiles(List.of("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate|1.0.0"));
                options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
                options.setAcceptedEncodings(List.of("xml","json"));
                var result = validationModule.validateFile("src/test/resources/profile-for-validation-selection/meta-profile-with-multuple-supported-and-unsupported-profiles.json", options);
                log.info("Validation result: " + result.toString());
                assertThat(result.isValid()).isTrue();
            }

            @Test
            @SneakyThrows
            @DisplayName("passed profile is referenced and unsupported --> invalid")
            void testProfilePassedAsParameterAndReferencedButIsUnknown() {
                var options = ValidationOptions.getDefaults();
                options.setProfiles(List.of("http://unknown-profile"));
                options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
                options.setAcceptedEncodings(List.of("xml","json"));
                Assertions.assertThrows(UnsupportedProfileException.class, () -> validationModule.validateFile("src/test/resources/profile-for-validation-selection/meta-profile-with-multuple-supported-and-unsupported-profiles.json", options));
            }

            @Test
            @SneakyThrows
            @DisplayName("passed profile is not referenced and unsupported --> UnsupportedProfileException")
            void testProfilePassedAsParameterNotReferencedAndUnknown() {
                var options = ValidationOptions.getDefaults();
                options.setProfiles(List.of("http://another-unknown-profile"));
                options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
                options.setAcceptedEncodings(List.of("xml","json"));
                Assertions.assertThrows(UnsupportedProfileException.class, () -> validationModule.validateFile("src/test/resources/profile-for-validation-selection/meta-profile-with-multuple-supported-and-unsupported-profiles.json", options));
            }

            @Test
            @SneakyThrows
            @DisplayName("passed profile is not referenced but supported --> valid")
            void testProfilePassedAsParameterNotReferencedButSupported() {
                var options = ValidationOptions.getDefaults();
                options.setProfiles(List.of("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate|1.0.0"));
                options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
                options.setAcceptedEncodings(List.of("xml","json"));
                var result = validationModule.validateFile("src/test/resources/profile-for-validation-selection/meta-profile-with-one-unsupported-profile.json", options);
                log.info("Validation result: " + result.toString());
                assertThat(result.isValid()).isTrue();
            }
        }

        @Nested
        @DisplayName("meta.profile is absent")
        class ValidationWithoutReferencedMetaProfile {
            @Test
            @SneakyThrows
            @DisplayName("passed profile is supported --> valid")
            void testProfilePassedAsParameterAndReferencedAndIsSupported() {
                var options = ValidationOptions.getDefaults();
                options.setProfiles(List.of("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate|1.0.0"));
                options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
                options.setAcceptedEncodings(List.of("xml","json"));
                var result = validationModule.validateFile("src/test/resources/profile-for-validation-selection/no-meta-profile.json", options);
                log.info("Validation result: " + result.toString());
                assertThat(result.isValid()).isTrue();
            }

            @Test
            @SneakyThrows
            @DisplayName("passed profile is unsupported --> UnsupportedProfileException")
            void testProfilePassedAsParameterAndReferencedButIsUnknown() {
                var options = ValidationOptions.getDefaults();
                options.setProfiles(List.of("http://unknown-profile"));
                options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
                options.setAcceptedEncodings(List.of("xml","json"));
                Assertions.assertThrows(UnsupportedProfileException.class, () -> validationModule.validateFile("src/test/resources/profile-for-validation-selection/no-meta-profile.json", options));
            }
        }
    }
}
