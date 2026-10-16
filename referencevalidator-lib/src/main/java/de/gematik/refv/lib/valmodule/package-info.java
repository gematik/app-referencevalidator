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
/// # Validation Module
///
/// > Finds validation modules and imports their manifests without loading their FHIR packages.
///
/// ## Boundary
///
/// - `load-validation-module` (`forValidation`) — find a named module and return its manifest and
///   location (R1).
/// - `load-snapshot-module` (`forSnapshotGeneration`) — import a manifest from a module root (R2).
/// - `import-module-manifest` (`importFromStream`) — import a supplied manifest source (R3).
/// - `create-module-loader` (`defaultLoader`) — obtain a loader for validation modules (R4).
///
/// ## Requirements
///
/// ### R1 — Find a validation module
///
/// - R1.1 When a valid module matching the requested name is found, the BC shall return its parsed
///   manifest and the path of that module.
/// - R1.2 If no valid matching module is found in a valid search directory, then the BC shall return
///   an empty result.
/// - R1.3 If multiple valid modules match the requested name, then the BC shall return the first
///   matching module encountered during the search without choosing by release version.
/// - R1.4 When a module name is compared with the requested name, the BC shall compare the names
///   case-insensitively.
/// - R1.5 When searching a directory, the BC shall consider only JAR files directly within that
///   directory, accepting the JAR filename extension case-insensitively.
/// - R1.6 If a candidate JAR does not itself contain both its manifest and its SPI service
///   registration, then the BC shall skip that candidate.
/// - R1.7 If a candidate JAR cannot be read or its manifest cannot be imported, then the BC shall
///   report a diagnostic identifying that candidate and the failure reason and continue searching.
/// - R1.8 If the search path is missing or is not a directory, then the BC shall report a
///   module-loading failure.
/// - R1.9 If the search directory or requested name is absent, then the BC shall reject the request.
/// - R1.10 If the search directory cannot be listed, then the BC shall report a module-loading
///   failure retaining the underlying cause.
/// - R1.11 When loading a module, the BC shall read its metadata without instantiating its SPI
///   provider or importing its FHIR packages.
/// - R1.12 If a loader does not support validation-module search, then the BC shall report an
///   unsupported-operation module-loading failure.
///
/// ### R2 — Load a module for snapshot generation
///
/// - R2.1 When a module root containing a valid manifest is supplied, the BC shall return the parsed
///   manifest and that module root as the module location.
/// - R2.2 If the manifest below the module root is missing or unreadable, then the BC shall report a
///   module-loading failure retaining the underlying cause.
/// - R2.3 If the manifest below the module root is invalid, then the BC shall report a module-loading
///   failure retaining the manifest diagnostic and underlying cause.
/// - R2.4 If the module root is absent, then the BC shall reject the request.
/// - R2.5 If a loader does not support snapshot-module loading, then the BC shall report an
///   unsupported-operation module-loading failure.
///
/// ### R3 — Import a module manifest
///
/// - R3.1 When a valid manifest source is supplied, the BC shall return its parsed configuration.
/// - R3.2 If the manifest source is absent, then the BC shall return an empty result.
/// - R3.3 If a manifest cannot be parsed or contains an unknown field or an incompatible value,
///   then the BC shall report the import failure with the underlying parsing diagnostic.
/// - R3.4 If the manifest importer is created without its required mapper, then the BC shall reject
///   its creation.
///
/// ### R4 — Create a module loader
///
/// - R4.1 When the default module loader is requested, the BC shall return a loader supporting the
///   validation-module search contract.
///
/// ## Entities
///
/// - `ValidationModule` pairs a parsed manifest with the module location. Its configuration and
///   location are required. For validation the location is the JAR, and for snapshot generation it
///   is the module root.
///
/// Manifests reside at `META-INF/config.yaml`. SPI registrations reside at
/// `META-INF/services/de.gematik.refv.valmodule.api.boundary.FhirValidationModule`.
/// The module release version is metadata, not a search priority or an engine compatibility check.
///
/// ## Out of scope
///
/// - Import, resolve, or download FHIR packages from a module.
/// - Select packages by resource profile or validity date.
/// - Execute SPI providers, validate resources, or generate snapshots.
/// - Select the newest module release or negotiate engine compatibility.
/// - Guarantee a stable ordering of the filesystem directory listing.
package de.gematik.refv.lib.valmodule;
