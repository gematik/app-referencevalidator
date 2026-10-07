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
package de.gematik.refv.lib.fhir_context.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidationPolicyConfigurationTest {

  @DisplayName("Given the default configuration, when created, then expected policies are set")
  @Test
  void expectDefaultConfigurationValues() {
    final var config = ValidationPolicyConfiguration.defaultConfiguration();
    Assertions.assertEquals(
        ValidationPolicyConfiguration.ValidationLevelPolicy.SHOW_WARNINGS_AND_ERRORS,
        config.validationLevelPolicy());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.UnknownCodeSystemsPolicy.ALLOWED,
        config.unknownCodeSystemsPolicy());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.ExampleCodeSystemsUsagePolicy.DISALLOWED,
        config.exampleCodeSystemsUsagePolicy());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.ExampleUrlsUsagePolicy.ALLOWED,
        config.exampleUrlsUsagePolicy());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.ExampleRestReferencesPolicy.ALLOWED,
        config.exampleRestReferencesPolicy());
    Assertions.assertEquals(
        ValidationPolicyConfiguration.RecursiveModePolicy.DISALLOWED, config.recursiveModePolicy());
  }

  @DisplayName("Given a null policy, when created, then a NullPointerException is thrown")
  @Test
  void expectNullPolicyThrows() {
    Assertions.assertThrows(
        NullPointerException.class,
        () ->
            new ValidationPolicyConfiguration(
                null,
                ValidationPolicyConfiguration.UnknownCodeSystemsPolicy.ALLOWED,
                ValidationPolicyConfiguration.ExampleCodeSystemsUsagePolicy.DISALLOWED,
                ValidationPolicyConfiguration.ExampleUrlsUsagePolicy.DISALLOWED,
                ValidationPolicyConfiguration.ExampleRestReferencesPolicy.DISALLOWED,
                ValidationPolicyConfiguration.RecursiveModePolicy.DISALLOWED,
                ValidationPolicyConfiguration.AnyExtensionPolicy.ALLOWED));
  }
}
