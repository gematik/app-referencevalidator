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
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Defines a FHIR Profile Canonical.
 *
 * @param canonical
 * @param version
 */
public record ProfileCanonical(@NonNull URI canonical, String version) {
  public static final String UNDEFINED_VERSION = "";
  public static final String DEFAULT_VERSION = "0.0.0";

  public ProfileCanonical {
    Objects.requireNonNull(canonical, "Canonical URI cannot be null");
  }

  public static ProfileCanonical fromCanonical(@NonNull String canonical) {
    String[] splittedString =
        Objects.requireNonNull(canonical, "The canonical must not be null").split("\\|");
    if (splittedString.length < 2) {
      return new ProfileCanonical(URI.create(splittedString[0]), UNDEFINED_VERSION);
    } else {
      return new ProfileCanonical(URI.create(splittedString[0]), splittedString[1]);
    }
  }

  public String value() {
    return version.isBlank() ? canonical.toString() : canonical + "|" + version;
  }
}
