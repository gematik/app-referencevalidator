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

import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileValidityTest {

  @DisplayName(
      "Given a profile canonical, when created with the single-argument constructor, then the date is empty")
  @Test
  void expectSingleArgumentConstructorHasEmptyDate() {
    final var profile = ProfileCanonical.fromCanonical("http://example.org/Profile/My|1.0");
    final var validity = new ProfileValidity(profile);
    Assertions.assertEquals(profile, validity.profileCanonical());
    Assertions.assertTrue(validity.validityDate().isEmpty());
  }

  @DisplayName("Given a profile and a date, when created, then the accessors return the values")
  @Test
  void expectFullConstructorWorks() {
    final var profile = ProfileCanonical.fromCanonical("http://example.org/Profile/My|1.0");
    final var date = Optional.of(LocalDate.of(2025, 6, 15));
    final var validity = new ProfileValidity(profile, date);
    Assertions.assertEquals(date, validity.validityDate());
  }

  @DisplayName("Given a null profile, when created, then a NullPointerException is thrown")
  @Test
  void expectNullProfileThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new ProfileValidity(null));
  }

  @DisplayName("Given a null date, when created, then a NullPointerException is thrown")
  @Test
  void expectNullDateThrows() {
    final var profile = ProfileCanonical.fromCanonical("http://example.org/Profile/My|1.0");
    Assertions.assertThrows(NullPointerException.class, () -> new ProfileValidity(profile, null));
  }
}
