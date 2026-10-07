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
/// # FHIR Context
///
/// > Creates configured FHIR processing contexts for resource validation and snapshot generation.
///
/// `ContextConfiguration` selects the FHIR release, locale, message display behavior, package
/// loading policy, terminology settings, and validation policies. A context reports its configured
/// FHIR release and supports validation or snapshot generation according to its public interface.
/// The properties are used for configuring an internal FHIR Engine, currently based on the HL7 Core
/// validator.
///
/// ## Boundary
///
/// - `create-validation-context` — create a validation context from configuration (R1).
/// - `create-validation-context-with-packages` — create a validation context with packages (R1).
/// - `clone-validation-context` — clone a validation context (R4).
/// - `clone-validation-context-with-packages` — clone a validation context with packages (R4).
/// - `create-snapshot-generation-context` — create a snapshot-generation context (R1).
/// - `create-snapshot-generation-context-with-packages` — create one with resolved packages (R1).
/// - `clone-snapshot-generation-context` — clone a snapshot-generation context (R4).
/// - `clone-snapshot-generation-context-with-packages` — clone one with resolved packages (R4).
/// - `report-fhir-release` — report the configured FHIR release (R1).
/// - `validate-resource` — validate a resource against supplied profiles (R2).
/// - `generate-snapshots` — generate snapshots for a package directory (R3).
/// - `report-package-cache-path` — return the configured package cache path (R5).
/// - `open-batch-validation-context-provider` — open a batch-scoped provider for isolated contexts
///   backed by reusable package-set templates (R6).
///
/// Context operations use the library's resource, profile, package-resolution, and result types.
/// Concrete HL7 validation-engine types are not exposed in context operation signatures.
///
/// ## Requirements
///
/// ### R1 — Context creation
///
/// - R1.1 When a context is created from a configuration, the FHIR Context shall report the FHIR
///   release selected by that configuration.
/// - R1.2 When a validation context is created without additional packages, the FHIR Context shall
///   make the base definitions (core packages) for its configured FHIR release available for
///   validation.
/// - R1.3 When a context is created with resolved packages, the FHIR Context shall make those
///   packages available to its validation or snapshot-generation operations.
///
/// ### R2 — Resource validation
///
/// - R2.1 When a resource is validated with one or more supplied profiles, the FHIR Context shall
///   validate the resource against those profiles and return the validation result.
/// - R2.2 When a resource is validated with no supplied profiles, the FHIR Context shall validate
///   the resource against the core FHIR structures for its configured release.
/// - R2.3 If a supplied profile is not available in the validation context, then the FHIR Context
///   shall report a validation failure.
///
/// ### R3 — Snapshot generation
///
/// - R3.1 When a package directory contains StructureDefinitions that have a base definition but no
///   snapshot, the FHIR Context shall generate their snapshots and write the generated definitions
///   back to the package directory.
/// - R3.2 If a package directory contains no StructureDefinitions, then the FHIR Context shall
///   return a warning result indicating that snapshot generation was skipped.
///
/// ### R4 — Context cloning
///
/// - R4.1 When a validation context is cloned with resolved packages, the FHIR Context shall retain
///   the source context's configuration and make the supplied packages available to validation.
/// - R4.2 When a snapshot-generation context is cloned with resolved packages, the FHIR Context
///   shall retain the source context's configuration and make the supplied packages available to
///   snapshot generation.
///
/// ### R5 — Package cache
///
/// - R5.1 The FHIR Context shall return the configured package cache path.
///
/// ### R6 — Batch validation contexts
///
/// - R6.1 When a batch requests a package set, the FHIR Context shall reuse its initialized
///   template while that package set remains retained by the batch provider.
/// - R6.2 When a resource requests a context from a package-set lease, the FHIR Context shall
/// return
///   an isolated clone and shall not expose the reusable template for validation.
/// - R6.3 When a batch provider is closed, the FHIR Context shall release its retained templates
///   after active leases have been released.
///
/// ## Entities
///
/// - `ContextConfiguration` selects a FHIR release, locale, message display behavior, package
///   loading policy, terminology settings, and validation policies.
/// - `FhirRelease` identifies the supported FHIR release; R4 and R5 are the supported releases.
/// - `DisplayBehaviorConfiguration` defines which validation messages and hints are displayed.
/// - `PackageDownloadConfiguration` defines whether remote package retrieval is allowed and where
///   package files are cached.
/// - `TerminologyConfiguration` defines terminology-server access and terminology cache settings.
/// - `ZtsTerminologyServerConfiguration` identifies the endpoints of a ZTS terminology server.
/// - `ValidationPolicyConfiguration` defines policy choices used when validating resources.
/// - `ValidationModuleIndex` resolves module profile versions, validity periods, package groups,
///   and message transformations for validation.
/// - `ProfileValidity` pairs a profile canonical with an optional resource validity date.
/// - `ContextResult` represents operation messages; `ResultMessage` carries message content, an
///   identifier, and its `IssueSeverity`.
/// - `MessageId` identifies a recognized validator message category.
///
/// ## Out of scope
///
/// - Resolve or Download the FHIR Packages from a remote server or a local cache.
/// - Define the Validation workflow
/// - Define the Snapshot Generation workflow
package de.gematik.refv.lib.fhir_context;
