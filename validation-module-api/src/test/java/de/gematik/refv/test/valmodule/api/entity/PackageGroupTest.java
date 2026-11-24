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

import de.gematik.refv.valmodule.api.entity.PackageGroup;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PackageGroupTest {

  @DisplayName("Given a valid package list, when created, then the packages are stored")
  @Test
  void expectValidPackageGroup() {
    final var group = new PackageGroup(List.of("a-1.0.tgz", "b-2.0.tgz"), null);
    Assertions.assertEquals(2, group.packages().size());
    Assertions.assertTrue(group.messageTransformations().isEmpty());
  }

  @DisplayName(
      "Given an empty package list, when created, then an IllegalArgumentException is thrown")
  @Test
  void expectEmptyPackagesThrow() {
    final var packages = List.<String>of();
    Assertions.assertThrows(IllegalArgumentException.class, () -> new PackageGroup(packages, null));
  }

  @DisplayName("Given a null package list, when created, then a NullPointerException is thrown")
  @Test
  void expectNullPackagesThrow() {
    Assertions.assertThrows(NullPointerException.class, () -> new PackageGroup(null, null));
  }

  @DisplayName(
      "Given message transformations, when created, then they are stored as an immutable copy")
  @Test
  void expectMessageTransformationsCopied() {
    final var group = new PackageGroup(List.of("a-1.0.tgz"), List.of("t1", "t2"));
    Assertions.assertEquals(List.of("t1", "t2"), group.messageTransformations());
    final var transformations = group.messageTransformations();
    Assertions.assertThrows(UnsupportedOperationException.class, () -> transformations.add("t3"));
  }
}
