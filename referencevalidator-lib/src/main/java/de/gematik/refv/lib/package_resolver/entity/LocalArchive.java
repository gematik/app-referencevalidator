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

import de.gematik.refv.lib.exceptions.PackageLoadFailedException;
import de.gematik.refv.lib.exceptions.PackageParsingFailedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Represents a TGZ-Archived FHIR Package.
 *
 * @param id the {@link PackageId} of the TGZ archive
 * @param path the path to the archived package
 */
public record LocalArchive(@NonNull PackageId id, @NonNull Path path) implements PackageCategory {
  public static final String ARCHIVE_PACKAGE_EXTENSION = ".tgz";
  public static final String PACKAGE_SEPARATOR = "-";

  public LocalArchive {
    Objects.requireNonNull(id, "The package Id cannot be null");
    path = Objects.requireNonNull(path, "The path cannot be null").toAbsolutePath().normalize();
    if (Files.notExists(path)) {
      throw new PackageLoadFailedException("The archive file does not exist");
    }
    if (Files.isDirectory(path)) {
      throw new PackageLoadFailedException("The archive file cannot be a directory");
    }
  }

  /**
   * Constructs a new {@link PackageId} from a FHIR coordinate (a FHIR package filename).
   *
   * @param coordinates a string containing the FHIR Package filename
   * @return a new instance of {@link PackageId}
   * @throws PackageParsingFailedException in case of errors during the parse
   */
  public static PackageId parse(@NonNull String coordinates) throws PackageParsingFailedException {
    final var normalized = Objects.requireNonNull(coordinates, "coordinates cannot be null").trim();
    int separator = PackageId.versionSeparatorIndex(PACKAGE_SEPARATOR, normalized);
    if (separator <= 0 || separator == normalized.length() - 1) {
      throw new PackageParsingFailedException(
          "Package coordinates must have the form 'name-version': " + normalized);
    }

    final String name = normalized.substring(0, separator);
    final String version =
        normalized.substring(separator + 1).replace(ARCHIVE_PACKAGE_EXTENSION, "");
    return new PackageId(name.toLowerCase(Locale.ROOT), version);
  }

  /**
   * Constructs a new {@link LocalArchive} from a FHIR coordinate (a FHIR package filename) and
   * path.
   *
   * @param coordinates a string containing the FHIR Package filename
   * @param path the path to the package
   * @return a new instance of {@link LocalArchive}
   * @throws PackageParsingFailedException in case of errors during the parse
   */
  public static LocalArchive parse(@NonNull String coordinates, @NonNull Path path)
      throws PackageParsingFailedException {
    final var packageId = parse(coordinates);
    final var packagePath = Objects.requireNonNull(path, "Path cannot be null").normalize();
    return new LocalArchive(packageId, packagePath);
  }
}
