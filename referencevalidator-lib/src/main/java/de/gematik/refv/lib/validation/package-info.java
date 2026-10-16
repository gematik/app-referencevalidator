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
/// # Validation
///
/// > Validates FHIR resources and applies module-specific profile, package, and result policies.
///
/// ## Boundary
///
/// - `create-core-validator` — create a validator using configured core FHIR definitions (R1).
/// - `create-package-validator` — create a validator with configured additional packages (R1).
/// - `create-context-validator` — create a validator using a caller-owned context (R1).
/// - `validate-resource` — validate a resource and return policy-processed messages (R2, R6).
/// - `create-profile-validity-extractor` — obtain an extractor for module profile validity (R3).
/// - `extract-profile-validity` — resolve a resource's supported profile and validity date (R3, R4).
/// - `select-resource-packages` — select module dependencies for a resource (R5).
/// - `close-validator` — release resources owned by a validator (R7).
///
/// ## Requirements
///
/// ### R1 — Validator creation
///
/// - R1.1 When a core validator is requested with a configuration, the BC shall create a validator
///   using the core definitions for the configured FHIR release without additional packages.
/// - R1.2 When a package validator is requested with a configuration and additional packages, the
///   BC shall make those packages available before accepting validation operations.
/// - R1.3 When a validator is requested with an initialized caller-owned context, the BC shall use
///   that context directly without cloning it or taking ownership of it.
/// - R1.4 If validator initialization fails or a required creation input is absent, then the BC
///   shall report an initialization failure retaining the underlying cause.
///
/// ### R2 — Resource validation
///
/// - R2.1 When a validation request supplies a JSON or XML FHIR resource and validation options,
///   the BC shall validate that resource using the validator's context and return the processed
///   validation result.
/// - R2.2 When validation options specify a profile, the BC shall request validation against that
///   profile, including its version when supplied.
/// - R2.3 When validation options specify no profile, the BC shall request validation without an
///   additional explicit profile, leaving core and resource-declared profile checks to the context.
/// - R2.4 If a validation request or its options are absent, or validation or result processing
///   fails, then the BC shall report a validation failure retaining the underlying cause.
///
/// ### R3 — Profile extraction
///
/// - R3.1 When a profile-validity extractor is requested, the BC shall provide an extractor that
///   accepts JSON and XML resources for the supplied FHIR release.
/// - R3.2 When a resource declares exactly one top-level profile, the BC shall select that profile
///   as the resource profile, retaining its canonical and any declared version.
/// - R3.3 When a Bundle or Parameters resource has no top-level profile, the BC shall inspect only
///   its first resource-bearing entry or parameter and select that resource's first profile when
///   present.
/// - R3.4 If no resource profile can be found, then the BC shall report a profile-extraction failure
///   even when validation options supply an explicit profile override.
/// - R3.5 If a resource declares multiple top-level profiles, then the BC shall report a
///   profile-extraction failure.
/// - R3.6 Where a profile filter is supplied, if the resource profile does not match that filter,
///   then the BC shall report a profile-extraction failure before applying any profile override.
/// - R3.7 When an explicit profile override is supplied and resource-profile checks succeed, the
///   BC shall select the override instead of the resource profile.
/// - R3.8 If the selected profile canonical is not supported by the module, then the BC shall report
///   a profile-extraction failure.
/// - R3.9 If the resource cannot be parsed or its profile cannot be resolved, then the BC shall
///   report a profile-extraction failure retaining the underlying cause.
/// - R3.10 If a required resource, FHIR release, validation options, or module index is absent,
///   then the BC shall reject the profile-extraction request.
///
/// ### R4 — Validity date extraction
///
/// - R4.1 When the module defines a validity-date locator for the selected profile and version,
///   the BC shall evaluate that locator against the resource.
/// - R4.2 If the locator is absent, yields no value, fails evaluation, yields multiple values, or
///   yields a non-date or unusable date value, then the BC shall return profile validity without
///   a date.
/// - R4.3 When a locator yields one usable date value with timezone information, the BC shall
///   return its calendar date in Europe/Berlin.
/// - R4.4 When a locator yields one usable date value without timezone information, the BC shall
///   retain that value's calendar date.
/// - R4.5 If the module defines a blank validity-date locator, then the BC shall report a
///   profile-extraction failure.
///
/// ### R5 — Package selection
///
/// - R5.1 When package selection resolves a profile with a validity date, the BC shall return the
///   module packages matching that profile canonical, version, and date.
/// - R5.2 When package selection resolves a profile without a validity date, the BC shall return
///   the module's fallback packages for that profile canonical and version.
/// - R5.3 While validity-period checking is enabled, if date-based selection returns no packages,
///   then the BC shall report that the profile is outside its validity period.
/// - R5.4 While validity-period checking is disabled, if date-based selection returns no packages,
///   then the BC shall return the empty selection without a validity-period failure.
/// - R5.5 If package selection has no initialized module index, then the BC shall reject the
///   selection request.
/// - R5.6 If profile-validity extraction fails during package selection, then the BC shall report
///   that failure rather than return a package selection.
///
/// ### R6 — Validation result policies
///
/// - R6.1 Where suppression rules are supplied, when a message's content matches any suppression
///   expression, the BC shall remove that message before severity transformation and filtering.
/// - R6.2 Where severity transformations are supplied, when a message matches a transformation's
///   specified source severity and all specified criteria, the BC shall apply only the first
///   matching transformation in declaration order, retaining the message content and identifier.
/// - R6.3 Where a transformation specifies criteria, the BC shall match identifiers by equality,
///   locators by content containment, and regular expressions by a match within the content.
/// - R6.4 Where a transformation omits or leaves blank its source severity or a matching criterion,
///   the BC shall treat that condition as unrestricted.
/// - R6.5 Where a matching transformation specifies a fatal, error, or warning target severity,
///   the BC shall apply that severity case-insensitively; otherwise it shall apply informational
///   severity.
/// - R6.6 Where ignored terminology canonicals are supplied, when a retained message contains a
///   nonblank ignored CodeSystem or ValueSet canonical, the BC shall set that message's severity
///   to informational after severity transformation and before severity filtering.
/// - R6.7 When messages are returned, the BC shall preserve identifiers beginning with `REFV-`
///   and replace other identifiers with the reference validator's validation-error identifier
///   without changing their content or severity.
/// - R6.8 While the keep-all policy is selected, the BC shall retain all messages remaining after
///   suppression, severity transformation, and ignored-terminology handling.
/// - R6.9 While the errors-only policy is selected, the BC shall retain only error and fatal
///   messages, except for the no-error result required when none remain.
/// - R6.10 While the errors-and-warnings policy is selected, the BC shall retain only warning,
///   error, and fatal messages, except for the no-error result required when none remain.
/// - R6.11 If suppression or severity filtering leaves no messages, then the BC shall return an
///   informational no-error result rather than fail or return an empty result.
/// - R6.12 When default validation options are requested, the BC shall select keep-all message
///   handling and enabled validity-period checking without a profile override, profile filter,
///   suppression rules, severity transformations, or ignored terminology canonicals.
///
/// ### R7 — Validator lifecycle
///
/// - R7.1 When a validator created from configuration is closed, the BC shall close its owned
///   validation context.
/// - R7.2 When a validator using a caller-owned context is closed, the BC shall leave that context
///   open and under the caller's ownership.
///
/// ## Entities
///
/// - FhirResource, JsonFhirResource, XmlFhirResource
/// - ProfileCanonical
/// - ValidationRequest, ValidationOptions, ValidationResult
/// - ValidationMessagesFilterStrategy, ProfileValidityPeriodCheckStrategy
///
/// ## Out of scope
///
/// - Constructing FHIR engines or defining context-level validation behavior.
/// - Downloading or resolving package archives and loading validation modules.
/// - Defining module profile mappings, validity periods, or package dependency groups.
/// - Generating snapshots.
/// - CLI input/output, batch scheduling, or recursive validation orchestration.
/// - New concurrency guarantees or guarantees for validation after closing a validator.
package de.gematik.refv.lib.validation;
