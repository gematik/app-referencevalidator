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
package de.gematik.refv.lib.fhir_context.control;

import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.fhir_context.boundary.BatchValidationContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.ValidationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import java.util.Collection;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/** Control implementation for batch-scoped validation context provisioning. */
public record DefaultBatchValidationContextProvider(
    @NonNull ContextConfiguration configuration,
    @NonNull BatchValidationContextTemplateManager manager)
    implements BatchValidationContextProvider {

  /** Default constructor. */
  public DefaultBatchValidationContextProvider {
    Objects.requireNonNull(configuration, "The context configuration must not be null");
    Objects.requireNonNull(manager, "The context manager must not be null");
  }

  /**
   * Constructs a new instance from the {@link ContextProvider}, configuration and number of maximum
   * templates allowed.
   */
  public DefaultBatchValidationContextProvider(
      @NonNull ContextProvider contextProvider,
      @NonNull ContextConfiguration configuration,
      int maximumTemplates) {
    this(
        configuration,
        new BatchValidationContextTemplateManager(
            Objects.requireNonNull(contextProvider, "The contextProvider must not be null"),
            maximumTemplates));
  }

  /** Acquires the package-set template until the returned lease is closed. */
  @Override
  public BatchValidationContextProvider.@NonNull TemplateLease acquire(
      @NonNull Collection<String> packageCoordinates)
      throws InitializationException, InterruptedException {
    return new TemplateLease(manager.acquire(configuration, packageCoordinates));
  }

  @Override
  public void close() {
    manager.close();
  }

  /** A lease keeps its package-set template alive while a resource is being validated. */
  private record TemplateLease(BatchValidationContextTemplateManager.TemplateLease delegate)
      implements BatchValidationContextProvider.TemplateLease {

    /** Creates a new resource-isolated validation context. */
    @Override
    public @NonNull ValidationContext newIsolatedContext() {
      return delegate.newIsolatedContext();
    }

    @Override
    public void close() {
      delegate.close();
    }
  }
}
