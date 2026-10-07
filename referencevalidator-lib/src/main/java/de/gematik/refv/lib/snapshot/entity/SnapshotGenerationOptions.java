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

import de.gematik.refv.lib.exceptions.SnapshotGenerationException;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Snapshot Generation Options passed through Input parameters (e.g. Command Line).
 *
 * @param patchesDirectory Directory containing Patches that override existing Packages Resources
 */
public record SnapshotGenerationOptions(@Nullable Path patchesDirectory) {

  public SnapshotGenerationOptions {
    if (Objects.nonNull(patchesDirectory)
        && (!patchesDirectory.toFile().exists() || !patchesDirectory.toFile().isDirectory())) {
      throw new SnapshotGenerationException("The provided patches directory is invalid");
    }
  }

  public SnapshotGenerationOptions() {
    this(null);
  }
}
