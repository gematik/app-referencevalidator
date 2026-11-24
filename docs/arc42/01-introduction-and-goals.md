# 1. Introduction and Goals

## 1.1 Purpose

The **gematik Referenzvalidator** checks FHIR resources for *technical processability*
("technische Verarbeitbarkeit") within the German Telematikinfrastruktur (TI). When a validation
module is selected, the validator can choose packages for a profile version from configured
validity periods and the resource date. The internal Core Validator performs the FHIR conformance check.
The final result can also reflect module-declared message transformations and suppression rules.

Validity-period selection is module-driven. A usable resource date selects an inclusive configured
period. Without a date, selection uses the profile version's open-ended period, if available.
The validator's result depends on the engine version, package set, runtime policies, and message
rules. A valid result means that no ERROR or FATAL findings remain after those policies. It is not
a guarantee of clinical correctness or complete terminology coverage.

It is intended to act as an authoritative **reference / "referee" (Schiedsrichter)** for automated
TI processes. The repository does not guarantee identical results to every separately configured
Validator, because package selection and module rules affect the final result.

## 1.2 Core features

| Feature                                  | Description                                                                                                                                  |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| **FHIR resource validation**             | Validates JSON/XML resources against core definitions, caller-supplied packages, or module-selected packages using the HL7 Core Validator.   |
| **Validity-period selection**            | With a module, selects the package group using the declared profile version, validity periods, and configured date source.                   |
| **Snapshot generation**                  | Generates snapshots for StructureDefinitions of a FHIR package, applies patches, and repackages the result as a `.tgz` NPM package.          |
| **Validation modules (plugins)**         | External JARs that declaratively define profiles, versions, package groups, validity periods, message transformations and suppression rules. |
| **Message transformation & suppression** | Applies explicit suppression and severity policies. Their rationale and effect on validation coverage must be documented.                    |
| **Bounded batch validation**             | Enforces configurable input-count, file-size, aggregate-size, directory-depth, and in-flight validation limits.                              |
| **Concurrent validation**                | Uses Java virtual threads with batch-scoped package-set templates and a separate cloned context and validator per resource.                  |
| **Reporting**                            | HTML report by default, or JSON with `--json`; command status is returned as the process exit code.                                          |

## 1.3 Primary use cases

* **Development support** — developers validate generated FHIR resources during implementation.
* **Referee scenario** — resolve disputes about whether a transmitted resource is technically valid.
* **Confirmation testing (Bestätigungsverfahren)** — gematik integrates the validator into its
  test platform to verify primary systems.
* **Real-time validation** — embedded as a library in TI applications (e.g. pharmacy data centers).
* **Specification authoring** — standardization bodies validate example instances and build new
  validation modules from the external template project. Module publishers maintain their own
  profile mappings, patches, test cases, and releases independently of the validator product.

## 1.4 Stakeholders

| Stakeholder                                                  | Interest                                                                                               |
|--------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| gematik                                                      | Authoritative reference, conformance of TI applications                                                |
| Primary-system vendors (PVS/KIS, AVS, pharmacy data centers) | QA of FHIR resource processing                                                                         |
| Standardization organizations (KBV, ABDA, bfarm, …)          | Validate profiles and examples, evolve specs                                                           |
| Health insurers / acceptance points                          | Re-evaluate rejected records (up to ~2 years back)                                                     |
| Confirmation/test platforms (e.g. Titus)                     | Must use the reference validator for testing                                                           |
| Validation module publishers                                 | Release application-specific content and demonstrate compatibility with the selected validator version |

## 1.5 Quality goals (top priorities)

| #  | Quality                             | Motivation                                                                                                                               |
|----|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Q1 | **Correctness / authoritativeness** | Use the HL7 Core Validator for conformance and apply the selected module's declared referee rules consistently.                          |
| Q2 | **Separation of concerns**          | Keep HL7-specific calls out of CLI and designated library boundaries, with dependency rules tested by ArchUnit.                          |
| Q3 | **Thread safety**                   | Concurrent validation uses a separate validator/context per task because the HL7 engine is mutable.                                      |
| Q4 | **Bounded archive processing**      | Stream package contents and enforce extraction limits rather than buffering archive contents in memory.                                  |
| Q5 | **Extensibility**                   | Compatible module JARs add application-specific content without rebuilding the validator. Module and validator releases remain separate. |
| Q6 | **Testability**                     | Unit and integration tests cover behavior, ArchUnit checks selected dependency boundaries.                                               |

