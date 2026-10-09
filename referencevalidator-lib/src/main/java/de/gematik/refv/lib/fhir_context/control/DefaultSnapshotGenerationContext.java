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
import de.gematik.refv.lib.exceptions.SnapshotGenerationException;
import de.gematik.refv.lib.fhir_context.boundary.SnapshotGenerationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.package_resolver.entity.PackageResolution;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.model.core.ElementDefinition;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.model.core.formats.JsonParser;
import org.hl7.fhir.standalone.context.ContextUtilities;
import org.hl7.fhir.standalone.context.SimpleWorkerContext;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.validation.ValidationEngine;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for initializing the FHIR Context used for snapshot generation.
 * Internally it holds a {@link ValidationEngine} instance, which is the core of the HL7 Validation
 * Library. The ValidationEngine manages internally dependencies and resources, so they can be used
 * directly. The clone of an existing ValidationEngine is not deep, instead it attempts to reusing
 * previously loaded dependencies, so the initialization is sped up.
 */
class DefaultSnapshotGenerationContext implements SnapshotGenerationContext {
  private static final Logger log = LoggerFactory.getLogger(DefaultSnapshotGenerationContext.class);
  private final ContextConfiguration contextConfiguration;
  private final ValidationEngine validationEngine;
  private final ManagedTemporaryDirectory terminologyCacheLease;

  public DefaultSnapshotGenerationContext(@NonNull ContextConfiguration contextConfiguration)
      throws InitializationException {
    this.contextConfiguration = contextConfiguration;
    try {
      final var engineSession = ValidationEngineFactory.createNewCoreEngine(contextConfiguration);
      this.validationEngine = engineSession.engine();
      this.terminologyCacheLease =
          engineSession.managedTerminologyCachePath() == null
              ? null
              : ManagedTemporaryDirectory.owned(engineSession.managedTerminologyCachePath());
    } catch (Exception e) {
      close();
      throw new InitializationException(e.getLocalizedMessage(), e);
    }
  }

  public DefaultSnapshotGenerationContext(
      @NonNull ContextConfiguration contextConfiguration, @NonNull PackageResolution packagesToLoad)
      throws InitializationException {
    Objects.requireNonNull(packagesToLoad, "The list of packages to load set must be supplied");
    this.contextConfiguration = contextConfiguration;
    try {
      final var engineSession = ValidationEngineFactory.createNewCoreEngine(contextConfiguration);
      this.validationEngine = engineSession.engine();
      this.terminologyCacheLease =
          engineSession.managedTerminologyCachePath() == null
              ? null
              : ManagedTemporaryDirectory.owned(engineSession.managedTerminologyCachePath());
      ContextPackageLoader.loadPackagesInContext(this.validationEngine, packagesToLoad.packages());
    } catch (Exception e) {
      close();
      throw new InitializationException(e.getLocalizedMessage(), e);
    }
  }

  /**
   * Returns the path to the cache where packages are stored.
   *
   * @return the path to the cache store
   */
  @Override
  public @NonNull Path cachePath() {
    return contextConfiguration.packageLoading().cachePath();
  }

  @Override
  public @NonNull FhirRelease fhirVersion() {
    return contextConfiguration.fhirRelease();
  }

  @Override
  public @NonNull SnapshotGenerationResult generateSnapshots(@NonNull Path packageSource)
      throws SnapshotGenerationException {
    NpmPackage npmPackage = null;
    try {
      // Load package resources manually because dependency workspaces may be temporary.
      npmPackage = NpmPackage.fromFolder(packageSource.toString(), false);
      final var structureDefinitionsFromPackage =
          ContextPackageLoader.extractAndLoadResourcesInContext(validationEngine, npmPackage);
      if (structureDefinitionsFromPackage.isEmpty()) {
        final String message =
            String.format(
                "No structure definitions found for package %s, skipping snapshot generation",
                npmPackage.name());
        log.info(message);
        return new SnapshotGenerationResult(
            List.of(
                ResultMessage.fromMessage(
                    IssueSeverity.WARNING, MessageId.NO_ERROR.getCode(), message)));
      }

      // generate snapshots
      generateSnapshots(validationEngine.getContext(), structureDefinitionsFromPackage.keySet());
      // Retrieve the StructureDefinitions with Snapshot and write them to folder (overwrite
      // existing)
      storeStructureDefinitionSnapshots(
          validationEngine.getContext(), structureDefinitionsFromPackage);

      return new SnapshotGenerationResult(
          List.of(
              ResultMessage.fromMessage(
                  IssueSeverity.INFORMATION,
                  MessageId.NO_ERROR.getCode(),
                  "Generation completed without issues for %s#%s"
                      .formatted(npmPackage.name(), npmPackage.version()))));

    } catch (Exception e) {
      log.error("Failed to generate snapshots: {}", e.getLocalizedMessage(), e);
      final String message;
      if (Objects.nonNull(npmPackage)) {
        message =
            "Failed to generate snapshots for %s#%s"
                .formatted(npmPackage.name(), npmPackage.version());
      } else {
        message = "Failed to generate snapshots for " + packageSource;
      }
      return new SnapshotGenerationResult(
          List.of(
              ResultMessage.fromMessage(
                  IssueSeverity.ERROR, MessageId.SNAPSHOT_GENERATION_ERROR.getCode(), message)));
    }
  }

  @Override
  public @NonNull SnapshotGenerationContext cloneContext(
      @NonNull Collection<ResolvedPackage> resolvedPackages) {
    log.debug("Cloning an existing Context and loading custom dependencies");
    return new DefaultSnapshotGenerationContext(this, resolvedPackages);
  }

