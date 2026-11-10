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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.valmodule.api.entity.PackageGroup;
import de.gematik.refv.valmodule.api.entity.PackageGroupReference;
import de.gematik.refv.valmodule.api.entity.ProfileDefinition;
import de.gematik.refv.valmodule.api.entity.ProfileFamily;
import de.gematik.refv.valmodule.api.entity.ProfileVersion;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModuleProfilePolicyTest {
  private static final String BASE = "https://example.org/fhir/StructureDefinition";

  @DisplayName("Matches explicitly declared profiles and profiles covered by a family prefix")
  @Test
  void supportsDeclaredAndPrefixFallbackProfiles() {
    var policy = ModuleProfilePolicy.fromManifest(manifest());

    assertTrue(policy.supports(ProfileCanonical.fromCanonical(BASE + "/Explicit|1.0.0")));
    assertTrue(policy.supports(ProfileCanonical.fromCanonical(BASE + "/Other|1.1.0")));
    assertTrue(policy.supports(ProfileCanonical.fromCanonical("https://example.org/other/P|5.0")));
  }

  @DisplayName("Uses the default version for versionless canonicals only when one is declared")
  @Test
  void resolvesVersionlessProfilesThroughTheirFamilyDefault() {
    var policy = ModuleProfilePolicy.fromManifest(manifest());

    assertTrue(policy.supports(ProfileCanonical.fromCanonical(BASE + "/Explicit")));
    assertTrue(policy.supports(ProfileCanonical.fromCanonical("https://example.org/other/P")));
    assertFalse(
        policy.supports(ProfileCanonical.fromCanonical("https://example.org/no-default/P")));
  }

  @DisplayName("Rejects unknown versions, foreign canonicals, and canonical-prefix lookalikes")
  @Test
  void rejectsUnsupportedProfileVersionsAndCanonicals() {
    var policy = ModuleProfilePolicy.fromManifest(manifest());

    assertFalse(policy.supports(ProfileCanonical.fromCanonical(BASE + "/Explicit|9.0.0")));
    assertFalse(policy.supports(ProfileCanonical.fromCanonical(BASE + "-other/P|1.0.0")));
    assertFalse(policy.supports(ProfileCanonical.fromCanonical("https://elsewhere.org/P|1.0.0")));
  }

  private static ValidationModuleManifest manifest() {
    var packageGroups =
        Map.of(
            "one", new PackageGroup(List.of("pkg.one#1.0.0"), null),
            "two", new PackageGroup(List.of("pkg.two#1.0.0"), null));
    var firstVersion =
        new ProfileVersion(
            List.of(new PackageGroupReference("one", LocalDate.of(2020, 1, 1), null)));
    var secondVersion =
        new ProfileVersion(
            List.of(new PackageGroupReference("two", LocalDate.of(2020, 1, 1), null)));
    var family =
        new ProfileFamily(
            BASE,
            "1.0.0",
            Map.of("1.0.0", firstVersion, "1.1.0", secondVersion),
            Map.of("Explicit", new ProfileDefinition(null, null)));
    var trailingSlashFamily =
        new ProfileFamily(
            "https://example.org/other/",
            "5.0",
            Map.of("5.0", firstVersion),
            Map.of("P", new ProfileDefinition(null, null)));
    var familyWithoutDefault =
        new ProfileFamily(
            "https://example.org/no-default",
            null,
            Map.of("1.0.0", firstVersion),
            Map.of("P", new ProfileDefinition(null, null)));
    return new ValidationModuleManifest(
        "3.0",
        "test",
        "1.0.0",
        "test",
        "test",
        "",
        false,
        false,
        false,
        List.of(),
        packageGroups,
        Map.of(),
        Map.of(
            "main", family,
            "trailing-slash", trailingSlashFamily,
            "no-default", familyWithoutDefault),
        List.of(),
        List.of());
  }
}
