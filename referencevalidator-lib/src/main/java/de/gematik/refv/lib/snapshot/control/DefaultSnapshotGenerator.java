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
package de.gematik.refv.lib.snapshot.control;

import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolver;
import de.gematik.refv.lib.package_resolver.control.DefaultPackageResolver;
import de.gematik.refv.lib.snapshot.boundary.SnapshotGenerator;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationOptions;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationRequest;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationResult;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default FHIR Snapshot Generator Implementation of the Reference Validator. */
public class DefaultSnapshotGenerator implements SnapshotGenerator {
  private static final Logger log = LoggerFactory.getLogger(DefaultSnapshotGenerator.class);
  private final DefaultPackageProcessor packageProcessor;

  /**
   * Creates a Snapshot Generator with an internal resolver, based on the given configuration.
   *
   * @param contextConfiguration the configuration used for initializing both the context and the
   *     cache where packages are resolved
   */
  public DefaultSnapshotGenerator(@NonNull ContextConfiguration contextConfiguration) {
    Objects.requireNonNull(contextConfiguration, "The context configuration must be defined.");
    final PackageResolver packageResolver =
        new DefaultPackageResolver(contextConfiguration.packageLoading());
    final var corePackages = packageResolver.loadCore(contextConfiguration.fhirRelease());
    this.packageProcessor =
        new DefaultPackageProcessor(
            ContextProvider.defaultProvider()
                .snapshotGenerationContext(contextConfiguration, corePackages),
            packageResolver);
  }

  /**
   * Generates Snapshots from the given request, assumes that dependencies have been made locally
   * available in the cache or in a local directory.
   *
   * @param request a {@link SnapshotGenerationRequest} instance that points to one or FHIR
   *     Resources that act as source for the generation of snapshots
   * @param options a {@link SnapshotGenerationOptions} instance that contain options for modifying
   *     the execution of the snapshot generation operation.
   * @return a {@link SnapshotGenerationResult} instance containing information about the snapshot
   *     generation operation
   */
  @Override
  public @NonNull SnapshotGenerationResult generateSnapshots(
      @NonNull SnapshotGenerationRequest request, @NonNull SnapshotGenerationOptions options) {
    try {
      Objects.requireNonNull(request, "The request cannot be null");
      Objects.requireNonNull(options, "The options cannot be null");

      if (Files.notExists(request.outputDirectoryPath())) {
        log.info("Creating output directory {}", request.outputDirectoryPath());
        Files.createDirectory(request.outputDirectoryPath());
      }

      return packageProcessor.process(
          request.sourcePackagePath(),
          request.outputDirectoryPath(),
          options.patchesDirectory(),
          request.packageNames());
    } catch (Exception e) {
      log.error("Failed to generate snapshots: {}", e.getLocalizedMessage());
      log.debug(e.getLocalizedMessage(), e);
      return new SnapshotGenerationResult(
          List.of(
              ResultMessage.fromMessage(
                  IssueSeverity.ERROR,
                  MessageId.SNAPSHOT_GENERATION_ERROR.getCode(),
                  e.getLocalizedMessage())));
    }
  }

  @Override
  public void close() {
    packageProcessor.close();
  }
}
