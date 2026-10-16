# 4. Solution Strategy

## 4.1 Fundamental decisions

| Goal                            | Strategy                                                                                                                                                                              |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Authoritative conformance (Q1)  | **Wrap the HL7 Core Validator** (`FhirValidator`, snapshot utilities, `ValueSetExpander`) instead of re-implementing rules.                                                           |
| Separation of concerns (Q2)     | BCE-style packages and interfaces in `referencevalidator-lib`; the CLI and selected library boundaries avoid direct HL7 dependencies. There is no separate `core-lib` artifact.       |
| Thread safety (Q3)              | A batch-local, bounded template cache initializes each retained package set once; every resource gets a distinct context clone and validator. Templates are never validated directly. |
| Bounded archive processing (Q4) | Stream archive entry data, sort entries, normalize metadata, and publish output only after compression succeeds.                                                                      |
| Extensibility (Q5)              | External module JARs carry declarative manifests and generated packages. A separate template and managed parent support module authoring without changing the validator.              |
| Testability (Q6)                | **JUnit 5** unit tests (given-when-then, `@DisplayName`) + **ArchUnit** rules enforcing the layering and the "no HL7 in API" constraint.                                              |

## 4.2 How the pieces collaborate

* **CLI boundary (`referencevalidator-cli`)** parses arguments and YAML, loads modules, collects
  inputs within configured bounds, selects packages, schedules only the configured number of
  in-flight validations, and writes reports after results are collected.
* **Library (`referencevalidator-lib`)** contains the public interfaces and value types as well as
  the workflows and HL7-backed implementations. Packages such as `validation`, `snapshot`,
  `fhir_context`, `package_resolver`, and `valmodule` use `boundary`, `control`, and `entity`
  subpackages. The `ContextProvider` boundary opens a batch-scoped context provider; its control
  keeps a capped, access-ordered template cache and returns a separately owned context clone from
  each lease. `ValidatorFactory.withValidationContext(...)` supports caller-owned isolated contexts.
  `DefaultContextProvider`, `DefaultValidationContext`, `DefaultValidator`, `DefaultPackageResolver`,
  and `DefaultPackageProcessor` are current implementation names.
* **Module contract (`validation-module-api`)** defines `FhirValidationModule` and the
  `ValidationModuleManifest` model, including profile families, package groups, transformations,
  and suppression rules. The manifest spec in the code is 3.0. The current JAR loader uses the
  manifest, not SPI discovery or custom module validation code.
* **Module build support (`validation-module-parent`)** manages snapshot generation and validation
  of the built JAR. Module publishers activate the managed exec plugin in projects created from
  the external template. This build-time integration is separate from runtime module loading.

Library callers perform the same composition explicitly: load module metadata, import its archives
through Package Resolver, select the resource's packages, then create a validator with core and
selected packages. Loading metadata alone does not configure a validator or apply module policies.

## 4.3 Package-selection algorithm (validity periods)

The heart of the validation is choosing the right FHIR packages for a resource:

1. Parse the resource and read its single top-level profile. Multiple top-level `meta.profile`
   entries fail extraction. If a Bundle or Parameters resource has no top-level profile, inspect
   only the first resource-bearing entry or parameter and use that resource's first profile.
   A missing profile fails extraction even when an explicit override was supplied.
2. Apply any profile-filter regex to the extracted resource profile before applying an explicit
   override. Check the selected canonical against the module index. A versionless canonical uses the family's declared
   `defaultVersion`; undeclared profile URLs may resolve through the family's canonical-base prefix.
3. Evaluate `validityDateSource`, using a version-specific locator when defined. Timezone-bearing
   values are converted to a calendar date in Europe/Berlin. Values without a timezone retain their
   calendar date. A usable date selects the first inclusive matching period in declaration order.
4. Without a date, use the single open-ended period. If all periods are closed, the current selector
   returns no module packages and validation can fail later because the intended profile is absent.
5. A date outside all periods raises `ProfileOutsideValidityPeriodException` when period checking is
   enabled. If checking is disabled, no module package is selected for that date.

`ValidationModuleIndex` builds a lookup from loaded module metadata. `DefaultProfileValidityExtractor`
reads the profile and date, while `ValidationPackageSelector` applies the period-check policy.
These rules apply to module package selection, not to every direct validator call. In particular,
core-only validation does not require a module-supported `meta.profile`.

Under validation contract R4.2, evaluation errors, multiple values, non-date values, and unusable
dates produce profile validity without a date. Year-only and year-month values are not precise
enough for period selection and follow this fallback too. Package selection then uses the
open-ended period, if available. A blank locator remains a profile-extraction failure under R4.5.
The correction and its verification status are tracked in
[chapter 11](11-risks-and-technical-debt.md).

## 4.4 Fail-fast configuration

Module manifests are checked during construction: package-group and message-transformation
references must exist, validity dates cannot be reversed, and each version may have at most one
open-ended period. Profile-version defaults and map relationships are represented in the manifest
model. Keep behavioral edge cases, especially missing date-source results, covered by tests.

The Configuration Parser supplies shared JSON and YAML mappers, not a configuration-loading service.
Callers choose sources, define configuration types and precedence, and preserve field-level parsing
diagnostics when translating failures. Shared mappers must not be reconfigured by consumers.

## 4.5 Engine migration and behavioral compatibility

Version 3.0.0 replaces the HAPI-based validation engine with the HL7 Core Validation library.
HAPI-related model utilities may still occur in implementation dependencies. They do not constitute
a second validation engine. The [regression notes](../Regressions.md) identify changes in FHIRPath
syntax acceptance, unknown terminology severity, Bundle references, unversioned terminology
resolution, and validation of embedded resources.

Compatibility is established for a tested combination of validator, module, FHIR packages, and
policies. It cannot be inferred from successful module loading. Module publishers should correct
profile defects through upstream updates or documented build-time patches, pin terminology
versions where needed, and use narrowly scoped message policies only with an explicit rationale.
Recursive validation of embedded resources is configured in the FHIR Context, independently of
CLI directory traversal and module package selection.

