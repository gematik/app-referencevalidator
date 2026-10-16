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
/// # Package Resolver
///
/// > Resolves FHIR packages from configured sources and returns their package identities, paths,
// and
/// > dependencies in their usage order.
///
/// ## Boundary
///
/// - `create-resolver` — creates a resolver from package-download configuration.
/// - `resolve-request` — resolves requested package sources, optionally including transitive
///   dependencies.
/// - `resolve-list` — resolves a collection of package coordinates or filesystem paths.
/// - `resolve-path` — resolves a remote coordinate, local package directory, archive, or archive
///   directory supplied as a path.
/// - `match-package` — returns one resolved package matching a package coordinate.
/// - `load-core` — loads the embedded packages for a FHIR release.
/// - `load-module-packages` — imports packages embedded in a validation-module JAR.
/// - `report-cache-path` — returns the configured package-cache path.
///
/// Resolved packages are returned in dependency-first order when transitive resolution is enabled.
/// Package coordinates use the form `name#version`; a version may use a wildcard expression where
/// package lookup supports it. A resolution contains each package's identity, cache path, and
// direct
/// dependencies. Public operations expose library package types rather than HL7 package types.
///
/// ## Requirements
///
/// ### R1 — Resolve package requests
///
/// - R1.1 When transitive resolution is enabled, the Package Resolver shall include the requested
///   packages and their transitive dependencies in dependency-first order.
/// - R1.2 When transitive resolution is ignored, the Package Resolver shall return only the
///   requested packages without resolving their dependencies.
/// - R1.3 If a required package cannot be loaded under the configured download policy, then the
///   Package Resolver shall report a package-load failure.
///
/// ### R2 — Resolve package lists
///
/// - R2.1 When a list of package coordinates or filesystem paths is supplied, the Package Resolver
///   shall resolve each entry and return the resulting packages in input order.
///
/// ### R3 — Resolve a path
///
/// - R3.1 When a path identifies a local `.tgz` package archive, the Package Resolver shall resolve
///   that archive as a package.
/// - R3.2 When a path identifies a coordinate-named local package directory, the Package Resolver
///   shall resolve that package and its transitive dependencies.
/// - R3.3 When a path identifies a directory without a package coordinate in its name, the Package
///   Resolver shall resolve the `.tgz` archives directly contained in that directory without
///   traversing their dependencies.
/// - R3.4 When a non-existing path has the form `name#version`, the Package Resolver shall treat it
///   as a remote package coordinate and resolve its transitive dependencies.
///
/// ### R4 — Match a package
///
/// - R4.1 When a package coordinate identifies one available package, the Package Resolver shall
///   return that package.
/// - R4.2 When a package coordinate uses a supported wildcard version expression, the Package
///   Resolver shall return one matching available package.
///
/// ### R5 — Load FHIR Core packages
///
/// - R5.1 When a FHIR release is supplied, the Package Resolver shall load the embedded package set
///   for that release.
///
/// ### R6 — Load module packages
///
/// - R6.1 When a validation-module JAR contains package archives, the Package Resolver shall import
///   those packages into its configured cache.
///
/// ### R7 — Report the package cache
///
/// - R7.1 When a resolver is created with a configured cache path, the Package Resolver shall
///   report that path.
///
/// ## Entities
///
/// - `PackageId` identifies a package by name and version, represented as `name#version`.
/// - `PackageCategory` represents a package source; `RemotePackage`, `LocalDirectory`, and
///   `LocalArchive` identify remote coordinates, local package directories, and local `.tgz`
///   archives, respectively.
/// - `PackageResolutionRequest` contains the package sources to resolve.
/// - `ResolvedPackage` contains a package identity, cache location, and direct dependencies.
/// - `PackageResolution` contains the ordered resolved packages returned for a request.
/// - `DependencyGraph` models package dependencies with edges from each dependency to the package
///   that requires it; its topological order places every dependency before its dependent package.
///
/// ## Out of scope
///
/// - Select packages based on a validation module's profile or validity-period rules.
/// - Validate FHIR resources or generate snapshots from resolved packages.
package de.gematik.refv.lib.package_resolver;
