/*-
 * #%L
 * Validation Core Library
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

/** Message ID representing a fast identifiable status in the system. */
public enum MessageId {
  NO_ERROR("REFV-000"),
  INIT_CONTEXT_FAILED("REFV-001"),
  VALIDATION_ERROR("REFV-002"),
  PROFILE_FILTER_MISMATCH("REFV-003"),
  NO_CREATION_DATE_IN_RESOURCE("REFV-004"),
  PROFILE_OUTSIDE_OF_VALIDITY_PERIOD("REFV-005"),
  PROFILE_PARSE_ERROR("REFV-006"),
  UNSUPPORTED_PROFILE("REFV-007"),
  MISSING_PROFILE("REFV-008"),
  IO_ERROR("REFV-009"),
  TERMINOLOGY_SERVER_ERROR("REFV-010"),
  INTERNAL_ERROR("REFV-011"),
  COMPONENT_NOT_MATCHING("REFV-012"),
  PACKAGE_LOADING_ERROR("REFV-013"),
  SNAPSHOT_GENERATION_ERROR("REFV-014"),
  SNAPSHOT_ALREADY_PROCESSED("REFV-015"),
  INTERNAL_CONTEXT("REFV-016"),
  VALIDATION_INFO("REFV-017");

  private final String code;

  MessageId(@NonNull String code) {
    this.code = Objects.requireNonNull(code, "The code must not be null");
  }

  public @NonNull String getCode() {
    return code;
  }
}
