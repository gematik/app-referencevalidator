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

import de.gematik.refv.lib.exceptions.PackageParsingFailedException;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Represents the exact coordinates in the form {@code `name#version`}.
 *
 * @param id the {@link PackageId} of the artifact
 */
public record RemotePackage(@NonNull PackageId id) implements PackageCategory {
  public RemotePackage {
    Objects.requireNonNull(id);
  }

  /**
   * Constructs a new {@link RemotePackage} from a FHIR coordinate in form {@code }name#version}.
   *
   * @param coordinates a string containing the FHIR Package filename
   * @return a new instance of {@link LocalArchive}
   * @throws PackageParsingFailedException in case of errors during the parse
   */
  public static RemotePackage parse(@NonNull String coordinates)
      throws PackageParsingFailedException {
    final var packageId = PackageId.parse(coordinates);
    return new RemotePackage(packageId);
  }
}
