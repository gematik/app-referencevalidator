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
package de.gematik.refv.cli.report.entity;

import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/** CLI report data for an input with an expected validity state. */
public record ExpectedValidationResult(
    @NonNull Validity expected,
    @NonNull Validity actual,
    boolean expectationMatched,
    @NonNull Collection<ResultMessage> messages) {

  public ExpectedValidationResult {
    Objects.requireNonNull(expected, "The expected validity cannot be null");
    Objects.requireNonNull(actual, "The actual validity cannot be null");
    messages = List.copyOf(Objects.requireNonNull(messages, "The messages cannot be null"));
  }

  public enum Validity {
    VALID,
    INVALID
  }
}
