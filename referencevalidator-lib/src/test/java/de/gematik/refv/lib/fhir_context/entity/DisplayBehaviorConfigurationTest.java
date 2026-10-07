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

class DisplayBehaviorConfigurationTest {

  @DisplayName("Given the default configuration, when created, then expected flags are set")
  @Test
  void expectDefaultConfigurationValues() {
    final var config = DisplayBehaviorConfiguration.defaultConfiguration();
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayWarnings.ENABLED, config.displayWarnings());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayMessagesFromReferences.ENABLED,
        config.displayMessagesFromReferences());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayMessageIds.ENABLED, config.displayMessageIds());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayInvariantsInMessage.ENABLED,
        config.displayInvariantsInMessage());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayHintAboutNonMustSupport.DISABLED,
        config.displayHintAboutNonMustSupport());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayExtensibleBindingsWarnings.DISABLED,
        config.displayExtensibleBindingsWarnings());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayBestPracticeMessageLevel.SHOW_WARNINGS,
        config.displayBestPracticeMessageLevel());
  }

  @DisplayName("Given displayAll, when created, then all flags are enabled")
  @Test
  void expectDisplayAllConfigurationValues() {
    final var config = DisplayBehaviorConfiguration.displayAll();
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayWarnings.ENABLED, config.displayWarnings());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayHintAboutNonMustSupport.ENABLED,
        config.displayHintAboutNonMustSupport());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayExtensibleBindingsWarnings.ENABLED,
        config.displayExtensibleBindingsWarnings());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayBestPracticeMessageLevel.SHOW_HINTS,
        config.displayBestPracticeMessageLevel());
  }

  @DisplayName("Given displayNone, when created, then all flags are disabled")
  @Test
  void expectDisplayNoneConfigurationValues() {
    final var config = DisplayBehaviorConfiguration.displayNone();
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayWarnings.DISABLED, config.displayWarnings());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayMessagesFromReferences.DISABLED,
        config.displayMessagesFromReferences());
    Assertions.assertEquals(
        DisplayBehaviorConfiguration.DisplayBestPracticeMessageLevel.IGNORE,
        config.displayBestPracticeMessageLevel());
  }

  @DisplayName("Given a null mandatory flag, when created, then a NullPointerException is thrown")
  @Test
  void expectNullMandatoryFlagThrows() {
    Assertions.assertThrows(
        NullPointerException.class,
        () ->
            new DisplayBehaviorConfiguration(
                null,
                DisplayBehaviorConfiguration.DisplayMessagesFromReferences.ENABLED,
                DisplayBehaviorConfiguration.DisplayMessageIds.ENABLED,
                DisplayBehaviorConfiguration.DisplayInvariantsInMessage.ENABLED,
                DisplayBehaviorConfiguration.DisplayHintAboutNonMustSupport.DISABLED,
                DisplayBehaviorConfiguration.DisplayExtensibleBindingsWarnings.DISABLED,
                DisplayBehaviorConfiguration.DisplayBestPracticeMessageLevel.SHOW_WARNINGS));
  }
}
