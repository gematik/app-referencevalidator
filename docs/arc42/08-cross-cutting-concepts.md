# 8. Cross-cutting Concepts

## 8.1 Immutability and domain modeling

Many request, configuration, result, and manifest value types are **Java records** with shallow
immutability and value-based equality. Collection fields are defensively copied where specified (`List.copyOf` /
`Map.copyOf`). Runtime contexts, validators, engines, and caches remain mutable and
are not made thread-safe merely by the use of records.

## 8.2 Concurrency

* Multi-file CLI validation uses a virtual-thread-per-task executor behind a sliding completion
  window. At most the configured `--parallel-threads` count (default 5, maximum 32) is in flight.
* `ValidationInputCollector` bounds the input list, bytes per file, total input bytes, and directory
  depth before validation. Resource bytes are read with a bound inside each task.
* The CLI owns batch scheduling and opens one batch-scoped context provider for both single- and
  multi-input validation. Its access-ordered template cache is capped by the in-flight limit. Active
  leases prevent eviction, and single-flight initialization avoids duplicate template construction
  for the same context configuration and ordered package set.
* A lease creates a distinct context clone for a resource. The template is never validated directly.
  Each clone is wrapped in a new validator and closed before its lease is released. The manager closes
  templates only after the batch's active leases end. The HL7 engine copy path reuses selected
  dependencies, so parity and concurrent-isolation tests are the verification gate for this design.

## 8.3 Error handling

* A **sealed exception hierarchy** rooted at `RefValException` carries a
  `ContextFailure.FailureCategory`. The CLI and designated library boundary packages avoid HL7
  types. Engine exceptions are wrapped by library operations.
* Validation and snapshot operations return a `ContextResult` (`ValidationResult` or
  `SnapshotGenerationResult`) containing `ResultMessage` findings. Expected conformance findings
  are results, while initialization, parsing, and operation failures may be exceptions. The common
  result interface does not impose a universal message-sorting guarantee.
* **Fail-fast** for configuration errors (module config cross-references, invalid options).
* The shared JSON/YAML mappers reject unknown fields unless a target type explicitly permits them.
  YAML failures identify offending fields or incompatible values and include line/column information
  for syntax errors when available. CLI loaders and module importers own source selection and must
  preserve these diagnostics when translating exceptions.

## 8.4 Message transformation & suppression

The Validation capability applies policies supplied in `ValidationOptions` in this order:

1. Remove messages whose content matches a suppression rule's regular expression.
2. Apply the first matching severity transformation in declaration order. All specified conditions
   must match. Message IDs use equality, locator strings use containment, and
   `messageLocationRegex` searches message content despite its name. Blank source severity or
   criteria impose no restriction. Fatal, error, and warning targets are case-insensitive. Other
   targets produce informational severity.
3. Downgrade retained messages containing a nonblank ignored CodeSystem or ValueSet canonical to
   informational severity. This is result handling, not proof that terminology was validated or a
   guarantee that the engine skipped the check.
4. Apply the requested severity filter. Preserve `REFV-` identifiers and normalize other message
   identifiers to the project's validation-error identifier. Return an informational no-error
   result when suppression or filtering leaves no messages.

Default options retain all messages, enable validity-period checking, and supply no override,
filter, suppression, transformations, or ignored terminology. Module rules must be carried into
the effective options by the orchestrating caller. The CLI performs this composition for module
validation, but currently appends every manifest transformation list to the base transformations.
It does not restrict them to the selected package group. Separately, `ValidationModuleIndex`
aggregates distinct transformation names from all groups bound to a profile version in declaration
order, not only from the date-selected group. This difference in scope is recorded as TD3 in
[chapter 11](11-risks-and-technical-debt.md).

These policies can change the final valid/invalid outcome without changing the underlying engine.
Module publishers must document their rationale and test their scope against both valid and invalid
instances. Regex patterns and engine message text are upgrade-sensitive.

## 8.5 Caching

* `DefaultPackageResolver` keeps resolved package results in an in-memory map for its own instance.
  `DefaultPackageProcessor` uses a file-backed `SnapshotArchiveCache` for successful generated
  archives. Validation has a separate, batch-scoped bounded context-template cache. It is not backed
  by Caffeine and is discarded when the batch closes.
