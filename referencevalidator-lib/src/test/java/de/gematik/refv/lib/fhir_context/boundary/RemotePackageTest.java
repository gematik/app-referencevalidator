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
package de.gematik.refv.lib.fhir_context.boundary;

import de.gematik.refv.lib.exceptions.PackageParsingFailedException;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.RemotePackage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RemotePackageTest {

  @DisplayName("Given valid coordinates, when parsing, then name is lower-cased and version kept")
  @Test
  void expectFromPackageCoordinatesParsesCorrectly() {
    final var pkg = RemotePackage.parse("De.Gematik.ISIK#6.0.0");
    Assertions.assertEquals("de.gematik.isik", pkg.id().name());
    Assertions.assertEquals("6.0.0", pkg.id().version());
  }

  @DisplayName(
      "Given malformed coordinates, when parsing, then a PackageParsingFailedException is thrown")
  @ParameterizedTest
  @ValueSource(strings = {"noversion", "a#b#c", "#1.0.0"})
  void expectMalformedCoordinatesThrow(String input) {
    Assertions.assertThrows(PackageParsingFailedException.class, () -> RemotePackage.parse(input));
  }

  @DisplayName("Given null coordinates, when parsing, then a NullPointerException is thrown")
  @Test
  void expectNullCoordinatesThrow() {
    Assertions.assertThrows(NullPointerException.class, () -> RemotePackage.parse(null));
  }

  @DisplayName("Given a null name, when created, then a NullPointerException is thrown")
  @Test
  void expectNullNameThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new PackageId(null, "1.0.0"));
  }

  @DisplayName("Given a null version, when created, then a NullPointerException is thrown")
  @Test
  void expectNullVersionThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new PackageId("name", null));
  }
}
