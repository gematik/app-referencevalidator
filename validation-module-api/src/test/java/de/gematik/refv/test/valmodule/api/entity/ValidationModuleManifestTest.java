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

import de.gematik.refv.valmodule.api.entity.MessageTransformation;
import de.gematik.refv.valmodule.api.entity.PackageGroup;
import de.gematik.refv.valmodule.api.entity.PackageGroupReference;
import de.gematik.refv.valmodule.api.entity.ProfileFamily;
import de.gematik.refv.valmodule.api.entity.ProfileVersion;
import de.gematik.refv.valmodule.api.entity.SuppressionRule;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidationModuleManifestTest {

  private static ValidationModuleManifest configWith(
      Map<String, PackageGroup> packageGroups,
      Map<String, List<MessageTransformation>> messageTransformations,
      Map<String, ProfileFamily> profileFamilies) {
    return new ValidationModuleManifest(
        "4.0",
        "test-module",
        "1.0",
        "author",
        "description",
        "",
        true,
        false,
        false,
        List.of(),
        packageGroups,
        messageTransformations,
        profileFamilies,
        List.of(),
        List.of());
  }

  @DisplayName("Given a valid configuration, when created, then null collections default to empty")
  @Test
  void expectNullCollectionsDefaultToEmpty() {
    final var config =
        new ValidationModuleManifest(
            "4.0",
            "m",
            "1.0",
            "a",
            "d",
            null,
            true,
            false,
            false,
            null,
            Map.of("g1", new PackageGroup(List.of("test.pkg#1.0.0"), List.of())),
            null,
            Map.of(
                "test",
                new ProfileFamily(
                    "http://example.org/fhir",
                    "1.0.0",
                    Map.of(
                        "1.0.0",
                        new ProfileVersion(List.of(new PackageGroupReference("g1", null, null)))),
                    Map.of())),
            null,
            null);
    Assertions.assertEquals("", config.specUrl());
    Assertions.assertTrue(Objects.requireNonNull(config.globalSuppressionRules()).isEmpty());
    Assertions.assertFalse(config.packageGroups().isEmpty());
    Assertions.assertTrue(Objects.requireNonNull(config.messageTransformations()).isEmpty());
    Assertions.assertFalse(config.profileFamilies().isEmpty());
    Assertions.assertTrue(Objects.requireNonNull(config.ignoredCodeSystems()).isEmpty());
    Assertions.assertTrue(Objects.requireNonNull(config.ignoredValueSets()).isEmpty());
  }

  @DisplayName(
      "R1.1: Given a null mandatory field, when created, then a NullPointerException is thrown")
  @Test
  @SuppressWarnings("DataFlowIssue")
  void expectR1_1_NullMandatoryFieldThrows() {
    final List<SuppressionRule> globalSuppressions = List.of();
    final Map<String, PackageGroup> packageGroups = Map.of();
    final Map<String, List<MessageTransformation>> transformations = Map.of();
    final Map<String, ProfileFamily> profileFamilies = Map.of();
    final List<String> ignoredCodeSystems = List.of();
    final List<String> ignoredValueSets = List.of();
    Assertions.assertThrows(
        NullPointerException.class,
        () ->
            new ValidationModuleManifest(
                null,
                "m",
                "1.0",
                "a",
                "d",
                "",
                true,
                false,
                false,
                globalSuppressions,
                packageGroups,
                transformations,
                profileFamilies,
                ignoredCodeSystems,
                ignoredValueSets));
  }

  @DisplayName(
      "R1.2: Given an empty package group map, when created, then an IllegalArgumentException is thrown")
  @Test
  void expectR1_2_EmptyPackageGroupsThrows() {
    final var families =
        Map.of("f1", new ProfileFamily("http://example.org/fhir", null, Map.of(), Map.of()));
    final Map<String, PackageGroup> packageGroups = Map.of();
    final Map<String, List<MessageTransformation>> transformations = Map.of();
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> configWith(packageGroups, transformations, families));
  }

  @DisplayName(
      "R1.3: Given an empty profile family map, when created, then an IllegalArgumentException is thrown")
  @Test
  void expectR1_3_EmptyProfileFamiliesThrows() {
    final var groups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    final Map<String, List<MessageTransformation>> transformations = Map.of();
    final Map<String, ProfileFamily> families = Map.of();
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> configWith(groups, transformations, families));
  }

  @DisplayName(
      "R2.1: Given a package group referencing an unknown message transformation, when created, then an IllegalArgumentException is thrown")
  @Test
  void expectR2_1_UnknownMessageTransformationThrows() {
    final var packageGroups =
        Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), List.of("unknown-transformation")));
    final Map<String, List<MessageTransformation>> transformations = Map.of();
    final Map<String, ProfileFamily> families = Map.of();
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> configWith(packageGroups, transformations, families));
  }

  @DisplayName(
      "R2.1: Given a package group referencing a known message transformation, when created, then it succeeds")
  @Test
  void expectR2_1_KnownMessageTransformationAccepted() {
    final var transformation = new MessageTransformation("error", "warning", "locator", ".*", "id");
    final var packageGroups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), List.of("t1")));
    final var profileFamilies =
        Map.of(
            "f1",
            new ProfileFamily(
                "http://example.org/fhir",
                null,
                Map.of(
                    "1.0.0",
                    new ProfileVersion(List.of(new PackageGroupReference("g1", null, null)))),
                Map.of()));
    final var config =
        Assertions.assertDoesNotThrow(
            () ->
                configWith(packageGroups, Map.of("t1", List.of(transformation)), profileFamilies));
    Assertions.assertTrue(config.packageGroups().containsKey("g1"));
  }

  @DisplayName(
      "R2.2: Given a profile family referencing an unknown package group, when created, then an IllegalArgumentException is thrown")
  @Test
  void expectR2_2_UnknownPackageGroupThrows() {
    final var version =
        new ProfileVersion(
            List.of(new PackageGroupReference("unknown-group", LocalDate.of(2025, 1, 1), null)));
    final var families =
        Map.of(
            "family1",
            new ProfileFamily("http://example.org/fhir", null, Map.of("1.0", version), Map.of()));
    final Map<String, PackageGroup> packageGroups = Map.of();
    final Map<String, List<MessageTransformation>> transformations = Map.of();
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> configWith(packageGroups, transformations, families));
  }

  @DisplayName(
      "R2.2: Given a profile family referencing a known package group, when created, then it succeeds")
  @Test
  void expectR2_2_KnownPackageGroupAccepted() {
    final var version =
        new ProfileVersion(
            List.of(new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null)));
    final var families =
        Map.of(
            "family1",
            new ProfileFamily("http://example.org/fhir", null, Map.of("1.0", version), Map.of()));
    final var packageGroups = Map.of("g1", new PackageGroup(List.of("a-1.0.tgz"), null));
    Assertions.assertDoesNotThrow(() -> configWith(packageGroups, Map.of(), families));
  }
}
