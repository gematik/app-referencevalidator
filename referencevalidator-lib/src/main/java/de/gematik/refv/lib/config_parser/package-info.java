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

/// # Configuration Parser
///
/// > Provides configured JSON and YAML mappers for configuration consumers.
///
/// Callers select configuration sources and use the supplied mappers to read or write their
/// configuration types. A mapper is shared within its format; callers must not reconfigure it.
/// Consumers preserve the mapper's failure reason when translating exceptions or reporting an
/// invalid configuration; a generic loading message alone is not an actionable diagnostic.
///
/// ## Boundary
///
/// - `provide-json-mapper` — obtain the configured mapper for JSON configuration (R2).
/// - `provide-yaml-mapper` — obtain the configured mapper for YAML configuration (R3).
///
/// ## Requirements
///
/// Retired requirement: `R1.1` (configuration source loading). Source loading belongs to callers;
/// this identifier is not reused.
///
/// ### R2 — JSON mapper provision
///
/// - R2.1 When a caller requests a JSON mapper, the Configuration Parser shall return a non-null
///   configured mapper and shall return the same instance on subsequent JSON mapper requests.
/// - R2.2 When a caller uses the supplied JSON mapper to read date/time configuration values, the
///   Configuration Parser shall support those values, including calendar dates expressed as
///   ISO-8601 strings in the form `yyyy-MM-dd`.
/// - R2.3 When a caller uses the supplied JSON mapper to write calendar dates, the Configuration
///   Parser shall represent them as ISO-8601 strings rather than timestamp arrays.
/// - R2.4 If JSON configuration contains a property unknown to its target configuration type and
///   that type does not explicitly permit unknown properties, then the Configuration Parser shall
///   supply a mapper that rejects that property.
/// - R2.5 When a caller uses the supplied JSON mapper to write configuration, the Configuration
///   Parser shall supply a mapper that produces indented, human-readable JSON.
///
/// ### R3 — YAML mapper provision
///
/// - R3.1 When a caller requests a YAML mapper, the Configuration Parser shall return a non-null
///   configured mapper and shall return the same instance on subsequent YAML mapper requests.
/// - R3.2 When a caller uses the supplied YAML mapper to read date/time configuration values, the
///   Configuration Parser shall support those values, including calendar dates expressed as
///   ISO-8601 strings in the form `yyyy-MM-dd`.
/// - R3.3 When a caller uses the supplied YAML mapper to write calendar dates, the Configuration
///   Parser shall represent them as ISO-8601 strings rather than timestamp arrays.
/// - R3.4 If YAML configuration contains a property unknown to its target configuration type and
///   that type does not explicitly permit unknown properties, then the Configuration Parser shall
///   supply a mapper that rejects that property.
/// - R3.5 When a caller uses the supplied YAML mapper to write configuration, the Configuration
///   Parser shall supply a mapper that produces indented, human-readable YAML.
/// - R3.6 If the supplied YAML mapper rejects an unknown property or an incompatible field value,
///   then the Configuration Parser shall expose a failure diagnostic identifying the offending
///   field and the reason for rejection.
/// - R3.7 If the supplied YAML mapper rejects malformed YAML, then the Configuration Parser shall
///   expose the parsing reason and the source line and column when that location is available.
///
/// ## Out of scope
///
/// - Select or read configuration sources, including files and environment variables.
/// - Parse configuration directly through additional component operations.
/// - Define configuration types, domain validation, defaults, or source precedence.
/// - Wrap parsing failures in library configuration exceptions or render user-facing diagnostics;
///   configuration consumers own exception translation and clear error reporting, preserving the
///   underlying diagnostic and adding the configuration source identity when available.
/// - Guarantee exact serialized whitespace or layout beyond human-readable indentation.
package de.gematik.refv.lib.config_parser;
