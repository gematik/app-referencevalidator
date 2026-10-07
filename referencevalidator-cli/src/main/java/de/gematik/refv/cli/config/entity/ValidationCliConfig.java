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
package de.gematik.refv.cli.config.entity;

import de.gematik.refv.cli.report.entity.ReportConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * CLI Configuration for executing Validation of FHIR Resources or generation of Snapshots.
 *
 * @param context the configuration of the core FHIR Engine, mandatory
 * @param module optional configuration, required for the validation of FHIR Resources
 */
public record ValidationCliConfig(
    @NonNull ContextConfiguration context,
    @Nullable ValidationModuleConfiguration module,
    @NonNull ValidationOptions validationOptions,
    @NonNull ReportConfiguration report)
    implements CliConfig {
  public ValidationCliConfig {
    Objects.requireNonNull(context, "The Context Configuration has not been set");
    Objects.requireNonNull(validationOptions, "The Validation Options has not been set");
    Objects.requireNonNull(report, "The Report Configuration has not been set");
  }
}
