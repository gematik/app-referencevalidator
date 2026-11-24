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

import de.gematik.refv.valmodule.api.entity.ProfileDefinition;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileDefinitionTest {

  @DisplayName(
      "Given a profile-wide date source, when resolving a version, then the profile-wide source is returned")
  @Test
  void expectProfileWideDateSourceUsedAsFallback() {
    final var definition = new ProfileDefinition("Resource.date", null);
    Assertions.assertEquals("Resource.date", definition.dateSourceFor("1.0").orElseThrow());
  }

  @DisplayName(
      "Given a version-specific override, when resolving that version, then the override wins")
  @Test
  void expectVersionSpecificOverrideWins() {
    final var definition =
        new ProfileDefinition("Resource.date", Map.of("2.0", "Resource.authoredOn"));
    Assertions.assertEquals("Resource.authoredOn", definition.dateSourceFor("2.0").orElseThrow());
    Assertions.assertEquals("Resource.date", definition.dateSourceFor("1.0").orElseThrow());
  }

  @DisplayName("Given no date source at all, when resolving, then an empty result is returned")
  @Test
  void expectNoDateSourceReturnsEmpty() {
    final var definition = new ProfileDefinition(null, null);
    Assertions.assertTrue(definition.dateSourceFor("1.0").isEmpty());
  }

  @DisplayName("Given a null override map, when created, then it defaults to an empty map")
  @Test
  void expectNullOverrideMapDefaultsToEmpty() {
    final var definition = new ProfileDefinition("Resource.date", null);
    Assertions.assertTrue(definition.validityDateSourceByVersion().isEmpty());
  }
}
