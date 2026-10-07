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
package de.gematik.refv.lib.package_resolver.entity;

import de.gematik.refv.lib.exceptions.PackageParsingFailedException;
import de.gematik.refv.lib.package_resolver.control.SemanticVersion;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Represents the Identification of a FHIR Package.
 *
 * <p>The version expression may be an exact version, such as {@code 1.2.3}, or a wildcard version,
 * such as {@code 1.x}, {@code 1.2.x}, or {@code *}.
 *
 * @param name the name of the package
 * @param version the version of the package
 */
public record PackageId(@NonNull String name, @NonNull String version)
    implements Comparable<PackageId> {
  public static final String PACKAGE_SEPARATOR = "#";

  public PackageId {
    name =
        Objects.requireNonNull(name, "Package name cannot be null").trim().toLowerCase(Locale.ROOT);
    version = Objects.requireNonNull(version, "Package version cannot be null").trim();
    if (name.isBlank() || version.isBlank()) {
      throw new PackageParsingFailedException("name and version are required");
    }
    if (name.contains("/") || name.contains("\\") || name.contains("#")) {
      throw new PackageParsingFailedException("Invalid FHIR package name: " + name);
    }
  }

  /**
   * Constructs a new {@link PackageId} from a FHIR coordinate ('name#version')
   *
   * @param coordinates a string containing the FHIR Package coordinate
   * @return a new instance of {@link PackageId}
   * @throws PackageParsingFailedException in case of errors during the parse
   */
  public static PackageId parse(@NonNull String coordinates) throws PackageParsingFailedException {
    if (Objects.requireNonNull(coordinates, "coordinates cannot be null").isBlank()) {
      throw new PackageParsingFailedException("The FHIR Package coordinates cannot be blank");
    }
    final var normalized = coordinates.trim();
    int separator = versionSeparatorIndex(PACKAGE_SEPARATOR, normalized);
    if (separator <= 0 || separator == normalized.length() - 1) {
      throw new PackageParsingFailedException(
          "Package coordinates must have the form 'name#version': " + normalized);
    }
    return new PackageId(normalized.substring(0, separator), normalized.substring(separator + 1));
  }

  /**
   * Returns the index of the dash separating the package name from its version. FHIR package
   * versions are semantic versions and therefore start with a digit, so the separator is the first
   * dash followed by a digit (e.g. {@code hl7.fhir.uv.sdc-4.0.0-ballot.tgz} splits before {@code
   * 4.0.0-ballot}). Falls back to the last dash for non-semver versions.
   */
  public static int versionSeparatorIndex(
      @NonNull String separatorChar, @NonNull String coordinates) {
    for (int i = 0; i < coordinates.length() - 1; i++) {
      if (coordinates.charAt(i) == separatorChar.charAt(0)
          && Character.isDigit(coordinates.charAt(i + 1))) {
        return i;
      }
    }
    return coordinates.lastIndexOf(separatorChar);
  }

  /**
   * Returns the coordinates as a complete string ('name#version').
   *
   * @return a string containing the name and version of a package
   */
  public String coordinates() {
    return name + PACKAGE_SEPARATOR + version;
  }

  @Override
  public int compareTo(PackageId other) {
    int byName = name.compareTo(other.name);
    return byName != 0 ? byName : SemanticVersion.compare(version, other.version);
  }
}
