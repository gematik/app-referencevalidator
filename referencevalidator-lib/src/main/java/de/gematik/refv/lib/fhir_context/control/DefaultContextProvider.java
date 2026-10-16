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
import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.SnapshotGenerationContext;
import de.gematik.refv.lib.fhir_context.boundary.ValidationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolver;
import de.gematik.refv.lib.package_resolver.control.DefaultPackageResolver;
import de.gematik.refv.lib.package_resolver.entity.PackageResolution;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * This class is responsible for initializing the FHIR Context used for validation and
 * snapshot-generation. Internally it holds a ValidationEngine instance, which is the core of the
 * HL7 Validation Library. The ValidationEngine manages internally dependencies and resources, so
 * they can be used directly. The clone of an existing ValidationEngine is not deep, instead it
 * attempts to reusing previously loaded dependencies, so the initialization is sped up.
 */
public record DefaultContextProvider() implements ContextProvider {
  @Override
  public @NonNull ValidationContext validationContext(
      @NonNull ContextConfiguration contextConfiguration) throws InitializationException {
    final PackageResolver packageResolver =
        new DefaultPackageResolver(contextConfiguration.packageLoading());
    final var resolvedPackages = packageResolver.loadCore(contextConfiguration.fhirRelease());
    return new DefaultValidationContext(contextConfiguration, resolvedPackages.packages());
  }

  @Override
  public @NonNull ValidationContext validationContext(
      @NonNull ContextConfiguration contextConfiguration,
      @NonNull Collection<String> packagesToLoad)
      throws InitializationException {
    final PackageResolver packageResolver =
        new DefaultPackageResolver(contextConfiguration.packageLoading());
    final var resolvedPackages = packageResolver.resolveList(packagesToLoad);
    return new DefaultValidationContext(contextConfiguration, resolvedPackages.packages());
  }

  @Override
  public @NonNull ValidationContext clone(@NonNull ValidationContext validationContext) {
    return validationContext.cloneContext(List.of());
  }

  @Override
  public @NonNull SnapshotGenerationContext snapshotGenerationContext(
      @NonNull ContextConfiguration contextConfiguration) throws InitializationException {
    return new DefaultSnapshotGenerationContext(contextConfiguration);
  }

  @Override
  public @NonNull SnapshotGenerationContext snapshotGenerationContext(
      @NonNull ContextConfiguration contextConfiguration,
      @NonNull PackageResolution packageResolution)
      throws InitializationException {
    return new DefaultSnapshotGenerationContext(contextConfiguration, packageResolution);
  }

  @Override
  public @NonNull SnapshotGenerationContext clone(
      @NonNull SnapshotGenerationContext snapshotGenerationContext) {
    return snapshotGenerationContext.cloneContext(List.of());
  }
}
