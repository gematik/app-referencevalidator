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
package de.gematik.refv.lib.package_resolver.control;

import de.gematik.refv.lib.exceptions.PackageLoadFailedException;
import de.gematik.refv.lib.package_resolver.entity.PackageCategory;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;

/** Defines the interface for a PackageCacheManager implementation. */
sealed interface PackageSourceProvider permits FilesystemPackageSourceProvider {

  /**
   * Returns the location where the packages are available.
   *
   * @return the location path
   */
  @NonNull Path location();

  /**
   * Returns whether a FHIR Package is installed or not.
   *
   * @param packageId the {@link PackageId} instance to check
   * @return true if exists, false otherwise
   */
  boolean packageInstalled(@NonNull PackageId packageId);

  /**
   * Loads an archived package.
   *
   * @param packageRequirement a {@link PackageCategory} instance representing the package to be
   *     loaded
   * @return an instance of {@link ResolvedPackage}, containing the package information
   * @throws PackageLoadFailedException in case of issues
   */
  @NonNull ResolvedPackage loadPackage(@NonNull PackageCategory packageRequirement)
      throws PackageLoadFailedException;
}
