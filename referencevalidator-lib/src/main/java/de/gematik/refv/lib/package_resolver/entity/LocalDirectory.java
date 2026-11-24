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
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Represents an extracted FHIR Package (as directory).
 *
 * @param id the {@link PackageId} of the package
 * @param path the path to the directory containing the package files
 */
public record LocalDirectory(@NonNull PackageId id, @NonNull Path path) implements PackageCategory {
  public LocalDirectory {
    Objects.requireNonNull(id, "The package Id cannot be null");
    path = Objects.requireNonNull(path, "The path cannot be null").toAbsolutePath().normalize();
    if (Files.notExists(path)) {
      throw new PackageLoadFailedException("The package directory does not exist: " + path);
    }
    if (!Files.isDirectory(path) || !Files.isReadable(path)) {
      throw new PackageLoadFailedException("The package directory is not readable: " + path);
    }
  }

  /**
   * Constructs a new {@link LocalDirectory} from a FHIR coordinate (a FHIR package filename) and
   * path.
   *
   * @param coordinates a string containing the FHIR Package filename
   * @param path the path to the package
   * @return a new instance of {@link LocalDirectory}
   * @throws PackageParsingFailedException in case of errors during the parse
   */
  public static LocalDirectory parse(@NonNull String coordinates, @NonNull Path path)
      throws PackageParsingFailedException {
    final var packageId = PackageId.parse(coordinates);
    final var packagePath = Objects.requireNonNull(path, "Path cannot be null").normalize();
    return new LocalDirectory(packageId, packagePath);
  }
}
