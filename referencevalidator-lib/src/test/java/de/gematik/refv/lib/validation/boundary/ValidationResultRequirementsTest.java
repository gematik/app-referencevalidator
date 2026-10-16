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
package de.gematik.refv.lib.validation.boundary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.refv.lib.fhir_context.boundary.ValidationContext;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationRequest;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import de.gematik.refv.valmodule.api.entity.MessageTransformation;
import de.gematik.refv.valmodule.api.entity.SuppressionRule;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ValidationResultRequirementsTest {
  @ParameterizedTest(name = "{0}")
  @MethodSource("policyCases")
  void appliesResultPolicies(String requirement) throws Exception {
    var warning = message(IssueSeverity.WARNING, "REFV-EXAMPLE", "locator: example warning");
    switch (requirement) {
      case "R6.1" -> {
        var retained = message(IssueSeverity.ERROR, "REFV-OTHER", "retained");
        var suppression = new SuppressionRule("example", "example", "known false positive");
        var options =
            options(
                List.of(transform("warning", "error", null, null, null)),
                List.of(suppression),
                List.of(),
                List.of(),
                keepAll());
        assertEquals(List.of(retained), validate(List.of(warning, retained), options).messages());
      }
      case "R6.2" -> {
        var options =
            options(
                List.of(
                    transform("warning", "error", "REFV-EXAMPLE", "locator", "example"),
                    transform("error", "fatal", null, null, null)),
                List.of(),
                List.of(),
                List.of(),
                keepAll());
        assertEquals(
            List.of(message(IssueSeverity.ERROR, "REFV-EXAMPLE", warning.messageContent())),
            validate(List.of(warning), options).messages());
      }
      case "R6.3" -> {
        var matching = transform("warning", "error", "REFV-EXAMPLE", "locator", "example");
        assertEquals(IssueSeverity.ERROR, transformed(warning, matching).severity());
        for (var nonMatching :
            List.of(
                transform("warning", "error", "EXAMPLE", "locator", "example"),
                transform("warning", "error", "REFV-EXAMPLE", "absent", "example"),
                transform("warning", "error", "REFV-EXAMPLE", "locator", "^example$"),
                transform("error", "fatal", "REFV-EXAMPLE", "locator", "example"))) {
          assertEquals(warning, transformed(warning, nonMatching));
        }
      }
      case "R6.4" -> {
        for (var unrestricted :
            List.of(
                transform(null, "error", null, null, null),
                transform(" ", "error", " ", " ", " "))) {
          assertEquals(IssueSeverity.ERROR, transformed(warning, unrestricted).severity());
        }
      }
      case "R6.5" -> {
        var targets = List.of("FATAL", "ErRoR", "WARNING", "information", "unknown", " ");
        var expected =
            List.of(
                IssueSeverity.FATAL,
                IssueSeverity.ERROR,
                IssueSeverity.WARNING,
                IssueSeverity.INFORMATION,
                IssueSeverity.INFORMATION,
                IssueSeverity.INFORMATION);
        for (int i = 0; i < targets.size(); i++) {
          assertEquals(
              expected.get(i),
              transformed(warning, transform("warning", targets.get(i), null, null, null))
                  .severity());
        }
        assertEquals(
            IssueSeverity.INFORMATION,
            transformed(warning, transform("warning", null, null, null, null)).severity());
      }
      case "R6.6" -> {
        var code = "http://example.org/CodeSystem";
        var value = "http://example.org/ValueSet";
        var codeMessage = message(IssueSeverity.ERROR, "REFV-CODE", "invalid code " + code);
        var valueMessage = message(IssueSeverity.WARNING, "REFV-VALUE", "invalid value " + value);
        var unaffected = message(IssueSeverity.ERROR, "REFV-OTHER", "other error");
        var options =
            options(
                List.of(transform(null, "fatal", null, null, null)),
                List.of(),
                List.of(code, " "),
                List.of(value, ""),
                keepAll());
        var messages =
            List.copyOf(
                validate(List.of(codeMessage, valueMessage, unaffected), options).messages());
        assertEquals(
            List.of(IssueSeverity.INFORMATION, IssueSeverity.INFORMATION, IssueSeverity.FATAL),
            messages.stream().map(ResultMessage::severity).toList());
        var errorsOnly =
            options(
                List.of(),
                List.of(),
                List.of(code),
                List.of(value),
                ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ERRORS_ONLY);
        assertEquals(
            List.of(unaffected),
            validate(List.of(codeMessage, valueMessage, unaffected), errorsOnly).messages());
      }
      case "R6.7" -> {
        var engineMessage = message(IssueSeverity.ERROR, "engine-id", "engine content");
        assertEquals(
            List.of(
                warning,
                message(
                    IssueSeverity.ERROR, MessageId.VALIDATION_ERROR.getCode(), "engine content")),
            validate(List.of(warning, engineMessage), ValidationOptions.defaultConfiguration())
                .messages());
      }
      case "R6.8" ->
          assertEquals(
              allSeverities(),
              validate(allSeverities(), ValidationOptions.defaultConfiguration()).messages());
      case "R6.9" ->
          assertEquals(
              List.of(allSeverities().get(0), allSeverities().get(1)),
              validate(
                      allSeverities(),
                      options(
                          List.of(),
                          List.of(),
                          List.of(),
                          List.of(),
                          ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ERRORS_ONLY))
                  .messages());
      case "R6.10" ->
          assertEquals(
              allSeverities().subList(0, 3),
              validate(
                      allSeverities(),
                      options(
                          List.of(),
                          List.of(),
                          List.of(),
                          List.of(),
                          ValidationOptions.ValidationMessagesFilterStrategy
                              .KEEP_ERRORS_AND_WARNINGS_ONLY))
                  .messages());
      case "R6.11" -> {
        var suppressed =
            options(
                List.of(),
                List.of(new SuppressionRule("all", ".*", "all suppressed")),
                List.of(),
                List.of(),
                keepAll());
        assertNoError(validate(List.of(warning), suppressed));
        for (var strategy :
            List.of(
                ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ERRORS_ONLY,
                ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ERRORS_AND_WARNINGS_ONLY)) {
          var info = message(IssueSeverity.INFORMATION, "REFV-INFO", "informational");
          assertNoError(
              validate(
                  List.of(info), options(List.of(), List.of(), List.of(), List.of(), strategy)));
          assertNoError(
              validate(
                  List.of(warning),
                  options(
                      List.of(transform("warning", "information", null, null, null)),
                      List.of(),
                      List.of(),
                      List.of(),
                      strategy)));
        }
      }
      case "R6.12" -> {
        var defaults = ValidationOptions.defaultConfiguration();
        assertEquals(keepAll(), defaults.validationMessagesFilterStrategy());
        assertEquals(
            ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE,
            defaults.profileValidityPeriodCheckStrategy());
        assertNull(defaults.profileToValidate());
        assertNull(defaults.profileFilterRegex());
        assertEquals(List.of(), defaults.messageTransformations());
        assertEquals(List.of(), defaults.suppressionRules());
        assertEquals(List.of(), defaults.ignoredCodeSystems());
        assertEquals(List.of(), defaults.ignoredValueSets());
      }
      default -> fail("Unexpected requirement " + requirement);
    }
  }

  static Stream<String> policyCases() {
    return Stream.of(
        "R6.1", "R6.2", "R6.3", "R6.4", "R6.5", "R6.6", "R6.7", "R6.8", "R6.9", "R6.10", "R6.11",
        "R6.12");
  }

  static ValidationResult validate(List<ResultMessage> messages, ValidationOptions options)
      throws Exception {
    var context = mock(ValidationContext.class);
    when(context.validate(any(), any())).thenReturn(new ValidationResult(messages));
    try (var validator = ValidatorFactory.withValidationContext(context)) {
      var request = new ValidationRequest(FhirResource.fromJson("{\"resourceType\":\"Patient\"}"));
      return validator.validate(request, options);
    }
  }

  static ResultMessage transformed(ResultMessage message, MessageTransformation transformation)
      throws Exception {
    return validate(
            List.of(message),
            options(List.of(transformation), List.of(), List.of(), List.of(), keepAll()))
        .messages()
        .iterator()
        .next();
  }

  static ValidationOptions options(
      List<MessageTransformation> transformations,
      List<SuppressionRule> suppressions,
      List<String> codes,
      List<String> values,
      ValidationOptions.ValidationMessagesFilterStrategy strategy) {
    return new ValidationOptions(
        null,
        null,
        strategy,
        ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE,
        transformations,
        suppressions,
        codes,
        values);
  }

  static MessageTransformation transform(
      String from, String to, String id, String locator, String regex) {
    return new MessageTransformation(from, to, locator, regex, id);
  }

  static ValidationOptions.ValidationMessagesFilterStrategy keepAll() {
    return ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL;
  }

  static ResultMessage message(IssueSeverity severity, String id, String content) {
    return ResultMessage.fromMessage(severity, id, content);
  }

  static List<ResultMessage> allSeverities() {
    return List.of(
        message(IssueSeverity.FATAL, "REFV-FATAL", "fatal"),
        message(IssueSeverity.ERROR, "REFV-ERROR", "error"),
        message(IssueSeverity.WARNING, "REFV-WARNING", "warning"),
        message(IssueSeverity.INFORMATION, "REFV-INFO", "information"));
  }

  static void assertNoError(ValidationResult result) {
    assertEquals(1, result.messages().size());
    var message = result.messages().iterator().next();
    assertEquals(IssueSeverity.INFORMATION, message.severity());
    assertEquals(MessageId.NO_ERROR.getCode(), message.messageId());
  }
}
