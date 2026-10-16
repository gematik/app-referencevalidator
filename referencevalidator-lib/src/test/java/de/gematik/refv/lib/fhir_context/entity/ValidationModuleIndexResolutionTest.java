/*-
 * #%L
 * Reference Validator Library
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

import de.gematik.refv.lib.valmodule.entity.ValidationModule;
import de.gematik.refv.valmodule.api.entity.PackageGroup;
import de.gematik.refv.valmodule.api.entity.PackageGroupReference;
import de.gematik.refv.valmodule.api.entity.ProfileDefinition;
import de.gematik.refv.valmodule.api.entity.ProfileFamily;
import de.gematik.refv.valmodule.api.entity.ProfileVersion;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ADR "Validation Module Configuration" resolution algorithm: overlapping periods,
 * declaration-order priority, versionless canonicals via defaultVersion, and the closed-period
 * fallback rule.
 */
class ValidationModuleIndexResolutionTest {

  private static final String BASE = "https://fhir.kbv.de/StructureDefinition";

  private static ValidationModuleIndex indexFor(
      Map<String, PackageGroup> groups, Map<String, ProfileFamily> families) {
    final var config =
        new ValidationModuleManifest(
            "3.0", "erp", "1.0", "gematik", "desc", "", true, false, false, List.of(), groups,
            Map.of(), families, List.of(), List.of());
    return new ValidationModuleIndex(new ValidationModule(config, Path.of("/modules/erp.jar")));
  }

  private static ProfileFamily family(Map<String, ProfileVersion> versions) {
    return new ProfileFamily(BASE, "1.0", versions, Map.of("P", new ProfileDefinition(null, null)));
  }

  @DisplayName(
      "Given overlapping periods, when resolving a date, then the first declared period wins")
  @Test
  void expectFirstDeclaredPeriodWinsOnOverlap() {
    // Mirrors kbv_erp 1.0.2 with overlapping transition windows
    final var groups =
        Map.of(
            "g1", new PackageGroup(List.of("a-1.0.tgz"), null),
            "g2", new PackageGroup(List.of("b-2.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(
                new PackageGroupReference(
                    "g1", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30)),
                new PackageGroupReference(
                    "g2", LocalDate.of(2022, 4, 1), LocalDate.of(2022, 12, 31))));
    final var index = indexFor(groups, Map.of("fam", family(Map.of("1.0", periods))));

    // 2022-05-01 is inside both periods -> first declared (g1) wins
    final var packages = index.getPackagesForDate(BASE + "/P", "1.0", LocalDate.of(2022, 5, 1));
    Assertions.assertEquals(List.of("a-1.0.tgz"), packages);
  }

  @DisplayName(
      "Given a versionless canonical, when resolving, then the defaultVersion dependencies are returned")
  @Test
  void expectVersionlessCanonicalUsesDefaultVersion() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null)));
    final var index = indexFor(groups, Map.of("fam", family(Map.of("1.0", periods))));

    // null version -> defaultVersion "1.0" -> open-ended period
    Assertions.assertEquals(List.of("a-1.0.tgz"), index.getFallbackPackages(BASE + "/P", null));
    Assertions.assertTrue(index.findProfileVersion(BASE + "/P", null).isPresent());
  }

  @DisplayName(
      "Given a version with only closed periods, when no date is available, then resolution is empty by design")
  @Test
  void expectClosedOnlyVersionHasNoFallback() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(
                new PackageGroupReference(
                    "g1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))));
    final var index = indexFor(groups, Map.of("fam", family(Map.of("1.0", periods))));

    Assertions.assertTrue(index.getFallbackPackages(BASE + "/P", "1.0").isEmpty());
  }

  @DisplayName("Given a date before all periods, when resolving, then an empty list is returned")
  @Test
  void expectDateBeforeAllPeriodsReturnsEmpty() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null)));
    final var index = indexFor(groups, Map.of("fam", family(Map.of("1.0", periods))));

    Assertions.assertTrue(
        index.getPackagesForDate(BASE + "/P", "1.0", LocalDate.of(2020, 1, 1)).isEmpty());
  }

  @DisplayName("Given a blank version, when resolving, then it is treated as the defaultVersion")
  @Test
  void expectBlankVersionTreatedAsDefault() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null)));
    final var index = indexFor(groups, Map.of("fam", family(Map.of("1.0", periods))));

    Assertions.assertTrue(index.findProfileVersion(BASE + "/P", "  ").isPresent());
  }
}
