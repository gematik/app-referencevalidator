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
package de.gematik.refv.lib.fhir_context.boundary;

import de.gematik.refv.lib.exceptions.SnapshotGenerationException;
import de.gematik.refv.lib.fhir_context.entity.ContextResult;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationResult;
import java.nio.file.Path;
import java.util.Collection;
import org.jspecify.annotations.NonNull;

/**
 * Represents an initialized and reusable FHIR processing context.
 *
 * <p>The context encapsulates the concrete FHIR engine, loaded packages, terminology configuration,
 * and any implementation-specific state required for snapshot generation.
 *
 * <p>A context can be reused for multiple processing operations, provided that the configured
 * engine supports the calling concurrency model.
 *
 * <p>The context must be closed when it is no longer required.
 */
public interface SnapshotGenerationContext extends FhirContext {

  /**
   * Returns the path to the cache where packages are stored.
   *
   * @return the path to the cache store
   */
  @NonNull Path cachePath();

  /**
   * Performs the generation of snapshots for a given extracted NPM Package within the context.
   *
   * @param packageSource the path to a FHIR Package
   * @return a {@link ContextResult} instance containing the information about the validation status
   *     of the resource
   * @throws SnapshotGenerationException in case of internal failures
   */
  @NonNull SnapshotGenerationResult generateSnapshots(@NonNull Path packageSource)
      throws SnapshotGenerationException;

  /**
   * Clones this {@link SnapshotGenerationContext} instance.
   *
   * @param resolvedPackages a list of packages that need to be imported in the new cloned context
   * @return a clone of the existing instance
   */
  @NonNull SnapshotGenerationContext cloneContext(
      @NonNull Collection<ResolvedPackage> resolvedPackages);
}
