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
package de.gematik.refv.test.valmodule.api.entity;

import de.gematik.refv.valmodule.api.entity.PackageGroupReference;
import de.gematik.refv.valmodule.api.entity.ProfileVersion;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileVersionTest {

  @DisplayName(
      "Given an empty period list, when created, then an IllegalArgumentException is thrown")
  @Test
  void expectEmptyPeriodsThrow() {
    final var periods = List.<PackageGroupReference>of();
    Assertions.assertThrows(IllegalArgumentException.class, () -> new ProfileVersion(periods));
  }

  @DisplayName("Given a null period list, when created, then a NullPointerException is thrown")
  @Test
  void expectNullPeriodsThrow() {
    Assertions.assertThrows(NullPointerException.class, () -> new ProfileVersion(null));
  }

  @DisplayName(
      "Given more than one open-ended period, when created, then an IllegalArgumentException is thrown")
  @Test
  void expectMultipleOpenEndedPeriodsThrow() {
    final var open1 = new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null);
    final var open2 = new PackageGroupReference("g2", LocalDate.of(2026, 1, 1), null);
    final var periods = List.of(open1, open2);
    Assertions.assertThrows(IllegalArgumentException.class, () -> new ProfileVersion(periods));
  }

  @DisplayName(
      "Given overlapping periods, when resolving a date, then the first matching period wins")
  @Test
  void expectFirstMatchingPeriodWins() {
    final var first =
        new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    final var second =
        new PackageGroupReference("g2", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 6, 1));
    final var version = new ProfileVersion(List.of(first, second));
    final var match = version.periodFor(LocalDate.of(2025, 9, 1));
    Assertions.assertTrue(match.isPresent());
    Assertions.assertEquals("g1", match.get().packageGroupName());
  }

  @DisplayName("Given a date outside all periods, when resolving, then an empty result is returned")
  @Test
  void expectNoMatchingPeriodReturnsEmpty() {
    final var period =
        new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    final var version = new ProfileVersion(List.of(period));
    Assertions.assertTrue(version.periodFor(LocalDate.of(2030, 1, 1)).isEmpty());
  }

  @DisplayName(
      "Given an open-ended period, when resolving it, then it is returned as the open-ended period")
  @Test
  void expectOpenEndedPeriodResolved() {
    final var closed =
        new PackageGroupReference("g1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
    final var open = new PackageGroupReference("g2", LocalDate.of(2025, 1, 1), null);
    final var version = new ProfileVersion(List.of(closed, open));
    Assertions.assertTrue(version.openEndedPeriod().isPresent());
    Assertions.assertEquals("g2", version.openEndedPeriod().get().packageGroupName());
  }

  @DisplayName("Given only closed periods, when resolving the open-ended period, then it is empty")
  @Test
  void expectNoOpenEndedPeriod() {
    final var closed =
        new PackageGroupReference("g1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
    final var version = new ProfileVersion(List.of(closed));
    Assertions.assertTrue(version.openEndedPeriod().isEmpty());
  }
}
