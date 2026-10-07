/*-
 * #%L
 * Reference Validator Library
 * %%
 * Copyright (C) 2024 - 2026 gematik GmbH
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
package de.gematik.refv.lib.validation.entity;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidationOptionsTest {

  @DisplayName("Given the default configuration, when created, then expected strategies are set")
  @Test
  void expectDefaultConfigurationValues() {
    final var options = ValidationOptions.defaultConfiguration();
    Assertions.assertNull(options.profileToValidate());
    Assertions.assertNull(options.profileFilterRegex());
    Assertions.assertEquals(
        ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
        options.validationMessagesFilterStrategy());
    Assertions.assertEquals(
        ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE,
        options.profileValidityPeriodCheckStrategy());
  }

  @DisplayName("Given a null filter strategy, when created, then a NullPointerException is thrown")
  @Test
  void expectNullFilterStrategyThrows() {
    Assertions.assertThrows(
        NullPointerException.class,
        () ->
            new ValidationOptions(
                null, null, null, ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE));
  }

  @DisplayName("Given all fields, when created, then the accessors return the values")
  @Test
  void expectAllFieldsAccessible() {
    final var profile = ProfileCanonical.fromCanonical("http://example.org/Profile/My|1.0");
    final var pattern = Pattern.compile(".*My.*");
    final var options =
        new ValidationOptions(
            profile,
            pattern,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ERRORS_ONLY,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE);
    Assertions.assertEquals(profile, options.profileToValidate());
    Assertions.assertEquals(pattern, options.profileFilterRegex());
    Assertions.assertEquals(
        ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ERRORS_ONLY,
        options.validationMessagesFilterStrategy());
    Assertions.assertEquals(
        ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE,
        options.profileValidityPeriodCheckStrategy());
  }

  @DisplayName(
      "Given the default configuration, when created, then the rule lists are empty and non-null")
  @Test
  void expectDefaultRuleListsAreEmpty() {
    final var options = ValidationOptions.defaultConfiguration();
    Assertions.assertNotNull(options.messageTransformations());
    Assertions.assertNotNull(options.suppressionRules());
    Assertions.assertTrue(options.messageTransformations().isEmpty());
    Assertions.assertTrue(options.suppressionRules().isEmpty());
  }

  @DisplayName("Given the custom constructor, when created, then the rule lists default to empty")
  @Test
  void expectFourArgConstructorDefaultsRulesToEmpty() {
    final var options =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE);
    Assertions.assertNotNull(options.messageTransformations());
    Assertions.assertTrue(options.messageTransformations().isEmpty());
    Assertions.assertNotNull(options.suppressionRules());
    Assertions.assertTrue(options.suppressionRules().isEmpty());
  }

  @DisplayName("Given null rule lists, when created, then they default to empty immutable lists")
  @Test
  void expectNullRuleListsDefaultToEmpty() {
    final var options =
        new ValidationOptions(
            null,
            null,
            ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
            ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE,
            null,
            null);
    Assertions.assertNotNull(options.messageTransformations());
    Assertions.assertTrue(options.messageTransformations().isEmpty());
    Assertions.assertNotNull(options.suppressionRules());
    Assertions.assertTrue(options.suppressionRules().isEmpty());
  }
}
