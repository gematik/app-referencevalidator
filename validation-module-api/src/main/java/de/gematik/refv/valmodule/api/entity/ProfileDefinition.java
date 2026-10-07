/*-
 * #%L
 * Validation Module API
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
package de.gematik.refv.valmodule.api.entity;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Per-profile settings within a {@link ProfileFamily}.
 *
 * @param validityDateSource a FHIRPath expression evaluated against the resource to extract the
 *     reference date used for selecting the correct {@link PackageGroupReference}; may be null when
 *     the profile carries no usable date
 * @param validityDateSourceByVersion optional per-version overrides of {@link
 *     #validityDateSource()}, keyed by version string
 */
public record ProfileDefinition(
    @Nullable String validityDateSource,
    @Nullable Map<String, String> validityDateSourceByVersion) {

  public ProfileDefinition {
    validityDateSourceByVersion =
        validityDateSourceByVersion == null ? Map.of() : Map.copyOf(validityDateSourceByVersion);
  }

  /**
   * Returns the FHIRPath date-source expression for the given version: the version-specific
   * override if declared, otherwise the profile-wide expression.
   */
  public Optional<String> dateSourceFor(@NonNull String version) {
    return Optional.ofNullable(
        Objects.requireNonNull(validityDateSourceByVersion)
            .getOrDefault(version, validityDateSource));
  }
}
