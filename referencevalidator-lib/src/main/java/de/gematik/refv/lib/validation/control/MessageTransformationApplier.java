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
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import de.gematik.refv.valmodule.api.entity.MessageTransformation;
import de.gematik.refv.valmodule.api.entity.SuppressionRule;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Applies the module-declared message transformations and suppression rules to a {@link
 * ValidationResult}.
 *
 * <p>This implements the referee behavior described in the module-configuration ADR: the severity
 * of selected engine messages is rewritten ({@link MessageTransformation}) and known false
 * positives are removed ({@link SuppressionRule}), without changing the underlying HL7 engine.
 *
 * <p>A {@link MessageTransformation} matches a message when <em>all</em> of its non-null criteria
 * match:
 *
 * <ul>
 *   <li>{@code messageId} — equals the message id
 *   <li>{@code locatorString} — is contained in the message content
 *   <li>{@code messageLocationRegex} — matches the message content
 * </ul>
 *
 * and the message severity equals {@code severityLevelFrom}. A matching message is rewritten to
 * {@code severityLevelTo}.
 *
 * <p>A {@link SuppressionRule} removes a message when its {@code messagePattern} (a regex) matches
 * the message content.
 */
final class MessageTransformationApplier {
  private static final ConcurrentMap<String, Pattern> cachedPatterns = new ConcurrentHashMap<>();

  private MessageTransformationApplier() {}

  /**
   * Applies the given transformations and suppression rules to the messages of a result.
   *
   * @param result the validation result to transform
   * @param validationOptions the passed validation options to consider
   * @return a new {@link ValidationResult} with transformed/suppressed messages
   */
  static @NonNull ValidationResult apply(
      @NonNull ValidationResult result, @NonNull ValidationOptions validationOptions) {
    Objects.requireNonNull(result, "The validation result must not be null");
    final boolean noTransformations =
        validationOptions.messageTransformations() == null
            || validationOptions.messageTransformations().isEmpty();
    final boolean noSuppressions =
        validationOptions.suppressionRules() == null
            || validationOptions.suppressionRules().isEmpty();

    Collection<ResultMessage> processed;
    if (noTransformations && noSuppressions) {
      // do nothing
      processed = result.messages();
    } else {
      // Suppress errors
      processed =
          result.messages().stream()
              .filter(message -> !isSuppressed(message, validationOptions.suppressionRules()))
              .map(message -> transform(message, validationOptions.messageTransformations()))
              .toList();
    }
    // Filter out messages
    final var filteredResults =
        IgnoredTerminologyMessagesFilter.apply(
            processed,
            validationOptions.ignoredCodeSystems(),
            validationOptions.ignoredValueSets());
    // A ValidationResult must contain at least one message.
    if (filteredResults.isEmpty()) {
      return ValidationResult.forMessage(
          IssueSeverity.INFORMATION, MessageId.NO_ERROR.getCode(), "No Validation Errors found");
    }
    return new ValidationResult(filteredResults);
  }

  private static boolean isSuppressed(
      @NonNull ResultMessage message, @Nullable Collection<SuppressionRule> suppressionRules) {
    if (suppressionRules == null || suppressionRules.isEmpty()) {
      return false;
    }
    return suppressionRules.stream()
        .filter(Objects::nonNull)
        .anyMatch(rule -> matchesPattern(message.messageContent(), rule.messagePattern()));
  }

  private static @NonNull ResultMessage transform(
      @NonNull ResultMessage message, @Nullable Collection<MessageTransformation> transformations) {
    if (transformations == null || transformations.isEmpty()) {
      return message;
    }
    return transformations.stream()
        .filter(Objects::nonNull)
        .filter(t -> severityMatches(message, t.severityLevelFrom()) && matches(message, t))
        .findFirst()
        .map(t -> ResultMessage.withSeverity(message, toSeverity(t.severityLevelTo())))
        .orElse(message);
  }

  private static boolean matches(@NonNull ResultMessage message, MessageTransformation t) {
    if (t.messageId() != null
        && !t.messageId().isBlank()
        && !t.messageId().equals(message.messageId())) {
      return false;
    }
    if (t.locatorString() != null
        && !t.locatorString().isBlank()
        && !message.messageContent().contains(t.locatorString())) {
      return false;
    }
    return t.messageLocationRegex() == null
        || t.messageLocationRegex().isBlank()
        || matchesPattern(message.messageContent(), t.messageLocationRegex());
  }

  private static boolean matchesPattern(@NonNull String content, @Nullable String pattern) {
    if (pattern == null || pattern.isBlank()) {
      return false;
    }

    return cachedPatterns
        .computeIfAbsent(pattern, key -> Pattern.compile(key, Pattern.DOTALL))
        .matcher(content)
        .find();
  }

  private static boolean severityMatches(
      @NonNull ResultMessage message, @Nullable String severityLevelFrom) {
    if (severityLevelFrom == null || severityLevelFrom.isBlank()) {
      return true;
    }
    return message.severity().getCode().equalsIgnoreCase(severityLevelFrom);
  }

  private static @NonNull IssueSeverity toSeverity(@Nullable String severityLevelTo) {
    if (severityLevelTo == null || severityLevelTo.isBlank()) {
      return IssueSeverity.INFORMATION;
    }
    return switch (severityLevelTo.toLowerCase(java.util.Locale.ROOT)) {
      case "fatal" -> IssueSeverity.FATAL;
      case "error" -> IssueSeverity.ERROR;
      case "warning" -> IssueSeverity.WARNING;
      default -> IssueSeverity.INFORMATION;
    };
  }
}
