/*-
 * #%L
 * Validation Module API
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
/// # validation-module-api
///
/// Defines the API and data structures for implementing a custom Validation Module.
///
/// ## Boundary
///
/// - `getName` — returns the name of the validation module.
///
/// ## Entities
///
/// - `{@link de.gematik.refv.valmodule.api.entity.ValidationModuleManifest}`
/// - `{@link de.gematik.refv.valmodule.api.entity.PackageGroup}`
/// - `{@link de.gematik.refv.valmodule.api.entity.ProfileFamily}`
/// - `{@link de.gematik.refv.valmodule.api.entity.MessageTransformation}`
/// - `{@link de.gematik.refv.valmodule.api.entity.SuppressionRule}`
/// - `{@link de.gematik.refv.valmodule.api.entity.PackageGroupReference}`
///
/// ## Requirements
///
/// ### R1: Manifest Structure
///
/// - R1.1 — The manifest shall contain a `configSpecVersion`, `name`, `version`, `author`, and
///   `description`.
/// - R1.2 — The manifest shall contain at least one valid `packageGroupName`.
/// - R1.3 — The manifest shall contain at least one valid `profileFamily`.
///
/// ### R2: Referential Integrity
///
/// - R2.1 — When a `packageGroupName` references a `messageTransformation`, then that
///   transformation shall be declared in the manifest.
/// - R2.2 — When a `profileFamily` version includes a `groups`, then the referenced name must point
///   to an existing `packageGroupName`.
///
/// ## Out of scope
///
/// - The actual loading of the module from the filesystem.
/// - The execution of the validation logic itself.
package de.gematik.refv.valmodule.api;
