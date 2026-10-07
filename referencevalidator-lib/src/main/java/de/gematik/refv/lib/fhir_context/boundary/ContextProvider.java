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

import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.fhir_context.control.DefaultContextProvider;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.package_resolver.entity.PackageResolution;
import java.util.Collection;
import org.jspecify.annotations.NonNull;

/** Provides a Context for Validation or Snapshot Generation operations. */
public interface ContextProvider {
  /**
   * Creates a context containing the base definitions of the selected FHIR version.
   *
   * @param contextConfiguration the context configuration
   * @return initialized validation context
   * @throws InitializationException if context creation fails
   */
  @NonNull ValidationContext validationContext(@NonNull ContextConfiguration contextConfiguration)
      throws InitializationException;

  /**
   * Creates a context containing base FHIR definitions and custom FHIR packages to be loaded,
   * before performing the validation.
   *
   * @param contextConfiguration the context configuration
   * @param packagesToLoad packages to be loaded in the context
   * @return initialized validation context
   * @throws InitializationException if context creation fails
   */
  @NonNull ValidationContext validationContext(
      @NonNull ContextConfiguration contextConfiguration,
      @NonNull Collection<String> packagesToLoad)
      throws InitializationException;

  /**
   * Clones an existing {@link ValidationContext} instance.
   *
   * @param validationContext an existing instance
   * @return a clone of an existing context
   */
  @NonNull ValidationContext clone(@NonNull ValidationContext validationContext);

  /**
   * Creates a context containing the base definitions of the selected FHIR version.
   *
   * @param contextConfiguration the context configuration
   * @return initialized validation context
   * @throws InitializationException if context creation fails
   */
  @NonNull SnapshotGenerationContext snapshotGenerationContext(
      @NonNull ContextConfiguration contextConfiguration) throws InitializationException;

  /**
   * Creates a context containing the base definitions of the selected FHIR version.
   *
   * @param contextConfiguration the context configuration
   * @param packageResolution resolved packages in*dependency-first order
   * @return initialized validation context
   * @throws InitializationException if context creation fails
   */
  @NonNull SnapshotGenerationContext snapshotGenerationContext(
      @NonNull ContextConfiguration contextConfiguration,
      @NonNull PackageResolution packageResolution)
      throws InitializationException;

  /**
   * Clones an existing {@link SnapshotGenerationContext} instance.
   *
   * @param snapshotGenerationContext an existing instance
   * @return a clone of an existing instance
   */
  @NonNull SnapshotGenerationContext clone(
      @NonNull SnapshotGenerationContext snapshotGenerationContext);

  /**
   * Creates the default provider for FHIR Contexts.
   *
   * @return a new instance of {@link DefaultContextProvider}
   */
  static ContextProvider defaultProvider() {
    return new DefaultContextProvider();
  }
}