* Validation package downloads use `~/.fhir/packages` by default. Snapshot downloads use the
  persistent `~/.fhir/snapshot-packages` cache, including its generated-archive cache. Both are
  configurable and operator-managed.
* Library-created temporary terminology caches use shared ownership. Closing a context releases
  its ownership, and the directory is removed only after the last sharing context closes. Explicit
  terminology-cache paths are caller-owned and are not deleted by the library.

## 8.6 Streaming and resource management

Package repackaging streams file contents into a TAR/GZIP archive, stores the intermediate archive in
the output directory, and publishes it after successful compression. Snapshot work directories are
cleaned after processing. Callers explicitly close resource `ValidationContext`s to release their
temporary terminology-cache ownership. Batch-provider closure releases
retained templates after all leases have been released. Validators created from configuration close
their owned context. Validators wrapping a caller-owned context leave it open. Snapshot generators
release owned resources and support repeated close without additional effects.

## 8.7 Security

* Archive extraction rejects absolute paths, traversal, links, duplicate/conflicting entries, and
  unsupported entry types. It also caps entry count and extracted sizes and removes partial content
  after failure.
* Validation inputs reject symbolic links and non-regular files, and enforce configurable count,
  per-file size, aggregate-size, and directory-depth bounds (each with a hard ceiling).
* Inputs are read from their supplied paths. Package caches, temporary files, and HTML/JSON reports
  may be written. Operators must secure those locations and any enabled network access.
* ArchUnit tests prevent direct HL7 dependencies in the CLI and selected library boundary packages.

## 8.8 Logging & observability

* The library uses **SLF4J**. Log4j2 is the CLI runtime binding and is also used in library tests.
* `--debug` raises root logging to DEBUG. `--additional-debug` also enables DEBUG for
  `org.hl7.fhir`.
* `ResultReporter` prints a console summary and writes a default HTML report or a JSON report when
  `--json` is selected.

## 8.9 Internationalization

Validation messages are localized via `ContextConfiguration.locale` (allowed: `de`, `en-GB`,
`en-US`). The engine locale is set accordingly.

## 8.10 Engine migration and known behavior changes

The [regression notes](../Regressions.md) describe the transition from 2.17.0 to 3.0.0. Their `R1`–`R5`
labels are regression identifiers, not library package requirement IDs.

| Regression                   | Architectural consequence                                                                                                                                                                     | Module and deployment response                                                                                                                                                                                                                                      |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| R1 — FHIRPath double quotes  | The new engine rejects nonconformant expressions using double-quoted string literals. Older profiles may fail processing.                                                                     | Correct the upstream profile or apply a documented replacement-file patch before snapshot generation. Use conformant FHIRPath in module date locators too.                                                                                                          |
| R2 — Unknown CodeSystems     | An unresolved CodeSystem can produce a WARNING rather than an ERROR. An otherwise clean result may therefore remain valid.                                                                    | Test expected outcomes with the chosen terminology policy. Do not interpret absence of ERROR/FATAL as evidence that all codes were verified.                                                                                                                        |
| R3 — Bundle references       | References unavailable or unmatched in the Bundle can be reported as ERROR.                                                                                                                   | Include resolvable reference content where required and test reference behavior. Offline validation is not a mechanism for retrieving external resources.                                                                                                           |
| R4 — Unversioned terminology | The migration notes report CodeSystem/ValueSet resolution against the latest FHIR release, identified there as R5, when no version is declared. This can introduce cross-version code errors. | Prefer explicit terminology versions. If an accepted limitation requires a transformation, scope it to the specific message and content, as in the regression example. Do not describe this as a general guarantee that every unversioned canonical resolves to R5. |
| R5 — Child validation        | Default engine behavior does not validate Bundle/Parameters children against all their own declared profiles.                                                                                 | Enable `context.validationPolicy.recursiveModePolicy` when those checks are required and test both JSON and XML. This is separate from directory recursion and package-selection profile extraction.                                                                |

Offline operation disables remote retrieval, not every terminology check. Checks can use locally
available CodeSystems and ValueSets. Snapshot generation does not make unavailable external
terminology complete. Package-download policy and terminology-server policy must be configured
and tested independently.

