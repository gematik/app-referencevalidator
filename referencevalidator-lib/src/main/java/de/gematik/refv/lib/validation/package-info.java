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
/// # Validation Context component
///
/// Owns construction, lifecycle, and safe internal access to initialized FHIR engines.
///
/// ## Rules
///
/// - `boundary` exposes opaque, closeable contexts and their factory.
/// - `control` constructs contexts but does not perform validation or snapshot generation.
/// - `entity` owns concrete context state.
/// - Consumers never receive a concrete engine or worker-context type.
///
/// ## Memory rules
///
/// - One engine is retained per context and reused for all operations.
/// - Closing a context deterministically releases engine-owned caches.
/// - No global context registry or String-to-context lookup is used.
package de.gematik.refv.lib.validation;
