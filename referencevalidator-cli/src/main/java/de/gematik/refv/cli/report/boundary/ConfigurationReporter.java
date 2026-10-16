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
package de.gematik.refv.cli.report.boundary;

import de.gematik.refv.cli.config.entity.CliConfig;
import de.gematik.refv.cli.config.entity.ValidationCliConfig;
import de.gematik.refv.cli.config.entity.ValidationModuleConfiguration;
import de.gematik.refv.cli.report.entity.ReportConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationReporter {
  private final Logger log = LoggerFactory.getLogger(ConfigurationReporter.class);

  public void printConfiguration(@Nullable CliConfig cliConfig) {
    if (Objects.isNull(cliConfig)) {
      log.warn("No valid configuration supplied");
      return;
    }
    final StringBuilder stringBuilder = new StringBuilder();

    stringBuilder.append(
        """


                        ===================================================
                        Context Configuration:
                        """);
    appendContextConfiguration(stringBuilder, cliConfig.context());

    stringBuilder.append(
        """


                            ===================================================
                            Report Configuration:
                            """);
    appendReportConfiguration(stringBuilder, cliConfig.report());

    if (cliConfig instanceof ValidationCliConfig validationCliConfig) {
      stringBuilder.append(
          """
                        ===================================================
                        Module Configuration:
                        """);
      appendModuleConfiguration(stringBuilder, validationCliConfig.module());

      stringBuilder.append(
          """
                        ===================================================
                        Validation Options:
                        """);
      appendValidationOptions(stringBuilder, validationCliConfig.validationOptions());
    }

    log.info("{}", stringBuilder);
  }

  private void appendContextConfiguration(
      final @NonNull StringBuilder stringBuilder,
      final @NonNull ContextConfiguration contextConfiguration) {
    stringBuilder.append(
        String.format("- FHIR Version: %s%n", contextConfiguration.fhirRelease().alias()));
    stringBuilder.append(
        String.format("- Validation Messages Locale: %s%n", contextConfiguration.locale()));
    stringBuilder.append(
        String.format(
            "- Example CodeSystem Usage: %s%n",
            contextConfiguration.validationPolicy().exampleCodeSystemsUsagePolicy().name()));
    stringBuilder.append(
        String.format(
            "- Example URL Usage: %s%n",
            contextConfiguration.validationPolicy().exampleUrlsUsagePolicy().name()));
    stringBuilder.append(
        String.format(
            "- Assume Valid Rest Reference: %s%n",
            contextConfiguration.validationPolicy().exampleRestReferencesPolicy().name()));
    stringBuilder.append(
        String.format(
            "- Unknown CodeSystems cause errors: %s%n",
            contextConfiguration.validationPolicy().unknownCodeSystemsPolicy().name()));
    stringBuilder.append(
        String.format(
            "- Performs Validation recursively: %s%n",
            contextConfiguration.validationPolicy().recursiveModePolicy().name()));
    stringBuilder.append(
        String.format(
            "- Best Practice Settings Level: %s%n",
            contextConfiguration.displayBehavior().displayBestPracticeMessageLevel().name()));
    stringBuilder.append(
        String.format(
            "- Display Warnings Mode: %s%n",
            contextConfiguration.displayBehavior().displayWarnings().name()));
    stringBuilder.append(
        String.format(
            "- Display Messages from References: %s%n",
            contextConfiguration.displayBehavior().displayMessagesFromReferences().name()));
    stringBuilder.append(
        String.format(
            "- Display Message IDs if available: %s%n",
            contextConfiguration.displayBehavior().displayMessageIds().name()));
    stringBuilder.append(
        String.format(
            "- Display Invariants in Messages: %s%n",
            contextConfiguration.displayBehavior().displayInvariantsInMessage().name()));
    stringBuilder.append(
        String.format(
            "- Display Hint about Non Must Support: %s%n",
            contextConfiguration.displayBehavior().displayHintAboutNonMustSupport().name()));
    stringBuilder.append(
        String.format(
            "- Remote Package Loading: %s%n",
            contextConfiguration.packageLoading().remoteDownloadPolicy().name()));
    stringBuilder.append(
        String.format(
            "- Package Cache Path: %s%n", contextConfiguration.packageLoading().cachePath()));
    stringBuilder.append(
        String.format(
            "- Terminology Remote Loading: %s%n",
            contextConfiguration.terminology().remoteLoadingPolicy().name()));
    stringBuilder.append(
        String.format(
            "- Terminology Server URL: %s%n", contextConfiguration.terminology().serverUri()));
  }

  private void appendReportConfiguration(
      StringBuilder stringBuilder, @NonNull ReportConfiguration report) {
    stringBuilder.append(String.format("- Report File Path: %s%n", report.filePath()));
    stringBuilder.append(String.format("- Report File Format: %s%n", report.format().name()));
  }

  private void appendModuleConfiguration(
      final @NonNull StringBuilder stringBuilder,
      final ValidationModuleConfiguration moduleConfig) {
    if (Objects.isNull(moduleConfig)) {
      stringBuilder.append(String.format("- none%n"));
      return;
    }
    stringBuilder.append(String.format("- Modules Directory: %s%n", moduleConfig.directory()));
    stringBuilder.append(String.format("- Validation Module selected: %s%n", moduleConfig.name()));
  }

  private void appendValidationOptions(
      final @NonNull StringBuilder stringBuilder, final ValidationOptions validationOptions) {
    if (Objects.isNull(validationOptions)) {
      stringBuilder.append(String.format("- none%n"));
      return;
    }
    stringBuilder.append(
        String.format("- Profile for Validation: %s%n", validationOptions.profileToValidate()));

    stringBuilder.append("- Profiles Filter Regular Expression: ");
    if (validationOptions.profileFilterRegex() == null) {
      stringBuilder.append("null").append("\n");
    } else {
      stringBuilder.append(validationOptions.profileFilterRegex()).append("\n");
    }
    stringBuilder.append(
        String.format(
            "- Validation Messages Filter Strategy: %s%n",
            validationOptions.validationMessagesFilterStrategy().name()));

    stringBuilder.append(
        String.format(
            "- Profile Validity Period Check Strategy: %s%n",
            validationOptions.profileValidityPeriodCheckStrategy().name()));
  }
}
