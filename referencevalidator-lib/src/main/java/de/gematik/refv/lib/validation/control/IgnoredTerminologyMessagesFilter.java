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
package de.gematik.refv.lib.validation.control;

import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import java.util.Collection;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Converts the messages containing errors related to given terminologies into "info" ones. */
final class IgnoredTerminologyMessagesFilter {
  private IgnoredTerminologyMessagesFilter() {}

  static @NonNull Collection<ResultMessage> apply(
      @NonNull Collection<ResultMessage> resultMessages,
      @Nullable Collection<String> ignoredCodeSystems,
      @Nullable Collection<String> ignoredValueSets) {
    if (isEmpty(ignoredCodeSystems) && isEmpty(ignoredValueSets)) {
      return resultMessages;
    }

    return resultMessages.stream()
        .map(
            message ->
                containsCanonical(message.messageContent(), ignoredCodeSystems, ignoredValueSets)
                    ? ResultMessage.withSeverity(message, IssueSeverity.INFORMATION)
                    : message)
        .toList();
  }

  private static boolean containsCanonical(
      @NonNull String message,
      @Nullable Collection<String> ignoredCodeSystems,
      @Nullable Collection<String> ignoredValueSets) {
    return containsAny(message, ignoredCodeSystems) || containsAny(message, ignoredValueSets);
  }

  private static boolean containsAny(
      @NonNull String message, @Nullable Collection<String> canonicals) {
    return canonicals != null
        && canonicals.stream()
            .filter(Objects::nonNull)
            .filter(canonical -> !canonical.isBlank())
            .anyMatch(message::contains);
  }

  private static boolean isEmpty(@Nullable Collection<String> values) {
    return values == null || values.isEmpty();
  }
}
