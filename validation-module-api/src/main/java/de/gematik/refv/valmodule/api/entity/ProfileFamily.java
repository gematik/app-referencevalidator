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
package de.gematik.refv.valmodule.api.entity;

import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A set of FHIR profiles sharing the same canonical base URL and release cycle.
 *
 * <p>A family answers the runtime question: "given profile URL + version + date, which {@link
 * PackageGroup} applies?".
 *
 * @param canonicalBase the base URL against which profile names are resolved (a profile's full URL
 *     is {@code canonicalBase + "/" + profileName})
 * @param defaultVersion the version to use when a canonical carries no {@code |version} part;
 *     always declared explicitly, never guessed. May be null if the family does not support
 *     versionless canonicals
 * @param versions the supported versions and their package-group timelines, keyed by the version
 *     string as it appears in canonicals
 * @param profiles the profiles of this family, keyed by profile name, with their optional validity
 *     date sources
 */
public record ProfileFamily(
    @NonNull String canonicalBase,
    @Nullable String defaultVersion,
    @Nullable Map<String, ProfileVersion> versions,
    @Nullable Map<String, ProfileDefinition> profiles) {

  public ProfileFamily {
    Objects.requireNonNull(canonicalBase, "A profile family must declare a canonical base URL");
    if (canonicalBase.isBlank()) {
      throw new IllegalArgumentException("A profile family must declare a canonical base URL");
    }
    versions = Objects.isNull(versions) ? Map.of() : Map.copyOf(versions);
    profiles = Objects.isNull(profiles) ? Map.of() : Map.copyOf(profiles);

    if (Objects.nonNull(defaultVersion) && !versions.containsKey(defaultVersion)) {
      throw new IllegalArgumentException(
          "defaultVersion '%s' is not declared among the versions %s of family '%s'"
              .formatted(defaultVersion, versions.keySet(), canonicalBase));
    }
  }
}
