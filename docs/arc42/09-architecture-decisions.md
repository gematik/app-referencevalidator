# 9. Architecture Decisions

## 9.1 Decision register and status

This chapter is the maintained decision register. It records current choices, their rationale,
alternatives, consequences, and status directly in arc42 rather than linking to missing ADR files.
The checkout reviewed on 2026-10-16 has no `docs/architecture/adr` directory. Historical ADR contents,
approval dates, and decision makers have not been recovered.

All decisions D01–D10 below have status **Implemented (observed)** as of 2026-10-16. This means the
choice is visible in the implementation and supporting documentation. It does not mean that a
formal approval was recorded or that tests were executed during this review. The rationale below
explains the current design in terms of its documented goals, not an attributed historical decision.
The evidence and consequences are summarized here, followed by the rationale for each decision.

| Area                  | Current decision                                                                                                       | Evidence/status                                                                                                                          | Consequence                                                                                                                                                                     |
|-----------------------|------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FHIR engine           | Use the HL7 Core Validator for validation and snapshot operations.                                                     | Implemented in `referencevalidator-lib`. The BOM pins the current engine at 7.0.1.                                                       | The 3.0.0 migration replaces the HAPI validation engine. The regression notes identify behavior changes that require validation of existing modules.                            |
| Package structure     | Organize capabilities into `boundary`, `control`, and `entity` packages.                                               | Implemented within `referencevalidator-lib` with selected ArchUnit dependency checks.                                                    | BCE is package-level organization, not three independently buildable modules.                                                                                                   |
| Batch execution       | Bound inputs and in-flight work. Default to 5 concurrent tasks with a hard cap of 32.                                  | `ValidationInputCollector` and `BoundedValidationExecutor` enforce configurable limits in the CLI.                                       | Limits bound accepted work and active validations, not supported workload or required heap size.                                                                                |
| Context reuse         | Retain package-set templates in a batch-scoped capped cache. Clone a context and create a validator for each resource. | The batch provider and template manager use leases to protect active entries.                                                            | Reuses initialization without a process-global context cache. HL7 clones still require concurrent parity and isolation tests.                                                   |
| Archive processing    | Stream TAR/GZIP output, sort entries, normalize metadata, and publish after compression succeeds.                      | Implemented in `PackageArchiveUtils`.                                                                                                    | Equal inputs can produce reproducible archives. This does not make all validation outputs deterministic.                                                                        |
| Module extension      | Load external JAR metadata by manifest name using format 3.0.                                                          | `JarValidationModuleLoader` reads `META-INF/config.yaml`. It does not invoke providers through `ServiceLoader`.                          | Module package import and policy application are separate operations. Duplicate names select the first match, not the newest release.                                           |
| Value types           | Use records for many request, configuration, and result values.                                                        | Implemented with defensive collection copies where defined.                                                                              | Record immutability is shallow, not a guarantee for all contained state.                                                                                                        |
| Capability boundaries | Use the six library package contracts to allocate responsibility.                                                      | `config_parser`, `fhir_context`, `package_resolver`, `snapshot`, `validation`, and `valmodule` each have a `package-info.java` contract. | Configuration source loading belongs to consumers, archive import to Package Resolver, and workflows to Validation and Snapshot. Contract discrepancies remain explicit debt.   |
| Module lifecycle      | Publish module content separately using an external template and the managed module parent.                            | `validation-module-parent` configures CLI executions at `generate-resources` and `verify`.                                               | Module publishers can release content independently, but must test it with a compatible validator and retain the generated artifacts. The template is not a runtime dependency. |
| Cache ownership       | Separate persistent package caches from library-owned temporary terminology caches.                                    | Snapshot download configuration and shared temporary-directory ownership in FHIR Context.                                                | Operators manage persistent and explicit caches. Closing the last sharing context removes a library-owned temporary terminology cache.                                          |

## 9.2 Decision rationale and alternatives

### D01 — FHIR engine

Use the HL7 Core Validator for validation and snapshots to reuse its FHIR conformance behavior
instead of maintaining a separate rule engine. The alternative of retaining the previous HAPI
validation engine would preserve a different integration and behavior baseline. This is not a claim
that both engines produce equivalent results. Engine upgrades require comparison of representative
cases against the [migration regressions](../Regressions.md). See the FHIR engine evidence above.

### D02 — Package structure

Use BCE packages to separate public operations, orchestration, and values within library
capabilities. A flat package structure would weaken responsibility boundaries. Separate Maven
artifacts for every layer would add build and dependency management without being required by the
current deployment model. The implemented package boundaries are checked selectively, not universally.

### D03 — Batch execution

Bound input collection and in-flight validation to prevent workload size from directly determining
active resource use. Unbounded task submission would provide weaker resource control. Serial-only
execution would remove concurrency rather than allow a configurable limit. The current defaults
and ceilings are safeguards, not measured capacity or heap guarantees. See
[deployment constraints](07-deployment-view.md) and [quality requirements](10-quality-requirements.md).

