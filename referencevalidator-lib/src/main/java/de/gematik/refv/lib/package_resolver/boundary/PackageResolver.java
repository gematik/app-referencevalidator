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
package de.gematik.refv.lib.package_resolver.boundary;

import de.gematik.refv.lib.exceptions.PackageLoadFailedException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.package_resolver.entity.LocalArchive;
import de.gematik.refv.lib.package_resolver.entity.LocalDirectory;
import de.gematik.refv.lib.package_resolver.entity.PackageCategory;
import de.gematik.refv.lib.package_resolver.entity.PackageResolution;
import de.gematik.refv.lib.package_resolver.entity.PackageResolutionRequest;
import de.gematik.refv.lib.package_resolver.entity.RemotePackage;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import java.nio.file.Path;
import java.util.Collection;
import org.jspecify.annotations.NonNull;

/** Resolves FHIR packages and their transitive dependencies. */
public interface PackageResolver {
  enum TransitiveResolution {
    ALLOWED,
    IGNORE
  }

  /**
   * Returns the location to the path where the Packages are resolved.
   *
   * @return the path to the resolved packages directory
   */
  @NonNull Path cachePath();

  /**
   * Resolves the requested packages and all transitive dependencies.
   *
   * <p>The result contains a list of all the packages, in the same order as the dependencies are
   * discovered.
   *
   * @param request a {@link PackageResolutionRequest} instance containing the list of packages to
   *     process
   * @param transitiveResolution if {@link TransitiveResolution#ALLOWED}, it resolves the transitive
   *     dependencies
   * @return resolved packages in dependency-first order
   * @throws PackageLoadFailedException if resolution fails
   */
  @NonNull PackageResolution resolveRequest(
      @NonNull PackageResolutionRequest request, @NonNull TransitiveResolution transitiveResolution)
      throws PackageLoadFailedException;

  /**
   * Resolves the requested packages and all transitive dependencies, if requested.
   *
   * <p>The result contains a list of all the packages, in the same order as the dependencies are
   * discovered.
   *
   * @param packagesToResolve a collection of Packages (path to local directory or archive, remote
   *     coordinates)
   * @return resolved packages in dependency-first order
   * @throws PackageLoadFailedException if resolution fails
   */
  @NonNull PackageResolution resolveList(@NonNull Collection<String> packagesToResolve)
      throws PackageLoadFailedException;

  /**
   * Resolves the given input into a set of ordered {@link PackageCategory} instances.
   *
   * <ul>
   *   <li>a directory is expanded into one {@link LocalDirectory} for a single folder or one {@link
   *       LocalArchive} per {@code .tgz} file directly contained in it
   *   <li>a path pointing to a single {@code .tgz} file yields exactly one {@link LocalArchive}
   *   <li>a "id#packageVersion" coordinate (not an existing file/directory) yields exactly one
   *       {@link RemotePackage}
   * </ul>
   *
   * @param inputPath the raw input to classify
   * @return the resolved, explicit set of {@link ResolvedPackage} instances (possibly empty, if a
   *     directory does not contain any package)
   */
  @NonNull PackageResolution resolvePath(@NonNull Path inputPath) throws PackageLoadFailedException;

  /**
   * Tries to find a matching package, given its coordinates.
   *
   * <p>The result is a single resolved package, if found, otherwise an exception is thrown.
   *
   * @param packageCoordinates the package coordinates to use for the lookup
   * @return a single {@link PackageCategory} package
   * @throws PackageLoadFailedException if resolution fails
   */
  @NonNull PackageCategory getMatchingPackage(@NonNull String packageCoordinates)
      throws PackageLoadFailedException;

  /**
   * Loads core embedded packages for the given {@link FhirRelease} definition.
   *
   * @param fhirRelease the Standard Version to use for importing packages
   * @return the resolved packages for the FHIR standard version
   * @throws PackageLoadFailedException if load fails
   */
  @NonNull PackageResolution loadCore(@NonNull FhirRelease fhirRelease)
      throws PackageLoadFailedException;

  /**
   * Loads embedded packages in the module for the given module JAR path.
   *
   * @param modulePath the path to the module
   * @throws PackageLoadFailedException if load fails
   */
  void loadModulePackages(@NonNull Path modulePath) throws PackageLoadFailedException;
}
