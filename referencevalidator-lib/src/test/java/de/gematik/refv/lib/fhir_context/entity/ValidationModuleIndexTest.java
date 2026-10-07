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
import de.gematik.refv.valmodule.api.entity.MessageTransformation;
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

class ValidationModuleIndexTest {

  private static final String BASE = "http://example.org/fhir/StructureDefinition";
  private static final String ISIK_BASE = "https://gematik.de/fhir/isik/StructureDefinition";

  @DisplayName(
      "Given a module, when checking profiles, then declared and prefix-matched profiles are defined, foreign bases are not")
  @Test
  void expectIsProfileDefinedForDeclaredProfile() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var families = Map.of("fam", familyWithOpenEnded());
    final var index = indexFor(config(groups, families));
    Assertions.assertTrue(index.isProfileDefined(BASE + "/MyProfile"));
    // undeclared profile under the same canonical base: defined via prefix fallback
    Assertions.assertTrue(index.isProfileDefined(BASE + "/Other"));
    // profile of a foreign canonical base: not defined
    Assertions.assertFalse(
        index.isProfileDefined("http://other.org/fhir/StructureDefinition/Other"));
  }

  @DisplayName(
      "Given a module, when resolving a versionless canonical, then the default version is used")
  @Test
  void expectVersionlessCanonicalResolvesDefaultVersion() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var families = Map.of("fam", familyWithOpenEnded());
    final var index = indexFor(config(groups, families));
    final var resolved = index.findProfileVersion(BASE + "/MyProfile", null);
    Assertions.assertTrue(resolved.isPresent());
  }

  @DisplayName(
      "Given a module, when resolving a profile of a foreign canonical base, then an empty result is returned")
  @Test
  void expectUnknownProfileResolvesEmpty() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var families = Map.of("fam", familyWithOpenEnded());
    final var index = indexFor(config(groups, families));
    Assertions.assertTrue(
        index
            .findProfileVersion("http://other.org/fhir/StructureDefinition/Unknown", "1.0")
            .isEmpty());
  }

  @DisplayName(
      "Given an open-ended period, when getting fallback dependencies, then the group dependencies are returned")
  @Test
  void expectFallbackPackagesFromOpenEndedPeriod() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz", "b-2.0.tgz"), null));
    final var families = Map.of("fam", familyWithOpenEnded());
    final var index = indexFor(config(groups, families));
    final var fallback = index.getFallbackPackages(BASE + "/MyProfile", "1.0");
    Assertions.assertEquals(List.of("a-1.0.tgz", "b-2.0.tgz"), fallback);
  }

  @DisplayName(
      "Given only closed periods, when getting fallback dependencies, then an empty list is returned")
  @Test
  void expectFallbackPackagesEmptyWhenNoOpenEndedPeriod() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var closed =
        new ProfileVersion(
            List.of(
                new PackageGroupReference(
                    "g1", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))));
    final var families =
        Map.of(
            "fam",
            new ProfileFamily(
                BASE,
                "1.0",
                Map.of("1.0", closed),
                Map.of("MyProfile", new ProfileDefinition(null, null))));
    final var index = indexFor(config(groups, families));
    Assertions.assertTrue(index.getFallbackPackages(BASE + "/MyProfile", "1.0").isEmpty());
  }

  @DisplayName(
      "Given a date within a period, when getting dependencies for that date, then the group dependencies are returned")
  @Test
  void expectPackagesForDateWithinPeriod() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var period =
        new ProfileVersion(
            List.of(
                new PackageGroupReference(
                    "g1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))));
    final var families =
        Map.of(
            "fam",
            new ProfileFamily(
                BASE,
                "1.0",
                Map.of("1.0", period),
                Map.of("MyProfile", new ProfileDefinition(null, null))));
    final var index = indexFor(config(groups, families));
    final var packages =
        index.getPackagesForDate(BASE + "/MyProfile", "1.0", LocalDate.of(2025, 6, 15));
    Assertions.assertEquals(List.of("a-1.0.tgz"), packages);
  }

  @DisplayName(
      "Given a date outside all periods, when getting dependencies for that date, then an empty list is returned")
  @Test
  void expectPackagesForDateOutsidePeriodEmpty() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var period =
        new ProfileVersion(
            List.of(
                new PackageGroupReference(
                    "g1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))));
    final var families =
        Map.of(
            "fam",
            new ProfileFamily(
                BASE,
                "1.0",
                Map.of("1.0", period),
                Map.of("MyProfile", new ProfileDefinition(null, null))));
    final var index = indexFor(config(groups, families));
    Assertions.assertTrue(
        index.getPackagesForDate(BASE + "/MyProfile", "1.0", LocalDate.of(2030, 1, 1)).isEmpty());
  }

  @DisplayName("Given a module, when reading the path and configuration, then they are returned")
  @Test
  void expectPathAndConfigurationAccessors() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var families = Map.of("fam", familyWithOpenEnded());
    final var config = config(groups, families);
    final var index = indexFor(config);
    Assertions.assertEquals(Path.of("/modules/m.jar"), index.getValidationModulePath());
    Assertions.assertEquals(config, index.getValidationModuleConfiguration());
  }

  @DisplayName("Given a null module, when created, then a NullPointerException is thrown")
  @Test
  void expectNullModuleThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new ValidationModuleIndex(null));
  }

  @DisplayName(
      "Given a null profile url, when checking definition, then a NullPointerException is thrown")
  @Test
  void expectIsProfileDefinedNullThrows() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var families = Map.of("fam", familyWithOpenEnded());
    final var index = indexFor(config(groups, families));
    Assertions.assertThrows(NullPointerException.class, () -> index.isProfileDefined(null));
  }

  @DisplayName("Given message transformations, when resolving a version, then they are aggregated")
  @Test
  void expectMessageTransformationsAggregated() {
    final var transformation = new MessageTransformation("error", "warning", "loc", ".*", "id");
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), List.of("t1")));
    final var families = Map.of("fam", familyWithOpenEnded());
    final var index = indexFor(config(groups, families, Map.of("t1", List.of(transformation))));
    final var transformations = index.getMessageTransformations(BASE + "/MyProfile", "1.0");
    Assertions.assertEquals(1, transformations.size());
    Assertions.assertEquals("id", transformations.get(0).messageId());
  }

  @DisplayName(
      "Given a profile with a date source, when resolving the validity date source, then it is returned")
  @Test
  void expectValidityDateSourceResolved() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null)));
    final var families =
        Map.of(
            "fam",
            new ProfileFamily(
                BASE,
                "1.0",
                Map.of("1.0", periods),
                Map.of("MyProfile", new ProfileDefinition("Resource.date", null))));
    final var index = indexFor(config(groups, families));
    Assertions.assertEquals(
        "Resource.date", index.getValidityDateSource(BASE + "/MyProfile", "1.0").orElseThrow());
  }

  @DisplayName(
      "Given a duplicate profile canonical across families, when created, then an IllegalStateException is thrown")
  @Test
  void expectDuplicateCanonicalThrows() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var family = familyWithOpenEnded();
    // Two families with the same canonical base and same profile name -> duplicate canonical
    final var families = Map.of("fam1", family, "fam2", family);
    final var configuration = config(groups, families);
    Assertions.assertThrows(IllegalStateException.class, () -> indexFor(configuration));
  }

  @DisplayName(
      "Given a family without declared profiles, when checking a URL under the canonical base, then it is defined by prefix")
  @Test
  void expectProfileDefinedByCanonicalBasePrefix() {
    final var index = isikIndexWithoutProfiles();
    Assertions.assertTrue(index.isProfileDefined(ISIK_BASE + "/ISiKPatient"));
    Assertions.assertTrue(index.isProfileDefined(ISIK_BASE + "/AnythingElse"));
  }

  @DisplayName(
      "Given a family without declared profiles, when resolving a versionless canonical, then the default version is used")
  @Test
  void expectVersionlessPrefixMatchResolvesDefaultVersion() {
    final var index = isikIndexWithoutProfiles();
    Assertions.assertTrue(index.findProfileVersion(ISIK_BASE + "/ISiKPatient", null).isPresent());
    Assertions.assertTrue(index.findProfileVersion(ISIK_BASE + "/ISiKPatient", "  ").isPresent());
  }

  @DisplayName(
      "Given a family without declared profiles, when resolving a declared version, then it matches by prefix")
  @Test
  void expectExplicitVersionPrefixMatch() {
    final var index = isikIndexWithoutProfiles();
    Assertions.assertTrue(
        index.findProfileVersion(ISIK_BASE + "/ISiKPatient", "5.0.0").isPresent());
  }

  @DisplayName(
      "Given a family without declared profiles, when resolving an undeclared version, then an empty result is returned")
  @Test
  void expectUnknownVersionPrefixMatchResolvesEmpty() {
    final var index = isikIndexWithoutProfiles();
    Assertions.assertTrue(index.findProfileVersion(ISIK_BASE + "/ISiKPatient", "9.9.9").isEmpty());
  }

  @DisplayName(
      "Given a family without declared profiles, when getting fallback dependencies, then the open-ended group packages are returned")
  @Test
  void expectFallbackPackagesForPrefixMatch() {
    final var index = isikIndexWithoutProfiles();
    Assertions.assertEquals(
        List.of("de.gematik.isik#5.1.3"),
        index.getFallbackPackages(ISIK_BASE + "/ISiKPatient", null));
  }

  @DisplayName(
      "Given a family without declared profiles, when getting dependencies for a date in the period, then the group packages are returned")
  @Test
  void expectPackagesForDateForPrefixMatch() {
    final var index = isikIndexWithoutProfiles();
    Assertions.assertEquals(
        List.of("de.gematik.isik#5.1.3"),
        index.getPackagesForDate(ISIK_BASE + "/ISiKPatient", null, LocalDate.of(2026, 3, 1)));
  }

  @DisplayName(
      "Given a family without declared profiles, when checking a URL of a foreign canonical base, then it is not defined")
  @Test
  void expectForeignCanonicalBaseNotDefined() {
    final var index = isikIndexWithoutProfiles();
    Assertions.assertFalse(
        index.isProfileDefined("https://other.org/fhir/StructureDefinition/ISiKPatient"));
    Assertions.assertTrue(
        index
            .findProfileVersion("https://other.org/fhir/StructureDefinition/ISiKPatient", null)
            .isEmpty());
  }

  @DisplayName(
      "Given a family without declared profiles, when the URL only shares a string prefix without a path separator, then it is not defined")
  @Test
  void expectNoMatchOnPartialBaseStringPrefix() {
    final var index = isikIndexWithoutProfiles();
    // "StructureDefinitionX" must not match base ".../StructureDefinition"
    Assertions.assertFalse(index.isProfileDefined(ISIK_BASE + "X/ISiKPatient"));
  }

  @DisplayName(
      "Given a family without declared profiles and message transformations, when resolving by prefix, then transformations are aggregated")
  @Test
  void expectMessageTransformationsForPrefixMatch() {
    final var t1 = new MessageTransformation("error", "information", "loc", ".*", "id-1");
    final var t2 = new MessageTransformation("error", "information", "loc", ".*", "id-2");
    final var groups =
        Map.of(
            "isik",
            new PackageGroup(
                List.of("de.gematik.isik#5.1.3"), List.of("meta_profile_slice_to_error")));
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("isik", LocalDate.of(2025, 1, 1), null)));
    final var family = new ProfileFamily(ISIK_BASE, null, Map.of("5.0.0", periods), Map.of());
    final var index =
        indexFor(
            config(
                groups,
                Map.of("isik", family),
                Map.of("meta_profile_slice_to_error", List.of(t1, t2))));
    // no defaultVersion declared -> the explicit version must be used
    Assertions.assertEquals(
        2, index.getMessageTransformations(ISIK_BASE + "/ISiKPatient", "5.0.0").size());
  }

  @DisplayName(
      "Given a family with declared profiles, when resolving an undeclared profile under the same base, then the prefix fallback applies")
  @Test
  void expectPrefixFallbackAlongsideDeclaredProfiles() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var family = familyWithOpenEnded();
    final var index = indexFor(config(groups, Map.of("fam", family)));
    // declared profile resolves as before
    Assertions.assertTrue(index.findProfileVersion(BASE + "/MyProfile", "1.0").isPresent());
    // undeclared profile of the same family resolves via prefix fallback
    Assertions.assertTrue(index.findProfileVersion(BASE + "/UndeclaredProfile", null).isPresent());
    Assertions.assertEquals(
        List.of("a-1.0.tgz"), index.getFallbackPackages(BASE + "/UndeclaredProfile", null));
  }

  @DisplayName(
      "Given a family without a default version, when resolving a versionless canonical, then an empty result is returned")
  @Test
  void expectVersionlessCanonicalEmptyWithoutDefaultVersion() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null)));
    final var family =
        new ProfileFamily(
            BASE,
            null,
            Map.of("1.0", periods),
            Map.of("MyProfile", new ProfileDefinition(null, null)));
    final var index = indexFor(config(groups, Map.of("fam", family)));
    Assertions.assertTrue(index.findProfileVersion(BASE + "/MyProfile", null).isEmpty());
    // explicit version still works
    Assertions.assertTrue(index.findProfileVersion(BASE + "/MyProfile", "1.0").isPresent());
  }

  @DisplayName(
      "Given overlapping periods, when getting dependencies for a date in both, then the first declared period wins")
  @Test
  void expectFirstDeclaredPeriodWinsOnOverlap() {
    final var groups =
        Map.of(
            "g1", new PackageGroup(List.of("a-1.0.tgz"), null),
            "g2", new PackageGroup(List.of("b-2.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(
                new PackageGroupReference(
                    "g1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
                new PackageGroupReference("g2", LocalDate.of(2025, 6, 1), null)));
    final var family =
        new ProfileFamily(
            BASE,
            "1.0",
            Map.of("1.0", periods),
            Map.of("MyProfile", new ProfileDefinition(null, null)));
    final var index = indexFor(config(groups, Map.of("fam", family)));
    Assertions.assertEquals(
        List.of("a-1.0.tgz"),
        index.getPackagesForDate(BASE + "/MyProfile", "1.0", LocalDate.of(2025, 8, 1)));
  }

  @DisplayName(
      "Given a closed period, when getting dependencies on the boundary dates, then both bounds are inclusive")
  @Test
  void expectPeriodBoundariesInclusive() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(
                new PackageGroupReference(
                    "g1", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))));
    final var family =
        new ProfileFamily(
            BASE,
            "1.0",
            Map.of("1.0", periods),
            Map.of("MyProfile", new ProfileDefinition(null, null)));
    final var index = indexFor(config(groups, Map.of("fam", family)));
    Assertions.assertEquals(
        List.of("a-1.0.tgz"),
        index.getPackagesForDate(BASE + "/MyProfile", "1.0", LocalDate.of(2025, 1, 1)));
    Assertions.assertEquals(
        List.of("a-1.0.tgz"),
        index.getPackagesForDate(BASE + "/MyProfile", "1.0", LocalDate.of(2025, 12, 31)));
    Assertions.assertTrue(
        index.getPackagesForDate(BASE + "/MyProfile", "1.0", LocalDate.of(2024, 12, 31)).isEmpty());
  }

  @DisplayName(
      "Given a canonical base with a trailing slash, when resolving a profile, then no double slash appears in the canonical")
  @Test
  void expectTrailingSlashCanonicalBaseNormalized() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null)));
    final var family =
        new ProfileFamily(
            BASE + "/",
            "1.0",
            Map.of("1.0", periods),
            Map.of("MyProfile", new ProfileDefinition(null, null)));
    final var index = indexFor(config(groups, Map.of("fam", family)));
    Assertions.assertTrue(index.isProfileDefined(BASE + "/MyProfile"));
    Assertions.assertTrue(index.findProfileVersion(BASE + "/MyProfile", "1.0").isPresent());
  }

  @DisplayName(
      "Given a family without a default version, when created, then the index builds and only explicit versions resolve")
  @Test
  void expectFamilyWithoutDefaultVersionLoads() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null)));
    final var family =
        new ProfileFamily(
            BASE,
            null, // no defaultVersion declared in the yaml
            Map.of("1.0", periods),
            Map.of("MyProfile", new ProfileDefinition(null, null)));
    final var index = indexFor(config(groups, Map.of("fam", family)));

    // versionless canonical cannot be served — no default to fall back to
    Assertions.assertTrue(index.findProfileVersion(BASE + "/MyProfile", null).isEmpty());
    Assertions.assertTrue(index.getFallbackPackages(BASE + "/MyProfile", null).isEmpty());
    // explicit version still resolves, both exact and via prefix fallback
    Assertions.assertTrue(index.findProfileVersion(BASE + "/MyProfile", "1.0").isPresent());
    Assertions.assertTrue(index.findProfileVersion(BASE + "/Undeclared", "1.0").isPresent());
    Assertions.assertEquals(
        List.of("a-1.0.tgz"), index.getFallbackPackages(BASE + "/MyProfile", "1.0"));
  }

  private static ValidationModuleIndex indexFor(ValidationModuleManifest config) {
    return new ValidationModuleIndex(new ValidationModule(config, Path.of("/modules/m.jar")));
  }

  private static ValidationModuleManifest config(
      Map<String, PackageGroup> groups, Map<String, ProfileFamily> families) {
    return config(groups, families, Map.of());
  }

  private static ValidationModuleManifest config(
      Map<String, PackageGroup> groups,
      Map<String, ProfileFamily> families,
      Map<String, List<MessageTransformation>> transformations) {
    return new ValidationModuleManifest(
        "4.0",
        "m",
        "1.0",
        "a",
        "d",
        "",
        true,
        false,
        false,
        List.of(),
        groups,
        transformations,
        families,
        List.of(),
        List.of());
  }

  private static ProfileFamily familyWithOpenEnded() {
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null)));
    return new ProfileFamily(
        BASE,
        "1.0",
        Map.of("1.0", periods),
        Map.of("MyProfile", new ProfileDefinition(null, null)));
  }

  /** Mirrors the ISIK yaml: canonicalBase + defaultVersion + one open-ended period, no profiles. */
  private static ValidationModuleIndex isikIndexWithoutProfiles() {
    final var groups = Map.of("isik", new PackageGroup(List.of("de.gematik.isik#5.1.3"), null));
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("isik", LocalDate.of(2025, 1, 1), null)));
    final var family = new ProfileFamily(ISIK_BASE, "5.0.0", Map.of("5.0.0", periods), Map.of());
    return indexFor(config(groups, Map.of("isik", family)));
  }
}
