/*-
 * #%L
 * referencevalidator-cli
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
package de.gematik.refv.cli.config.entity;

import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for Module, needed for generating the snapshots, starting from the given
 * configuration.
 *
 * @param manifestPath the path to the manifest YAML.
 * @param sourcePackagesPath the optional path to the directory containing source packages in TGZ
 *     format to process
 * @param patchesPath the optional path to the directory containing patches
 */
public record SnapshotModuleConfiguration(
    @NonNull Path manifestPath, @Nullable Path sourcePackagesPath, @Nullable Path patchesPath) {
  public SnapshotModuleConfiguration {
    Objects.requireNonNull(manifestPath, "manifestPath is null");
  }
}
