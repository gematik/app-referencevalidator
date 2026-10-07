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

import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Binds a {@link PackageGroup} to an explicit, inclusive a validity date range.
 *
 * <p>An entry without {@code validTill} is <em>open-ended</em>: it describes the currently active
 * package group and serves as the fallback when no reference date can be determined from a
 * resource.
 *
 * @param packageGroupName the name of the referenced {@link PackageGroup}, which must exist in the
 *     module configuration
 * @param validFrom the first day (inclusive) on which the package group applies
 * @param validTill the last day (inclusive) on which the package group applies, or {@code null} for
 *     an open-ended period
 */
public record PackageGroupReference(
    @NonNull String packageGroupName,
    @Nullable LocalDate validFrom,
    @Nullable LocalDate validTill) {

  public PackageGroupReference {
    if (Objects.requireNonNull(packageGroupName, "The referenced packageGroup must not be null")
        .isBlank()) {
      throw new IllegalArgumentException("A validity period must reference a package group");
    }
    if (Objects.nonNull(validFrom) && Objects.nonNull(validTill) && validTill.isBefore(validFrom)) {
      throw new IllegalArgumentException(
          "validTill (%s) must not be before validFrom (%s) for package group '%s'"
              .formatted(validTill, validFrom, packageGroupName));
    }
  }

  /**
   * Constructs a new {@link PackageGroupReference} instance with only the list of {@link
   * PackageGroup} references, without any validity period specified.
   *
   * @param packageGroupName the name of the referenced {@link PackageGroup}
   */
  public PackageGroupReference(@NonNull String packageGroupName) {
    this(packageGroupName, null, null);
  }

  /** Returns true if this period has no end date and thus describes the currently active group. */
  public boolean isOpenEnded() {
    return Objects.isNull(validTill);
  }

  /** Returns true if the given date lies within this period (both bounds inclusive). */
  public boolean contains(@NonNull LocalDate date) {
    return !date.isBefore(validFrom) && (Objects.isNull(validTill) || !date.isAfter(validTill));
  }
}
