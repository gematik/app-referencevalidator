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

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Defines the Behavior for displaying messages during the validation.
 *
 * @param displayWarnings flag to enable/disable the displaying of warnings
 * @param displayMessagesFromReferences flag to enable/disable the displaying of messages from
 *     references
 * @param displayMessageIds flag to enable/disable the displaying the message IDs
 * @param displayInvariantsInMessage flag to enable/disable the displaying of Invariants in messages
 * @param displayHintAboutNonMustSupport flag to enable/disable the displaying of hints about
 *     Must-Support fields
 * @param displayExtensibleBindingsWarnings flag to enable/disable the displaying of extensible
 *     bindings warnings
 * @param displayBestPracticeMessageLevel flag to configure the level of best-practices message
 *     hints
 */
public record DisplayBehaviorConfiguration(
    @NonNull DisplayWarnings displayWarnings,
    @NonNull DisplayMessagesFromReferences displayMessagesFromReferences,
    @NonNull DisplayMessageIds displayMessageIds,
    @NonNull DisplayInvariantsInMessage displayInvariantsInMessage,
    @NonNull DisplayHintAboutNonMustSupport displayHintAboutNonMustSupport,
    @NonNull DisplayExtensibleBindingsWarnings displayExtensibleBindingsWarnings,
    @NonNull DisplayBestPracticeMessageLevel displayBestPracticeMessageLevel) {

  public enum DisplayWarnings {
    ENABLED,
    DISABLED
  }

  public enum DisplayMessagesFromReferences {
    ENABLED,
    DISABLED
  }

  public enum DisplayMessageIds {
    ENABLED,
    DISABLED
  }

  public enum DisplayInvariantsInMessage {
    ENABLED,
    DISABLED
  }

  public enum DisplayHintAboutNonMustSupport {
    ENABLED,
    DISABLED
  }

  public enum DisplayExtensibleBindingsWarnings {
    ENABLED,
    DISABLED
  }

  public enum DisplayBestPracticeMessageLevel {
    IGNORE,
    SHOW_HINTS,
    SHOW_WARNINGS,
    SHOW_ERRORS
  }

  public DisplayBehaviorConfiguration {
    Objects.requireNonNull(
        displayWarnings, "The configuration for 'displayWarnings' has not been set");
    Objects.requireNonNull(
        displayMessagesFromReferences,
        "The configuration for 'displayMessagesFromReferences' has not been set");
    Objects.requireNonNull(
        displayMessageIds, "The configuration for 'displayMessageIds' has not been set");
    Objects.requireNonNull(
        displayInvariantsInMessage,
        "The configuration for 'displayInvariantsInMessage' has not been set");
    Objects.requireNonNull(
        displayHintAboutNonMustSupport,
        "The configuration for 'displayHintAboutNonMustSupport' has not been set");
    Objects.requireNonNull(
        displayExtensibleBindingsWarnings,
        "The configuration for 'displayExtensibleBindingsWarnings' has not been set");
  }

  public static DisplayBehaviorConfiguration defaultConfiguration() {
    return new DisplayBehaviorConfiguration(
        DisplayWarnings.ENABLED,
        DisplayMessagesFromReferences.ENABLED,
        DisplayMessageIds.ENABLED,
        DisplayInvariantsInMessage.ENABLED,
        DisplayHintAboutNonMustSupport.DISABLED,
        DisplayExtensibleBindingsWarnings.DISABLED,
        DisplayBestPracticeMessageLevel.SHOW_WARNINGS);
  }

  public static DisplayBehaviorConfiguration displayAll() {
    return new DisplayBehaviorConfiguration(
        DisplayWarnings.ENABLED,
        DisplayMessagesFromReferences.ENABLED,
        DisplayMessageIds.ENABLED,
        DisplayInvariantsInMessage.ENABLED,
        DisplayHintAboutNonMustSupport.ENABLED,
        DisplayExtensibleBindingsWarnings.ENABLED,
        DisplayBestPracticeMessageLevel.SHOW_HINTS);
  }

  public static DisplayBehaviorConfiguration displayNone() {
    return new DisplayBehaviorConfiguration(
        DisplayWarnings.DISABLED,
        DisplayMessagesFromReferences.DISABLED,
        DisplayMessageIds.DISABLED,
        DisplayInvariantsInMessage.DISABLED,
        DisplayHintAboutNonMustSupport.DISABLED,
        DisplayExtensibleBindingsWarnings.DISABLED,
        DisplayBestPracticeMessageLevel.IGNORE);
  }
}
