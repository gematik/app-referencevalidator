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
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Structure holding the information parsed from a FHIR Resource and relevant for the validation.
 *
 * @param profileCanonical the Profile canonical to be used for validating a FHIR Resource against
 * @param validityDate optional, the creation date of the Resource, found through a DateLocator FHIR
 *     Expression defined in a validation module
 */
public record ProfileValidity(
    @NonNull ProfileCanonical profileCanonical, @NonNull Optional<LocalDate> validityDate) {

  public ProfileValidity {
    Objects.requireNonNull(profileCanonical, "Profile canonical cannot be null");
    Objects.requireNonNull(validityDate, "The validity date cannot be null");
  }

  public ProfileValidity(@NonNull ProfileCanonical profileCanonical) {
    this(profileCanonical, Optional.empty());
  }
}
