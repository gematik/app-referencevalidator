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

import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.snapshot.control.DefaultSnapshotGenerator;
import org.jspecify.annotations.NonNull;

/**
 * Factory Class providing a valid {@link SnapshotGenerator} instance used for generating snapshots
 * from FHIR packages.
 */
public final class SnapshotGeneratorFactory {

  private SnapshotGeneratorFactory() {}

  /**
   * Initializes a new {@link SnapshotGenerator} instance with the provided configuration and set of
   * dependencies that need to be processed.
   *
   * <p>Internally it creates a new core {@link
   * de.gematik.refv.lib.fhir_context.boundary.FhirContext} and processes the dependencies.<br>
   *
   * <p>During the generation of snapshots, the original package resources are modified, thus the
   * original one should be checked in a temporary folder and then later on only the resulting new
   * package resources should be imported in the FHIR Context, for the validation operation. <br>
   *
   * <p><strong>Important: </strong>The Snapshot Generation operation requires access to online
   * package registries, so missing dependencies can be easily fetched.
   *
   * <p>The returned generator owns a context and must be closed when snapshot generation is
   * complete.
   *
   * @param contextConfiguration the {@link ContextConfiguration} configuration, defining the
   *     behavior of the SnapshotGenerator
   * @return a new {@link SnapshotGenerator} instance in case of success
   * @throws InitializationException in case of initialization errors
   */
  public static @NonNull SnapshotGenerator fromConfiguration(
      @NonNull ContextConfiguration contextConfiguration) throws InitializationException {
    try {
      return new DefaultSnapshotGenerator(contextConfiguration);
    } catch (Exception e) {
      throw new InitializationException(e.getLocalizedMessage(), e);
    }
  }
}
