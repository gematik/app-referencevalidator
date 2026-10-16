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
package de.gematik.refv.lib.validation.entity;

import java.net.URI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileCanonicalTest {

  @DisplayName(
      "Given a canonical without version, when parsing, then the undefined version is used")
  @Test
  void expectCanonicalWithoutVersion() {
    final var canonical = ProfileCanonical.fromCanonical("http://example.org/Profile/My");
    Assertions.assertEquals(URI.create("http://example.org/Profile/My"), canonical.canonical());
    Assertions.assertEquals(ProfileCanonical.UNDEFINED_VERSION, canonical.version());
  }

  @DisplayName("Given a canonical with version, when parsing, then the version is extracted")
  @Test
  void expectCanonicalWithVersion() {
    final var canonical = ProfileCanonical.fromCanonical("http://example.org/Profile/My|1.5");
    Assertions.assertEquals(URI.create("http://example.org/Profile/My"), canonical.canonical());
    Assertions.assertEquals("1.5", canonical.version());
  }

  @DisplayName("Given a null canonical, when parsing, then a NullPointerException is thrown")
  @Test
  void expectNullCanonicalThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> ProfileCanonical.fromCanonical(null));
  }

  @DisplayName(
      "Given a canonical with a version, when reading value, then canonical and version are joined")
  @Test
  void expectValueWithVersion() {
    final var canonical = ProfileCanonical.fromCanonical("http://example.org/Profile/My|1.5");
    Assertions.assertEquals("http://example.org/Profile/My|1.5", canonical.value());
  }

  @DisplayName(
      "Given a canonical with a blank version, when reading value, then only the canonical is returned")
  @Test
  void expectValueWithBlankVersion() {
    final var canonical = new ProfileCanonical(URI.create("http://example.org/Profile/My"), "");
    Assertions.assertEquals("http://example.org/Profile/My", canonical.value());
  }

  @DisplayName("Given a null canonical URI, when created, then a NullPointerException is thrown")
  @Test
  void expectNullCanonicalUriThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new ProfileCanonical(null, "1.0"));
  }
}
