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
import org.jspecify.annotations.NonNull;

/**
 * Defines the request for resolving a package from a remote registry or from the local system
 *
 * @param requirements
 */
public record PackageResolutionRequest(@NonNull List<PackageCategory> requirements) {
  public PackageResolutionRequest {
    requirements = List.copyOf(Objects.requireNonNull(requirements));
    if (requirements.isEmpty())
      throw new IllegalArgumentException("requirements must not be empty");
  }

  public static PackageResolutionRequest of(PackageCategory... r) {
    return new PackageResolutionRequest(List.of(r));
  }
}
