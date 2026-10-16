# 2. Architecture Constraints

## 2.1 Technical constraints

| #   | Constraint                                                                    | Rationale / Consequence                                                                                                                                                                                                                       |
|-----|-------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| C1  | **Java 25** (`maven.compiler.release=25`)                                     | Modern language features (records, sealed types, virtual threads, pattern matching).                                                                                                                                                          |
| C2  | **HL7 Core Validator is the FHIR engine** (`org.hl7.fhir.validation`, v7.0.1) | FHIR conformance is delegated to the pinned HL7 engine. Final results also depend on packages, configuration, and module rules. HAPI is not an alternative validation engine.                                                                 |
| C3  | **HL7 engine contains mutable state**                                         | Concurrent CLI tasks create a separate validator and isolated context clone per resource. A batch template is only a clone source and is never used to validate a resource.                                                                   |
| C4  | **Keep HL7 types out of designated boundaries**                               | ArchUnit checks the CLI and selected library boundary packages for direct HL7 dependencies. Public interfaces and models live in `referencevalidator-lib`, not a separate `core-lib` artifact.                                                |
| C5  | **Use records for value types where appropriate**                             | Records provide value-based equality and shallow immutability. Collection fields are defensively copied where specified. Runtime contexts and engines remain mutable.                                                                         |
| C6  | **Streaming and defensive package archive handling**                          | Archive contents are streamed with sorted entries and normalized metadata. Extraction is path-checked and size-limited. Output is published after successful compression.                                                                     |
| C7  | **Validation modules are external JARs**                                      | Loaded from a user-supplied directory by manifest name. Their content and releases are maintained outside the validator reactor.                                                                                                              |
| C8  | **Maven multi-module build**                                                  | The default reactor contains `bom`, `validation-module-parent`, `validation-module-api`, `referencevalidator-lib`, `referencevalidator-cli`, and `report-aggregate`. The `jmh` profile adds benchmarks. Maven 3.9.x is required by the build. |
| C9  | **Reproducible outputs**                                                      | Maven has a fixed output timestamp. Archives use sorted entries and normalized metadata. This does not establish that every full CLI result is byte-for-byte reproducible.                                                                    |
| C10 | **Command-specific network policy**                                           | Validation disables package downloads by default (`--online-mode` opts in). Snapshot generation allows downloads by default (`--offline-mode` opts out). Terminology access is separate.                                                      |
| C11 | **Bounded validation inputs and concurrency**                                 | Defaults are 1,000 files, 50 MiB per file, 500 MiB aggregate, depth 32, and 5 in-flight tasks. Hard ceilings are 10,000 files, 100 MiB per file, 1 GiB aggregate, depth 64, and 32 in-flight tasks.                                           |

## 2.2 Organizational / process constraints

| #   | Constraint                                                                                                                                                                                                                                                                        |
|-----|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| C12 | **Open Source (Apache 2.0).** License headers are enforced by `license-maven-plugin`.                                                                                                                                                                                             |
| C13 | **Independent versioning.** The original concept specifies SemVer for the product and MAJOR.MINOR for modules. Manifest `version` is publisher-defined and is separate from `configSpecVersion` (`3.0`). The loader does not enforce the historical module versioning convention. |
| C14 | **Behavior changes require stakeholder review.** The original concept describes coordinated change management. Engine changes and module rule changes both affect referee outcomes.                                                                                               |
| C15 | **Profile rollout planning.** The original concept calls for integration at least three months before mandatory introduction. This is a process requirement, not a runtime constraint.                                                                                            |
| C16 | **Formatting and analysis.** Spotless uses google-java-format. Sonar and aggregated JaCoCo reports support quality review.                                                                                                                                                        |

## 2.3 Legal / regulatory constraints

| #   | Constraint                                                                                                                                                                                          |
|-----|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| C17 | Inputs may contain **highly confidential health data**. The operator must account for generated reports, package/cache directories, and temporary files when setting access and retention controls. |
| C18 | The operator is responsible for securing the runtime environment, output paths, and any network connections.                                                                                        |
| C19 | Embedded deployments should use the dependency versions tested and pinned by the project BOM. This reduces version drift but does not by itself guarantee correctness or confidentiality.           |

## 2.4 Assumptions

| #  | Assumption                                                                                                                                                                                                        |
|----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A1 | The HL7 Core Validator remains the reference implementation for FHIR conformance.                                                                                                                                 |
| A2 | Module publishers build and publish separately using the external template and a compatible `validation-module-parent`. The selected parent, CLI, library, and API versions must be tested together.              |
| A3 | A FHIR package registry (or local cache) is reachable when remote downloads are allowed.                                                                                                                          |
| A4 | A module that uses validity periods provides a usable date-source expression for the resources that need date-based package selection.                                                                            |
| A5 | Runtime memory and disk capacity are provisioned for the workload. Input options are bounded by hard ceilings, but the repository does not define a verified minimum heap or workload-wide performance guarantee. |

The process constraints above derive from the [original concept](../concept/concept.md).
Technical behavior for 3.0.0 follows the current package contracts and implementation, including
the [documented changes from 2.17.0](../Regressions.md). Neither a matching module name nor a
manifest format identifier alone demonstrates engine or behavioral compatibility.

