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
package de.gematik.refv.cli;

import de.gematik.refv.cli.commands.VersionProvider;
import de.gematik.refv.lib.exceptions.ContextFailure;
import de.gematik.refv.lib.exceptions.MissingProfileDefinitionException;
import de.gematik.refv.lib.exceptions.ProfileMismatchException;
import de.gematik.refv.lib.exceptions.ProfileOutsideValidityPeriodException;
import de.gematik.refv.lib.exceptions.RefValException;
import de.gematik.refv.lib.exceptions.UnsupportedProfileException;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Defines common properties across commands, performs common operations based on such properties.
 */
public abstract class BaseCommand implements Callable<Integer> {
  private static final Logger log = LoggerFactory.getLogger(BaseCommand.class);

  @CommandLine.Option(
      names = {"-d", "--debug"},
      description = "Show debug log messages",
      defaultValue = "false")
  private boolean showDebugLogs;

  @CommandLine.Option(
      names = {"--additional-debug"},
      description = "Show additional debug log messages from validator",
      defaultValue = "false")
  private boolean showAdditionalDebugLogs;

  protected void configureLogLevel() {
    if (showDebugLogs || showAdditionalDebugLogs) {
      Configurator.setRootLevel(Level.DEBUG);
      if (showAdditionalDebugLogs) {
        Configurator.setLevel("org.hl7.fhir", Level.DEBUG);
      }
    }
  }

  @Override
  public Integer call() throws Exception {
    if (log.isInfoEnabled()) {
      log.info(showInfo());
    }
    configureLogLevel();
    return 0;
  }

  protected String showInfo() {
    return "%ngematik Reference Validator %s".formatted(VersionProvider.PROJECT_VERSION)
        + "%nJava: %17s on %s (%s MB, %s CPU cores available)"
            .formatted(
                System.getProperty("java.version"),
                System.getProperty("os.arch"),
                Runtime.getRuntime().maxMemory() / (1024 * 1024),
                Runtime.getRuntime().availableProcessors())
        + "%nSystem Locale: %s".formatted(Locale.getDefault())
        + "%nTimezone: %18s%n".formatted(TimeZone.getDefault().getID());
  }

  protected String formatErrorMessage(Throwable exception) {
    return "%s: %s".formatted(messageIdFor(exception).getCode(), describeError(exception));
  }

  protected String describeError(Throwable exception) {
    String message =
        Objects.toString(exception.getLocalizedMessage(), exception.getClass().getSimpleName());
    Throwable rootCause = exception;
    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
      rootCause = rootCause.getCause();
    }

    String rootCauseMessage = rootCause.getLocalizedMessage();
    if (rootCause != exception
        && rootCauseMessage != null
        && !rootCauseMessage.isBlank()
        && !message.contains(rootCauseMessage)) {
      return "%s (details: %s)".formatted(message, rootCauseMessage);
    }
    return message;
  }

  protected MessageId messageIdFor(Throwable exception) {
    if (exception instanceof ProfileMismatchException) {
      return MessageId.PROFILE_FILTER_MISMATCH;
    }
    if (exception instanceof ProfileOutsideValidityPeriodException) {
      return MessageId.PROFILE_OUTSIDE_OF_VALIDITY_PERIOD;
    }
    if (exception instanceof UnsupportedProfileException) {
      return MessageId.UNSUPPORTED_PROFILE;
    }
    if (exception instanceof MissingProfileDefinitionException) {
      return MessageId.MISSING_PROFILE;
    }
    if (exception instanceof RefValException refValException) {
      return messageIdFor(refValException.getCategory());
    }
    if (exception instanceof IOException || exception instanceof InvalidPathException) {
      return MessageId.IO_ERROR;
    }
    return MessageId.INTERNAL_ERROR;
  }

  protected MessageId messageIdFor(ContextFailure.FailureCategory category) {
    return switch (category) {
      case INITIALIZATION, CONFIGURATION -> MessageId.INIT_CONTEXT_FAILED;
      case NETWORK -> MessageId.TERMINOLOGY_SERVER_ERROR;
      case PARSING -> MessageId.PROFILE_PARSE_ERROR;
      case INTERNAL -> MessageId.INTERNAL_ERROR;
      case VALIDATION -> MessageId.VALIDATION_ERROR;
      case SNAPSHOT -> MessageId.SNAPSHOT_GENERATION_ERROR;
      case PACKAGE_LOADING -> MessageId.PACKAGE_LOADING_ERROR;
    };
  }
}
