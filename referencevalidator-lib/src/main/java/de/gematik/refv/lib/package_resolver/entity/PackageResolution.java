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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Contains resolved packages in dependency-first order.
 *
 * @param packages resolved packages
 */
public record PackageResolution(@NonNull List<ResolvedPackage> packages) {

  public PackageResolution {
    packages = List.copyOf(Objects.requireNonNull(packages, "packages must not be null"));
  }

  /**
   * Finds a resolved package by its exact identifier.
   *
   * @param id package identifier
   * @return resolved package, if present
   */
  public Optional<ResolvedPackage> find(@NonNull PackageId id) {
    return packages.stream().filter(resolvedPackage -> resolvedPackage.id().equals(id)).findFirst();
  }
}
