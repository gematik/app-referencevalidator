/*-
 * #%L
 * Validation Module API
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
package de.gematik.refv.valmodule.api.entity;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A named, immutable set of FHIR packages that are always loaded together, based on a Module
 * configuration.
 *
 * <p>A package group might intentionally omit a validity period, so it defines only <em>what</em>
 * packages get loaded. <em>When</em> a group applies, is declared exclusively by the {@link
 * PackageGroupReference} entries of a {@link ProfileFamily} version.
 *
 * @param packages the complete, literal list of package archives (e.g. {@code "kbv.basis-1.7.0.tgz"
 *     }) loaded for this group
 * @param messageTransformations names of reusable message-transformation templates applied when
 *     this group is used
 */
public record PackageGroup(
    @NonNull List<String> packages, @Nullable List<String> messageTransformations) {

  public PackageGroup {
    if (Objects.requireNonNull(packages, "The packages list must not be null").isEmpty()) {
      throw new IllegalArgumentException("A package group must declare at least one package");
    }
    packages = List.copyOf(packages);
    messageTransformations =
        Objects.isNull(messageTransformations) ? List.of() : List.copyOf(messageTransformations);
  }

  /**
   * Creates a new PackageGroup with the defined packages and an empty {@link MessageTransformation}
   * list.
   */
  public PackageGroup(@NonNull List<String> packages) {
    this(packages, null);
  }
}
