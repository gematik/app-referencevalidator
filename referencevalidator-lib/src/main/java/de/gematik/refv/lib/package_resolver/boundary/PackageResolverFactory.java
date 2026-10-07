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

import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.package_resolver.control.DefaultPackageResolver;
import org.jspecify.annotations.NonNull;

/** Gets the Default Implementation of the Resolver */
public final class PackageResolverFactory {
  private PackageResolverFactory() {}

  /**
   * Creates an instance of a default package resolver that offers the following
   * features/capabilities:
   *
   * <ul>
   *   <li>Ability to load remote or local packages, with or without transitive dependencies
   *   <li>Local Packages can come in form of TGZ archives or extracted directories of TGZ archives
   *   <li>Transitive dependencies are expected to be fetched from cache or from a remote registry
   * </ul>
   *
   * @param packageDownloadConfiguration the configuration specifying the path to the cache and the
   *     download policy
   * @return the default {@link PackageResolver} instance
   */
  public static @NonNull PackageResolver withConfiguration(
      @NonNull PackageDownloadConfiguration packageDownloadConfiguration) {
    return new DefaultPackageResolver(packageDownloadConfiguration);
  }
}
