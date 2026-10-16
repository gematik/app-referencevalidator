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
package de.gematik.refv.lib.snapshot.boundary;

import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationOptions;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationRequest;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationResult;
import org.jspecify.annotations.NonNull;

/** Interface defining a FHIR Snapshot Generator. */
public interface SnapshotGenerator extends AutoCloseable {
  /**
   * Performs the generation of Snapshots for or more FHIR Resources specified in the request.
   *
   * @param request a {@link SnapshotGenerationRequest} instance that points to one or FHIR
   *     Resources that act as source for the generation of snapshots
   * @param options a {@link SnapshotGenerationOptions} instance that contain options for modifying
   *     the execution of the snapshot generation operation.
   * @return an instance of {@link SnapshotGenerationResult}, containing the result information of
   *     the generation of snapshots
   */
  @NonNull SnapshotGenerationResult generateSnapshots(
      @NonNull SnapshotGenerationRequest request, @NonNull SnapshotGenerationOptions options);

  @Override
  default void close() {}
}