### D04 — Context reuse

Reuse batch-local package-set templates while giving each resource a distinct context clone and
validator. Rebuilding every context would forgo initialization reuse. Sharing a mutable validation
context would weaken isolation. A process-global template cache would introduce a longer lifetime
and additional retention concerns. Clone isolation remains a release gate, not a consequence that
can be assumed from distinct wrappers. See risk R1 in [chapter 11](11-risks-and-technical-debt.md).

### D05 — Archive processing

Stream archive contents to avoid whole-archive buffering, while sorting entry paths and normalizing
metadata to support reproducible output. Buffering complete archives would increase memory demand.
Publishing directly to the final destination before compression succeeds could expose incomplete
output. Reproducible archives do not establish deterministic engine messages or reports.

### D06 — Module extension

Read declarative metadata from external module JARs to keep content loading separate from executable
validation behavior. Invoking arbitrary module providers through `ServiceLoader` would introduce a
different extension and lifecycle contract. The current loader requires candidate-local metadata
and service registration but does not instantiate the provider. First-name-match selection is an
implementation limitation, not an automatic compatibility policy. See the
[module-loading contract](../../referencevalidator-lib/src/main/java/de/gematik/refv/lib/valmodule/package-info.java).

### D07 — Value types

Use records for request, configuration, and result values to express data carriers with explicit
components. Mutable bean-style carriers would permit additional state changes and require more
accessor code. Defensive copies protect collections where implemented. A record alone does not
make referenced objects deeply immutable.

### D08 — Capability boundaries

Use the six library package contracts as the responsibility and behavior baseline. Centralizing all
configuration, package import, and validation in a single workflow would blur ownership and make
independent library use harder. The contracts, rather than incidental implementation calls, define
the intended boundaries. Contract changes require explicit review and corresponding test updates.
The [building-block view](05-building-block-view.md) links the six contracts.

### D09 — Module lifecycle

Publish modules separately from the validator so that profile and package content can evolve on
its publisher's schedule. Bundling every module into the validator release would couple content
delivery to engine releases. The external template and managed parent support consistent builds
without becoming runtime services. Independent releases still require verification of the shipped
module JAR with the intended validator. See the
[module lifecycle](07-deployment-view.md) and [module guide](../user-guide/module-user-guide.md).

### D10 — Cache ownership

Keep downloaded package caches persistent, while reference-counting library-owned temporary
terminology directories. Treating all caches as temporary would discard reusable packages.
Treating all directories as library-owned could delete caller-managed data. Explicit cache paths
remain caller-owned. Operators must define retention and access controls for persistent locations.
See [resource ownership](08-cross-cutting-concepts.md).

## 9.3 Key trade-offs

* **Engine fidelity vs. engine independence** — the HL7 engine supplies FHIR behavior, while
  boundaries expose project types. The library remains coupled to the engine in its implementation.
* **Per-resource isolation vs. initialization reuse** — each resource gets a distinct mutable
  context clone and validator. A batch-local template can be the source of those clones but is never
  used for validation. There is no process-global context cache.
* **Streaming vs. whole-archive buffering** — output is streamed, while a list of archive entry
  paths is collected and sorted before writing.
* **External modules vs. built-in profiles** — module JARs let deployments provide application
  profiles and package mappings independently of the CLI release. They do not isolate those rules
  from engine behavior changes or negotiate engine compatibility automatically.
* **Build-time preparation vs. runtime work** — module builds generate snapshots and verify the
  shipped JAR. Runtime validation uses those archives, but still needs the applicable package set
  and cannot infer that all external terminology is available offline.

`validation-module-api` contains a `module-info.java`. This alone does not establish project-wide
JPMS enforcement and is not recorded here as a decision to enforce JPMS throughout the project.

## 9.4 Maintenance and decision history

Maintain rationale and status in this chapter alongside changes to the affected code, contracts,
and guides. Separate ADR files are not required by this documentation approach.

* Keep D01–D10 stable. Add a new identifier for a new decision rather than renumbering existing ones.
* Use **Proposed** for an option not yet adopted, **Implemented (observed)** for a source-backed
  current choice, **Accepted** only with a recorded approval reference, and **Superseded** when a
  replacement decision is identified. Acceptance and implementation are separate facts.
* For each change, record its date, status, rationale, alternatives, consequences, and source
  evidence. Record an approver or approval date only when supported by an actual review record.
* Preserve superseded rationale and link its replacement. Use repository history for the detailed
  diff rather than reconstructing undocumented historical approvals.
* Keep test-verification status distinct from decision status. Link relevant quality scenarios or
  risks and report test results only when execution evidence is available.

| Recorded on | Change | Status |
|-------------|--------|--------|
| 2026-10-16 | Established this chapter as the maintained register and assigned D01–D10 to the documented current choices. Added rationale and alternatives. | Implemented (observed). Historical approvals remain unknown. No new test verdict is asserted. |

