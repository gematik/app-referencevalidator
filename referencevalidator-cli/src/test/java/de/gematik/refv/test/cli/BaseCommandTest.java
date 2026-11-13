/*-
 * #%L
 * referencevalidator-cli
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
package de.gematik.refv.test.cli;

import de.gematik.refv.cli.BaseCommand;
import de.gematik.refv.lib.exceptions.ConfigurationException;
import de.gematik.refv.lib.exceptions.ContextFailure;
import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.exceptions.MissingProfileDefinitionException;
import de.gematik.refv.lib.exceptions.PackageLoadFailedException;
import de.gematik.refv.lib.exceptions.ParsingException;
import de.gematik.refv.lib.exceptions.ProfileMismatchException;
import de.gematik.refv.lib.exceptions.ProfileOutsideValidityPeriodException;
import de.gematik.refv.lib.exceptions.SnapshotGenerationException;
import de.gematik.refv.lib.exceptions.TerminologyServerException;
import de.gematik.refv.lib.exceptions.UnsupportedProfileException;
import de.gematik.refv.lib.exceptions.ValidationException;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseCommandTest {

  /** Minimal concrete command to exercise the base behavior. */
  private static class TestCommand extends BaseCommand {
    @Override
    public Integer call() throws Exception {
      return super.call();
    }

    @Override
    public String showInfo() {
      return super.showInfo();
    }

    public MessageId errorCode(Throwable exception) {
      return super.messageIdFor(exception);
    }

    public MessageId errorCode(ContextFailure.FailureCategory category) {
      return super.messageIdFor(category);
    }

    public String diagnostic(Throwable exception) {
      return super.formatErrorMessage(exception);
    }
  }

  @DisplayName("Given a command, when calling it, then zero is returned and info is shown")
  @Test
  void expectCallReturnsZero() {
    final var command = new TestCommand();
    final var exitCode = Assertions.assertDoesNotThrow(command::call);
    Assertions.assertEquals(0, exitCode);
  }

  @DisplayName("Given a command, when showing info, then version, java and locale are included")
  @Test
  void expectShowInfoContainsSystemDetails() {
    final var command = new TestCommand();
    final String info = command.showInfo();
    Assertions.assertTrue(info.contains("gematik Reference Validator"));
    Assertions.assertTrue(info.contains("Java:"));
    Assertions.assertTrue(info.contains("Locale:"));
    Assertions.assertTrue(info.contains("Timezone:"));
  }

  @DisplayName("Configuration diagnostics include a category code and the underlying cause")
  @Test
  void expectConfigurationDiagnosticIncludesCause() {
    final var command = new TestCommand();
    final var cause = new IllegalArgumentException("Unrecognized field 'contextt' at line 3");
    final var configurationFailure =
        new ConfigurationException("Invalid YAML configuration: ./refv.yaml", cause);

    final var message = command.diagnostic(configurationFailure);
    Assertions.assertTrue(message.startsWith("REFV-001:"));
    Assertions.assertTrue(message.contains("./refv.yaml"));
    Assertions.assertTrue(message.contains("Unrecognized field 'contextt' at line 3"));
  }

  @DisplayName("Specific exceptions map to their corresponding error codes")
  @Test
  void expectSpecificExceptionsMapToErrorCodes() {
    final var command = new TestCommand();
    final var cause = new IllegalArgumentException("underlying failure");
    Assertions.assertEquals(MessageId.IO_ERROR, command.errorCode(new IOException("read failed")));
    Assertions.assertEquals(
        MessageId.TERMINOLOGY_SERVER_ERROR,
        command.errorCode(new TerminologyServerException("server unavailable", cause)));
    Assertions.assertEquals(
        MessageId.SNAPSHOT_GENERATION_ERROR,
        command.errorCode(new SnapshotGenerationException("generation failed")));
    Assertions.assertEquals(
        MessageId.PROFILE_OUTSIDE_OF_VALIDITY_PERIOD,
        command.errorCode(new ProfileOutsideValidityPeriodException("profile expired")));
    Assertions.assertEquals(
        MessageId.UNSUPPORTED_PROFILE,
        command.errorCode(new UnsupportedProfileException("unsupported profile")));
    Assertions.assertEquals(
        MessageId.MISSING_PROFILE,
        command.errorCode(new MissingProfileDefinitionException("profile missing")));
    Assertions.assertEquals(
        MessageId.PROFILE_FILTER_MISMATCH,
        command.errorCode(new ProfileMismatchException("profile does not match")));
    Assertions.assertEquals(
        MessageId.PACKAGE_LOADING_ERROR,
        command.errorCode(new PackageLoadFailedException("package missing")));
    Assertions.assertEquals(
        MessageId.INIT_CONTEXT_FAILED,
        command.errorCode(new InitializationException("initialization failed")));
    Assertions.assertEquals(
        MessageId.PROFILE_PARSE_ERROR,
        command.errorCode(new ParsingException("parsing failed")));
    Assertions.assertEquals(
        MessageId.VALIDATION_ERROR,
        command.errorCode(new ValidationException("validation failed")));
    Assertions.assertEquals(
        MessageId.IO_ERROR, command.errorCode(new InvalidPathException("path", "invalid")));
    Assertions.assertEquals(
        MessageId.INTERNAL_ERROR, command.errorCode(new IllegalStateException("unexpected")));
  }

  @DisplayName("Failure categories map to their corresponding error codes")
  @Test
  void expectFailureCategoriesMapToErrorCodes() {
    final var command = new TestCommand();
    Assertions.assertEquals(
        MessageId.INTERNAL_ERROR,
        command.errorCode(ContextFailure.FailureCategory.INTERNAL));
    Assertions.assertEquals(
        MessageId.INIT_CONTEXT_FAILED,
        command.errorCode(ContextFailure.FailureCategory.CONFIGURATION));
    Assertions.assertEquals(
        MessageId.INIT_CONTEXT_FAILED,
        command.errorCode(ContextFailure.FailureCategory.INITIALIZATION));
    Assertions.assertEquals(
        MessageId.TERMINOLOGY_SERVER_ERROR,
        command.errorCode(ContextFailure.FailureCategory.NETWORK));
    Assertions.assertEquals(
        MessageId.PROFILE_PARSE_ERROR, command.errorCode(ContextFailure.FailureCategory.PARSING));
    Assertions.assertEquals(
        MessageId.VALIDATION_ERROR, command.errorCode(ContextFailure.FailureCategory.VALIDATION));
    Assertions.assertEquals(
        MessageId.SNAPSHOT_GENERATION_ERROR,
        command.errorCode(ContextFailure.FailureCategory.SNAPSHOT));
    Assertions.assertEquals(
        MessageId.PACKAGE_LOADING_ERROR,
        command.errorCode(ContextFailure.FailureCategory.PACKAGE_LOADING));
  }

  @DisplayName("Error formatting avoids redundant or empty cause details")
  @Test
  void expectErrorFormattingHandlesMissingOrDuplicateCauseMessages() {
    final var command = new TestCommand();
    Assertions.assertEquals(
        "REFV-011: plain failure", command.diagnostic(new IllegalStateException("plain failure")));
    Assertions.assertEquals(
        "REFV-011: same message",
        command.diagnostic(
            new IllegalStateException("same message", new IllegalArgumentException("same message"))));
    Assertions.assertEquals(
        "REFV-011: outer failure",
        command.diagnostic(
            new IllegalStateException("outer failure", new IllegalArgumentException())));
  }
}
