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

/// # config_parser
///
/// Creates a parser for the handling of JSON and YAML configuration files.
///
/// ## Boundary
///
/// - `getMapper` — creates an instance of a Mapper (YAML or JSON)
///
/// ## Requirements
///
/// ### R1: Configuration Source Loading
///
/// - R1.1 — The parser shall be able to load configuration from a specified source (e.g., file,
///   environment variable).
///
/// ## Out of scope
///
/// Parse Configuration directly
package de.gematik.refv.lib.config_parser;
