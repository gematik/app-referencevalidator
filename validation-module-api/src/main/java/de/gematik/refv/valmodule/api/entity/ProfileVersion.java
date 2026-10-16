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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * A supported profile version and its package-group timeline.
 *
 * <p>Resolution rules:
 *
 * <ul>
 *   <li>With a reference date: the <b>first</b> period (in declaration order) containing the date
 *       wins. Overlapping periods are allowed — declaration order is the priority.
 *   <li>Without a reference date: the single open-ended period (no {@code validTill}) is used. If
 *       every period is closed, resolution fails — the version is only valid within known time
 *       windows.
 * </ul>
 *
 * @param groups the ordered list of package-group references; never empty, containing at most one
 *     open-ended entry
 */
public record ProfileVersion(@NonNull List<PackageGroupReference> groups) {

  public ProfileVersion {
    if (Objects.requireNonNull(groups, "The packageGroup references must not be null").isEmpty()) {
      throw new IllegalArgumentException(
          "A profile version must declare at least one validity period");
    }
    groups = List.copyOf(groups);
    final long openEndedCount = groups.stream().filter(PackageGroupReference::isOpenEnded).count();
    if (openEndedCount > 1) {
      throw new IllegalArgumentException(
          "A profile version must declare at most one open-ended validity period (without"
              + " validTill), but found "
              + openEndedCount);
    }
  }

  /** Returns the first period (declaration order) containing the given date, if any. */
  public Optional<PackageGroupReference> periodFor(@NonNull LocalDate referenceDate) {
    return groups.stream().filter(period -> period.contains(referenceDate)).findFirst();
  }

  /** Returns the single open-ended period (the currently active package group), if any. */
  public Optional<PackageGroupReference> openEndedPeriod() {
    return groups.stream().filter(PackageGroupReference::isOpenEnded).findFirst();
  }
}
