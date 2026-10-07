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
/// # Snapshot
/// > Generate and archive snapshot packages for an Implementation Guide and its transitive package
/// > dependencies.
///
/// ## Boundary
/// - `create-generator` — creates a snapshot generator using supplied context configuration.
/// - `generate-snapshots` — generates and archives snapshots for selected package roots and their
///   transitive dependencies.
/// - `close-generator` — releases resources owned by a snapshot generator.
///
/// ## Requirements
///
/// ### R1 — Generate snapshots
/// - R1.1 When a coordinate-named local package directory and output directory are supplied, the
///   Snapshot shall generate snapshots for the selected packages and their transitive dependencies.
/// - R1.2 When a package declares dependencies, the Snapshot shall obtain the dependency packages
///   through Package Resolver and compute the complete transitive dependency graph before generating
///   any snapshots.
/// - R1.3 If the complete dependency graph contains a cycle or a required package cannot be
///   resolved, then the Snapshot shall return an error result without starting snapshot generation.
/// - R1.4 When the dependency graph is acyclic, the Snapshot shall generate packages in
///   dependency-first order and provide each package with the archived snapshots of its dependencies.
/// - R1.5 When a package coordinate occurs more than once in the selected roots or dependency graph,
///   the Snapshot shall generate and archive that package at most once per request.
/// - R1.6 When a package is generated successfully, the Snapshot shall write its snapshot package
///   archive to the supplied output directory.
/// - R1.7 When a package has a matching `name#version` subdirectory in the supplied patches
///   directory, the Snapshot shall apply that package's patches before generating its snapshot.
/// - R1.8 If snapshot generation fails for a package, then the Snapshot shall not generate packages
///   that depend on it, shall continue with unrelated packages, and shall retain archives already
///   generated successfully.
/// - R1.9 When any package generation returns an ERROR or FATAL message, the Snapshot shall report
///   the request as failed in its result while including the package outcomes.
/// - R1.10 When the source package directory is invalid or package processing fails before
///   generation, the Snapshot shall return an error result.
/// - R1.11 The Snapshot shall return results whose internal contents cannot be mutated by callers.
/// - R1.12 The Snapshot shall not expose concrete HL7 types through its boundary.
///
/// ### R2 — Create a generator
/// - R2.1 When context configuration is supplied, the Snapshot shall create a generator.
/// - R2.2 If a generator cannot be initialized, then the Snapshot shall report an initialization
///   failure.
///
/// ### R3 — Close a generator
/// - R3.1 When a generator is closed, the Snapshot shall release the resources it owns.
/// - R3.2 When a closed generator is closed again, the Snapshot shall leave it closed without
///   additional effects.
///
/// ## Entities
/// - `SnapshotGenerationRequest` identifies the local source package directory, output directory,
///   and optional selected root package coordinates.
/// - `SnapshotGenerationOptions` supplies optional package-specific patches.
/// - `SnapshotGenerationResult` contains the messages describing package generation outcomes.
///
/// ## Out of scope
/// - Configure the FHIR context or construct snapshot-generation options.
/// - Implement package resolution or package loading; Snapshot obtains dependency packages through
///   the Package Resolver boundary.
/// - Load or configure validation modules.
/// - The control does not retain requests, outcomes, or results.
/// - An output byte array is defensively copied at the public boundary.
/// - Validation findings are copied once into an immutable list.
/// - The validation context engine is reused rather than reconstructed.
package de.gematik.refv.lib.snapshot;
