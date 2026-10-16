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
import java.time.LocalDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PackageGroupReferenceTest {

  @DisplayName(
      "Given a closed period, when checking containment, then boundary dates are inclusive")
  @Test
  void expectClosedPeriodContainsBoundaryDates() {
    final var period =
        new PackageGroupReference("group", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    Assertions.assertTrue(period.contains(LocalDate.of(2025, 1, 1)));
    Assertions.assertTrue(period.contains(LocalDate.of(2025, 6, 15)));
    Assertions.assertTrue(period.contains(LocalDate.of(2025, 12, 31)));
    Assertions.assertFalse(period.contains(LocalDate.of(2024, 12, 31)));
    Assertions.assertFalse(period.contains(LocalDate.of(2026, 1, 1)));
  }

  @DisplayName(
      "Given an open-ended period, when checking, then isOpenEnded is true and it has no upper bound")
  @Test
  void expectOpenEndedPeriod() {
    final var period = new PackageGroupReference("group", LocalDate.of(2025, 1, 1), null);
    Assertions.assertTrue(period.isOpenEnded());
    Assertions.assertTrue(period.contains(LocalDate.of(2030, 1, 1)));
    Assertions.assertFalse(period.contains(LocalDate.of(2024, 12, 31)));
  }

  @DisplayName("Given a closed period, when checking isOpenEnded, then it is false")
  @Test
  void expectClosedPeriodIsNotOpenEnded() {
    final var period =
        new PackageGroupReference("group", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    Assertions.assertFalse(period.isOpenEnded());
  }

  @DisplayName(
      "Given validTill before validFrom, when created, then an IllegalArgumentException is thrown")
  @Test
  void expectValidTillBeforeValidFromThrows() {
    final var validFrom = LocalDate.of(2025, 12, 31);
    final var validTill = LocalDate.of(2025, 1, 1);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new PackageGroupReference("group", validFrom, validTill));
  }

  @DisplayName(
      "Given a blank package group, when created, then an IllegalArgumentException is thrown")
  @Test
  void expectBlankPackageGroupThrows() {
    final var validFrom = LocalDate.of(2025, 1, 1);
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> new PackageGroupReference("  ", validFrom, null));
  }
}