  @Override
  public void close() {
    if (terminologyCacheLease != null) {
      terminologyCacheLease.close();
    }
  }

  /**
   * Initializes internally a new FHIR context with the one of a previously initialized FHIR Context
   * and additionally with the supplied {@link ResolvedPackage} content. Packages are not resolved
   * recursively.
   *
   * @param other the source FHIR Context to use
   * @param packages a list of {@link ResolvedPackage} to be imported
   * @throws InitializationException in case of initialization errors
   */
  private DefaultSnapshotGenerationContext(
      @NonNull DefaultSnapshotGenerationContext other,
      @NonNull Collection<ResolvedPackage> packages)
      throws InitializationException {
    Objects.requireNonNull(other, "A valid context must be supplied");
    Objects.requireNonNull(packages, "A list of packages must be supplied");
    try {
      this.contextConfiguration = other.contextConfiguration;
      this.validationEngine = new ValidationEngine(other.validationEngine);
      this.terminologyCacheLease =
          other.terminologyCacheLease == null ? null : other.terminologyCacheLease.retain();
      log.trace(
          "Source Snapshot Generation Context: {}",
          System.identityHashCode(other.validationEngine));
      log.trace(
          "New Snapshot Generation Context: {}", System.identityHashCode(this.validationEngine));
      ContextPackageLoader.loadPackagesInContext(this.validationEngine, packages);
    } catch (Exception e) {
      close();
      throw new InitializationException(
          "Failed to create a new custom Snapshot Generation Context: " + e.getLocalizedMessage(),
          e);
    }
  }

  /**
   * Generates Snapshots for the given collection of {@link StructureDefinition}, using the given
   * context. Internally, after the generation of snapshots, these are cached in context for direct
   * usage.
   *
   * @param structureDefinitions the collection of StructureDefinitions that need to be processed
   * @throws SnapshotGenerationException in case of relevant errors during the snapshot generation.
   */
  private void generateSnapshots(
      @NonNull SimpleWorkerContext workerContext,
      Collection<StructureDefinition> structureDefinitions)
      throws SnapshotGenerationException {
    final var contextUtilities = new ContextUtilities(workerContext);

    try {
      for (var sdFromPackage : structureDefinitions) {
        log.debug(
            "Processing {}|{} object={}",
            sdFromPackage.getUrl(),
            sdFromPackage.getVersion(),
            System.identityHashCode(sdFromPackage));
        generateSnapshotIfNeeded(contextUtilities, workerContext, sdFromPackage);
      }
    } catch (Exception e) {
      throw new SnapshotGenerationException(e.getLocalizedMessage(), e);
    }
  }

  private void generateSnapshotIfNeeded(
      ContextUtilities contextUtilities,
      SimpleWorkerContext workerContext,
      StructureDefinition structureDefinition) {
    if (structureDefinition.hasDifferential()) {
      assertUniqueElementIds(
          structureDefinition,
          structureDefinition.getDifferential().getElementList(),
          "differential");
    }
    boolean hasSnapshot =
        structureDefinition.hasSnapshot() && structureDefinition.getSnapshot().hasElement();
    if (structureDefinition.hasBaseDefinition() && !hasSnapshot) {
      log.debug(
          "Generating snapshot for {}|{} object={}",
          structureDefinition.getUrl(),
          structureDefinition.getVersion(),
          System.identityHashCode(structureDefinition));
      contextUtilities.generateSnapshot(structureDefinition);
    }
    if (structureDefinition.hasSnapshot()) {
      assertUniqueElementIds(
          structureDefinition, structureDefinition.getSnapshot().getElementList(), "snapshot");
    }
    workerContext.cacheResource(structureDefinition);
  }

  static void assertUniqueElementIds(
      StructureDefinition structureDefinition,
      List<ElementDefinition> elements,
      String representation) {
    var counts =
        elements.stream()
            .filter(ElementDefinition::hasId)
            .collect(
                Collectors.groupingBy(
                    ElementDefinition::getId, LinkedHashMap::new, Collectors.counting()));
    var duplicateIds =
        counts.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .toList();
    if (duplicateIds.isEmpty()) {
      return;
    }

    var duplicateElements =
        elements.stream()
            .filter(element -> duplicateIds.contains(element.getId()))
            .map(
                element ->
                    "id="
                        + element.getId()
                        + ", path="
                        + element.getPath()
                        + ", slice="
                        + element.getSliceName())
            .toList();
    throw new SnapshotGenerationException(
        "Duplicate element IDs in "
            + representation
            + " for "
            + structureDefinition.getUrl()
            + "|"
            + structureDefinition.getVersion()
            + ": "
            + duplicateElements);
  }

  void storeStructureDefinitionSnapshots(
      @NonNull SimpleWorkerContext context,
      Map<StructureDefinition, Path> structureDefinitionsFromPackage)
      throws IOException {
    final var jsonParser = new JsonParser(context.getModelContext());
    for (var entry : structureDefinitionsFromPackage.entrySet()) {
      var sd = entry.getKey();
      // Check that StructureDefinition has a snapshot now
      if (sd.hasSnapshot() && sd.getSnapshot().hasElement()) {
        log.debug("StructureDefinition '{}' has been generated correctly", sd.getName());
      } else {
        log.warn("StructureDefinition '{}' does not contain a snapshot", sd.getName());
      }

      Files.write(
          entry.getValue(), jsonParser.composeBytes(sd), StandardOpenOption.TRUNCATE_EXISTING);
    }
  }
}
