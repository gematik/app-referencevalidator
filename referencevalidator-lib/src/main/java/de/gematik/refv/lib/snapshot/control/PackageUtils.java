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
package de.gematik.refv.lib.snapshot.control;

import de.gematik.refv.lib.exceptions.SnapshotGenerationException;
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.package_resolver.entity.LocalDirectory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Helper Class that exposes common methods about the handling of package coordinates or paths. */
final class PackageUtils {
  private PackageUtils() {}

  static @NonNull String coordinateKey(String coordinates) {
    return requireNonBlank(coordinates, "The package coordinates cannot be blank")
        .toLowerCase(Locale.ROOT);
  }

  static boolean sameCoordinates(String first, String second) {
    return coordinateKey(first).equals(coordinateKey(second));
  }

  @NonNull
  static String requireNonBlank(@Nullable String value, @NonNull String message) {
    if (Objects.requireNonNull(value, "The value cannot be null").isBlank()) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }

  static @NonNull Path resolveContainedPath(@NonNull Path parent, @NonNull String child) {
    Path normalizedParent = parent.toAbsolutePath().normalize();
    Path resolved = normalizedParent.resolve(child).normalize();
    if (!resolved.startsWith(normalizedParent)) {
      throw new SnapshotGenerationException(
          "Resolved path escapes its expected parent directory: " + resolved);
    }
    return resolved;
  }

  static @NonNull String safeDirectoryName(@NonNull String coordinates) {
    return "pkg-"
        + UUID.nameUUIDFromBytes(
            PackageUtils.coordinateKey(coordinates).getBytes(StandardCharsets.UTF_8));
  }

  static @NonNull String safeArchiveRootName(
      @NonNull String packageName, @NonNull String packageVersion) {
    String combined = packageName + "-" + packageVersion;
    String sanitized = combined.replaceAll("[^a-zA-Z0-9._-]", "_");
    return sanitized.isBlank() || ".".equals(sanitized) || "..".equals(sanitized)
        ? safeDirectoryName(combined)
        : sanitized;
  }

  static @NonNull String expectedArchiveFileName(@NonNull LocalDirectory pkg) {
    return safeArchiveRootName(pkg.id().name(), pkg.id().version())
        + LocalArchive.ARCHIVE_PACKAGE_EXTENSION;
  }
}
