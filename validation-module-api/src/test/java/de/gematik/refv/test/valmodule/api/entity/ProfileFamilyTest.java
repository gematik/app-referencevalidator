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
import de.gematik.refv.valmodule.api.entity.ProfileDefinition;
import de.gematik.refv.valmodule.api.entity.ProfileFamily;
import de.gematik.refv.valmodule.api.entity.ProfileVersion;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileFamilyTest {

  private static ProfileVersion versionWithGroup(String group) {
    return new ProfileVersion(
        List.of(new PackageGroupReference(group, LocalDate.of(2025, 1, 1), null)));
  }

  @DisplayName("Given a valid family, when created, then the accessors return the values")
  @Test
  void expectValidFamilyCreation() {
    final var family =
        new ProfileFamily(
            "http://example.org/fhir", "1.0", Map.of("1.0", versionWithGroup("g1")), Map.of());
    Assertions.assertEquals("http://example.org/fhir", family.canonicalBase());
    Assertions.assertEquals("1.0", family.defaultVersion());
    Assertions.assertTrue(family.versions().containsKey("1.0"));
  }

  @DisplayName(
      "Given a blank canonical base, when created, then an IllegalArgumentException is thrown")
  @Test
  void expectBlankCanonicalBaseThrows() {
    final Map<String, ProfileVersion> versions = Map.of();
    final Map<String, ProfileDefinition> profiles = Map.of();
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> new ProfileFamily("  ", null, versions, profiles));
  }

  @DisplayName("Given a null canonical base, when created, then a NullPointerException is thrown")
  @Test
  void expectNullCanonicalBaseThrows() {
    final Map<String, ProfileVersion> versions = Map.of();
    final Map<String, ProfileDefinition> profiles = Map.of();
    Assertions.assertThrows(
        NullPointerException.class, () -> new ProfileFamily(null, null, versions, profiles));
  }

  @DisplayName(
      "Given a defaultVersion not declared among versions, when created, then an IllegalArgumentException is thrown")
  @Test
  void expectUndeclaredDefaultVersionThrows() {
    final var versions = Map.of("1.0", versionWithGroup("g1"));
    final Map<String, ProfileDefinition> profiles = Map.of();
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new ProfileFamily("http://example.org/fhir", "9.9", versions, profiles));
  }

  @DisplayName("Given null versions and profiles, when created, then they default to empty maps")
  @Test
  void expectNullVersionsAndProfilesDefaultToEmpty() {
    final var family = new ProfileFamily("http://example.org/fhir", null, null, null);
    Assertions.assertTrue(family.versions().isEmpty());
    Assertions.assertTrue(family.profiles().isEmpty());
  }
}
