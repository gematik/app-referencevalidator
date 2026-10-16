/*-
 * #%L
 * Validation Core Library
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
package de.gematik.refv.lib.snapshot.entity;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Request record for generating one or more snapshots for packages, fetched remotely or contained
 * in a folder. In order to build a snapshot from remote packages, the `sourcePackagePath` parameter
 * should be set in the form <code>id#packageVersion"</code>.
 *
 * @param sourcePackagePath the source package coordinates or directory path containing the packages
 *     used as input
 * @param outputDirectoryPath the output directory path to be used as write destination
 * @param packageNames the list of packages to generate, as coordinate ({@code name#version}).
 */
public record SnapshotGenerationRequest(
    @NonNull Path sourcePackagePath,
    @NonNull Path outputDirectoryPath,
    @NonNull List<String> packageNames) {

  public SnapshotGenerationRequest {
    Objects.requireNonNull(sourcePackagePath, "No valid source package path has been provided");
    Objects.requireNonNull(outputDirectoryPath, "No valid output path has been provided");
    packageNames =
        List.copyOf(
            Objects.requireNonNull(packageNames, "No valid package names has been provided"));
  }

  public SnapshotGenerationRequest(
      @NonNull Path sourcePackagePath, @NonNull Path outputDirectoryPath) {
    this(sourcePackagePath, outputDirectoryPath, List.of());
  }
}
