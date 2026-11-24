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
import de.gematik.refv.lib.fhir_context.control.DefaultBatchValidationContextProvider;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import java.util.Collection;
import org.jspecify.annotations.NonNull;

/** Provides isolated validation contexts backed by reusable templates for one batch. */
public interface BatchValidationContextProvider extends AutoCloseable {
  /** Acquires the package set template until the returned lease is closed. */
  @NonNull TemplateLease acquire(@NonNull Collection<String> packageCoordinates)
      throws InitializationException, InterruptedException;

  @Override
  void close();

  /** A lease keeps its package set template alive while a resource is being validated. */
  interface TemplateLease extends AutoCloseable {
    /** Creates a new resource-isolated validation context. */
    @NonNull ValidationContext newIsolatedContext();

    @Override
    void close();
  }

  /**
   * Creates a default batch-scoped provider that reuses templates while returning an isolated
   * context for each validation resource.
   *
   * @param contextConfiguration configuration shared by the batch
   * @param maximumTemplates maximum number of package-set templates retained by the batch
   * @return an independently managed batch-scoped provider
   */
  static BatchValidationContextProvider defaultProvider(
      @NonNull ContextProvider contextProvider,
      @NonNull ContextConfiguration contextConfiguration,
      int maximumTemplates) {
    return new DefaultBatchValidationContextProvider(
        contextProvider, contextConfiguration, maximumTemplates);
  }
}
