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

import de.gematik.refv.lib.exceptions.ProfilePatchException;
import de.gematik.refv.lib.package_resolver.entity.PackageCategory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Applies custom patches to an existing package. */
class PatchApplier {
  private static final Logger log = LoggerFactory.getLogger(PatchApplier.class);
  private static final String PACKAGE_FOLDER_NAME = "package";
  private static final String PATCHES_FOLDER_NAME = "patches";

  private PatchApplier() {}

  /**
   * Applies dependencies for a given FHIR Package.
   *
   * @param fhirPackage the FHIR Package to modify
   * @param packageDir the directory containing the package itself
   * @param patchDir the root directory containing patches
   */
  static void applyForPackage(
      @NonNull PackageCategory fhirPackage, @NonNull Path packageDir, @NonNull Path patchDir) {
    applyForPackage(fhirPackage.id().name(), fhirPackage.id().version(), packageDir, patchDir);
  }

  /**
   * Applies dependencies for a given FHIR Package.
   *
   * @param packageName the name of the FHIR Package
   * @param packageVersion the version of the FHIR Package
   * @param packageDir the directory containing the package itself
   * @param patchDir the root directory containing patches
   */
  static void applyForPackage(
      @NonNull String packageName,
      @NonNull String packageVersion,
      @NonNull Path packageDir,
      @NonNull Path patchDir) {
    final Path packagePatchDir =
        Objects.requireNonNull(patchDir, "The source patch directory cannot be null")
            .resolve(
                Path.of(
                    PATCHES_FOLDER_NAME,
                    packageName + PackageId.PACKAGE_SEPARATOR + packageVersion));

    if (!Files.exists(packagePatchDir)) {
      log.debug("Package directory {} not detected, skipping", packagePatchDir);
      return;
    }

    if (!packagePatchDir
        .getFileName()
        .toString()
        .contentEquals(packageName + "#" + packageVersion)) {
      log.debug(
          "The patch directory does not correspond to the package given: {}",
          packagePatchDir.getFileName());
      throw new ProfilePatchException(
          "The patch directory does not correspond to the package information given");
    }

    AtomicInteger numPatchesApplied = new AtomicInteger();
    try (var stream = Files.walk(packagePatchDir)) {
      for (Path file : stream.filter(Files::isRegularFile).toList()) {
        applyPatch(file, packageDir, numPatchesApplied);
      }
    } catch (IOException | ProfilePatchException e) {
      final String message =
          "Failed to apply patch files to %s#%s".formatted(packageName, packageVersion);
      throw new ProfilePatchException(message, e);
    } finally {
      log.info(
          "Number of patches applied for package {}#{}: {}",
          packageName,
          packageVersion,
          numPatchesApplied.get());
    }
  }

  private static void applyPatch(Path file, Path packageDir, AtomicInteger numPatchesApplied) {
    Path target = packageDir.resolve(PACKAGE_FOLDER_NAME).resolve(file.getFileName());
    try {
      Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
      numPatchesApplied.getAndIncrement();
      log.info("--- Applied patch {} to {}", file, target);
    } catch (IOException e) {
      throw new ProfilePatchException("Failed to apply patch file " + file, e);
    }
  }
}
