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
package de.gematik.refv.lib.valmodule.entity;

import de.gematik.refv.valmodule.api.entity.PackageGroup;
import de.gematik.refv.valmodule.api.entity.PackageGroupReference;
import de.gematik.refv.valmodule.api.entity.ProfileFamily;
import de.gematik.refv.valmodule.api.entity.ProfileVersion;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidationModuleTest {

  @Test
  void expectCreationIsValid() {
    Assertions.assertDoesNotThrow(
        () ->
            new ValidationModule(
                new ValidationModuleManifest(
                    "test",
                    "4.0",
                    "me",
                    "Test validation module",
                    "",
                    "",
                    true,
                    false,
                    false,
                    List.of(),
                    createDummyPackageGroups(),
                    Map.of(),
                    createDummyProfileFamilies(),
                    List.of(),
                    List.of()),
                Path.of("/modules/")));
  }

  @Test
  void expectSuccessOnNullSpecUrl() {
    Assertions.assertDoesNotThrow(
        () ->
            new ValidationModule(
                new ValidationModuleManifest(
                    "test",
                    "3.0",
                    "me",
                    "Test validation module",
                    "",
                    null,
                    true,
                    false,
                    false,
                    List.of(),
                    createDummyPackageGroups(),
                    Map.of(),
                    createDummyProfileFamilies(),
                    List.of(),
                    List.of()),
                Path.of("/modules/")));
  }

  @Test
  void expectSuccessOnNullListOfSuppressions() {
    Assertions.assertDoesNotThrow(
        () ->
            new ValidationModule(
                new ValidationModuleManifest(
                    "test",
                    "3.0",
                    "me",
                    "Test validation module",
                    "",
                    "https://blabla",
                    true,
                    false,
                    false,
                    null,
                    createDummyPackageGroups(),
                    Map.of(),
                    createDummyProfileFamilies(),
                    List.of(),
                    List.of()),
                Path.of("/modules/")));
  }

  @DisplayName("Expect Success on null dependency list")
  @Test
  void expectSuccessOnNullDependencyLists() {
    Assertions.assertDoesNotThrow(
        () ->
            new ValidationModule(
                new ValidationModuleManifest(
                    "test",
                    "3.0",
                    "me",
                    "Test validation module",
                    "",
                    "https://blabla",
                    true,
                    false,
                    false,
                    List.of(),
                    createDummyPackageGroups(),
                    Map.of(),
                    createDummyProfileFamilies(),
                    List.of(),
                    List.of()),
                Path.of("/modules/")));
  }

  @Test
  void expectSuccessOnNullMessageTransforms() {
    Assertions.assertDoesNotThrow(
        () ->
            new ValidationModule(
                new ValidationModuleManifest(
                    "test",
                    "3.0",
                    "me",
                    "Test validation module",
                    "",
                    "https://blabla",
                    true,
                    false,
                    false,
                    List.of(),
                    createDummyPackageGroups(),
                    null,
                    createDummyProfileFamilies(),
                    List.of(),
                    List.of()),
                Path.of("/modules/")));
  }

  @DisplayName("Expect NullPointerException when no profile group is defined")
  @Test
  void expectExceptionOnNullProfileGroups() {
    Assertions.assertThrows(NullPointerException.class, this::createModuleWithNullProfileFamilies);
  }

  @Test
  void expectSuccessOnNullIgnoredCodeSystem() {
    Assertions.assertDoesNotThrow(
        () ->
            new ValidationModule(
                new ValidationModuleManifest(
                    "test",
                    "3.0",
                    "me",
                    "Test validation module",
                    "",
                    "https://blabla",
                    true,
                    false,
                    false,
                    List.of(),
                    createDummyPackageGroups(),
                    Map.of(),
                    createDummyProfileFamilies(),
                    null,
                    List.of()),
                Path.of("/modules/")));
  }

  @Test
  void expectSuccessOnNullIgnoredValueSets() {
    Assertions.assertDoesNotThrow(
        () ->
            new ValidationModule(
                new ValidationModuleManifest(
                    "test",
                    "3.0",
                    "me",
                    "Test validation module",
                    "",
                    "https://blabla",
                    true,
                    false,
                    false,
                    List.of(),
                    createDummyPackageGroups(),
                    Map.of(),
                    createDummyProfileFamilies(),
                    List.of(),
                    null),
                Path.of("/modules/")));
  }

  private Map<String, PackageGroup> createDummyPackageGroups() {
    return Map.of("my-group-id", new PackageGroup(List.of("de.gematik.test#1.0.0")));
  }

  private ValidationModule createModuleWithNullProfileFamilies() {
    return new ValidationModule(
        new ValidationModuleManifest(
            "test",
            "3.0",
            "me",
            "Test validation module",
            "",
            "https://blabla",
            true,
            false,
            false,
            null,
            createDummyPackageGroups(),
            null,
            null,
            null,
            null),
        Path.of("/modules/"));
  }

  private Map<String, ProfileFamily> createDummyProfileFamilies() {
    return Map.of(
        "my-family-id",
        new ProfileFamily(
            "https://gematik.de/fhir/example",
            null,
            Map.of("1.0.0", new ProfileVersion(List.of(new PackageGroupReference("my-group-id")))),
            null));
  }
}
