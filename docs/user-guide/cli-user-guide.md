# Reference Validator CLI User Guide

## Scope

The Reference Validator CLI is a tool that performs operations such as the validation of FHIR resources through
Implementation Guides defined in validation modules and the generation of snapshots from given FHIR packages.

## Usage

The latest version of the Reference Validator CLI can be downloaded
from [GitHub Releases](https://github.com/gematik/app-referencevalidator/releases).

The CLI follows a command-based structure:

```bash
java -jar referencevalidator-cli.jar <command> [options]
```

and exposes an help showing the available commands:

```bash
java -jar referencevalidator-cli.jar --help
```

For each command, you can also display help specific to that command:

```bash
java -jar referencevalidator-cli.jar <command> --help
```

The CLI application can be executed by passing different arguments, but it supports also the execution through a
configuration file, in YAML format. Both the `validate` and `generate-snapshot` commands have their own settings, that
can help you fine-tune the operations.

### Global Flags

| Flag           | Description                         |
|----------------|-------------------------------------|
| `--help`, `-h` | Prints usage information and exits. |
| `-V`           | Displays the version information.   |

---

## Commands

### 1. Configuration Management (`configuration`)

This command allows you to manage the YAML configuration files required for validation or snapshot generation
operations. You can either validate an existing configuration or generate a new one.

**Common usage:**

- **Generate a new configuration (e.g., for FHIR R4):**
  ```bash
  java -jar referencevalidator-cli.jar configuration --config path/to/new_config.yaml --generate --fhir-version R4
  ```
- **Validate an existing configuration:**
  ```bash
  java -jar referencevalidator-cli.jar configuration --config path/to/existing_config.yaml --validate
  ```

**Options:**

| Flag                       | Description                                                                                 |
|:---------------------------|:--------------------------------------------------------------------------------------------|
| `--config <path>`          | **(Required)** Path to the YAML configuration file. If used with `--generate`, this is the  |
| destination path.          |                                                                                             |
| `--fhir-version <version>` | Specifies the FHIR version (e.g., `R4`, `R5`). Defaults to `R4`.                            |
| `--generate`               | Generates a new configuration file at the path specified by `--config`.                     |
| `--validate`               | Validates the provided configuration file and prints its content.                           |
| `--for-snapshot`           | Configures the generation/validation for use with the **Snapshot Generator** instead of the |
| standard Validator.        |                                                                                             |

#### Writing a CLI configuration YAML

The CLI uses two different top-level YAML structures. A validation configuration is read by
`validate`; a snapshot configuration is read by `generate-snapshot`. Generate the appropriate
starting point with `configuration --generate` and `--for-snapshot` for snapshot generation, then
edit the generated file for your environment. The generated paths and module names are examples and
must be updated to match your installation. The generated validation template contains an example
`module` block; remove it if you want to use core FHIR definitions only. The generated snapshot
template contains an example module block; remove it when snapshots will be generated from a
`--source` package, archive, or directory instead.

Configuration keys use the camelCase spelling shown below. The YAML loader rejects unknown keys,
so a misspelling is an error. Keep the required sections and fields in place; the configuration
generator is the safest way to get a complete template. Paths are interpreted by Java relative to
the process working directory, not relative to the YAML file. Use a path such as `./packages` or an
absolute path; a `~` prefix in YAML is not expanded as it would be by a shell.

##### Validate FHIR resources

This is a validation configuration with an explicit validation module. The `module` block may be
omitted when validation should use core FHIR definitions without a module. `directory` points to
the folder containing validation module JAR files, and `name` selects the module to load.

```yaml
context:
  fhirRelease: "R4"
  locale: "de"
  displayBehavior:
    displayWarnings: "ENABLED"
    displayMessagesFromReferences: "ENABLED"
    displayMessageIds: "ENABLED"
    displayInvariantsInMessage: "ENABLED"
    displayHintAboutNonMustSupport: "DISABLED"
    displayExtensibleBindingsWarnings: "DISABLED"
    displayBestPracticeMessageLevel: "SHOW_WARNINGS"
  packageLoading:
    remoteDownloadPolicy: "DISALLOWED"
    cachePath: "./.fhir/packages"
  terminology:
    remoteLoadingPolicy: "DISALLOWED"
  validationPolicy:
    validationLevelPolicy: "SHOW_WARNINGS_AND_ERRORS"
    unknownCodeSystemsPolicy: "ALLOWED"
    exampleCodeSystemsUsagePolicy: "DISALLOWED"
    exampleUrlsUsagePolicy: "ALLOWED"
    exampleRestReferencesPolicy: "ALLOWED"
    recursiveModePolicy: "DISALLOWED"
    anyExtensionPolicy: "ALLOWED"
module:
  directory: "./modules"
  name: "my-module"
validationOptions:
  validationMessagesFilterStrategy: "KEEP_ERRORS_ONLY"
  profileValidityPeriodCheckStrategy: "VALIDATE"
report:
  filePath: "./output/report.html"
  format: "HTML"
```

To validate against one profile from the configuration, add `profileToValidate` under
`validationOptions` as an object with a canonical URL and a version. Use an empty version when the
canonical does not include one:

```yaml
validationOptions:
  profileToValidate:
    canonical: "https://example.org/fhir/StructureDefinition/example"
    version: ""
  validationMessagesFilterStrategy: "KEEP_ERRORS_ONLY"
  profileValidityPeriodCheckStrategy: "VALIDATE"
```

Alternatively, select a profile for one run with the `--profile` option. To permit downloading
remote package dependencies, set `context.packageLoading.remoteDownloadPolicy` to `ALLOWED`.
Terminology expansion is configured independently: set `context.terminology.remoteLoadingPolicy` to
`ALLOWED` and provide `serverUri`, for example `"https://tx.fhir.org"`. When the terminology policy
is `DISALLOWED`, no `serverUri` is needed and code validation remains offline.

The accepted FHIR releases are `R4` and `R5`; `locale` accepts `de`, `en-GB`, or `en-US`. Policy
values are the uppercase enum names shown in the example. In particular, `remoteDownloadPolicy` and
`remoteLoadingPolicy` accept `ALLOWED` or `DISALLOWED`; `validationMessagesFilterStrategy` accepts
`KEEP_ALL`, `KEEP_ERRORS_ONLY`, or `KEEP_ERRORS_AND_WARNINGS_ONLY`; and
`profileValidityPeriodCheckStrategy` accepts `VALIDATE` or `IGNORE`.

##### Generate snapshots from a source

For a package, archive, or dependency directory supplied with `--source`, the snapshot configuration
contains `context` and `report` sections, but no `module` section. Package source and output directory
remain command-line arguments; they are not fields in this YAML. This example enables remote package
downloads but leaves terminology expansion disabled:

```yaml
context:
  fhirRelease: "R4"
  locale: "de"
  displayBehavior:
    displayWarnings: "ENABLED"
    displayMessagesFromReferences: "ENABLED"
    displayMessageIds: "ENABLED"
    displayInvariantsInMessage: "ENABLED"
    displayHintAboutNonMustSupport: "DISABLED"
    displayExtensibleBindingsWarnings: "DISABLED"
    displayBestPracticeMessageLevel: "SHOW_WARNINGS"
  packageLoading:
    remoteDownloadPolicy: "ALLOWED"
    cachePath: "./.fhir/snapshot-packages"
  terminology:
    remoteLoadingPolicy: "DISALLOWED"
  validationPolicy:
    validationLevelPolicy: "SHOW_WARNINGS_AND_ERRORS"
    unknownCodeSystemsPolicy: "ALLOWED"
    exampleCodeSystemsUsagePolicy: "DISALLOWED"
    exampleUrlsUsagePolicy: "ALLOWED"
    exampleRestReferencesPolicy: "ALLOWED"
    recursiveModePolicy: "DISALLOWED"
    anyExtensionPolicy: "ALLOWED"
report:
  filePath: "./output/snapshot-report.html"
  format: "HTML"
```

Run this form with the source and output directory, for example:

```bash
java -jar referencevalidator-cli.jar generate-snapshot \
  --config ./snapshot.yaml --source my.package#1.0.0 --output-dir ./snapshots
```

To expand ValueSets through a terminology server, change the terminology block to include
`remoteLoadingPolicy: "ALLOWED"` and the server's `serverUri`. To run without fetching packages,
set `remoteDownloadPolicy: "DISALLOWED"` and ensure the configured cache already contains the
dependencies.

##### Generate snapshots from a validation module

For module-based snapshot generation, add a `module` block to the snapshot configuration:

```yaml
module:
  manifestPath: "./modules/my-module/config.yaml"
  sourcePackagesPath: "./modules/my-module/src-package"
  patchesPath: "./modules/my-module/src-package/patches"
```

`manifestPath` points to the validation module's own manifest YAML; the other paths locate its
source package archives and optional patches. This module manifest is a different file and format
from the CLI configuration described in this section. Run with `generate-snapshot --config
./snapshot.yaml --output-dir ./snapshots`; package groups can be filtered with `--packages`.

The `report` block requires `filePath`; its `format` value is `HTML` or `JSON`. The current CLI
selects the actual report output format with `--json` (otherwise it writes HTML), so do not rely on
the YAML `format` value alone to switch formats. Set `filePath` to a matching extension.
Configuration paths and other command-specific inputs such as validation resource files,
`generate-snapshot --source`, and `--output-dir` remain command-line arguments.
When `--config` is supplied, the YAML provides the validator/snapshot settings rather than merging
with corresponding settings such as `--fhir-version`, `--locale`, or module flags. Supply inputs
that are not part of the YAML (for example resource files, snapshot source/output, and `--json`) on
the command line.

---

### 2. FHIR Resource Validation (`validate`)

This command validates one or more FHIR resource files against specific profiles or modules.

> [!NOTE]
> The `validate` command by default runs in offline mode, which means that no external connection will be attempted
against a remote FHIR package server or a remote Terminology server. Running in offline mode means that no active
validation of codes is possible.

The supported formats for the FHIR Resources to be validated are `JSON` and `XML`.

**Common usage:**

- **Validate a single file:**
  ```bash
  java -jar referencevalidator-cli.jar validate my_resource.json --config path/to/config.yaml --report output.html
  ```
- **Validate multiple files in a directory:**
  ```bash
  java -jar referencevalidator-cli.jar validate ./my_resources_folder/ --config path/to/config.yaml --report output.html
  ```
- **Validate against a specific profile:**
  ```bash
  java -jar referencevalidator-cli.jar validate -p http://example.org/Profile/MyProfile --fhir-version R4 --report output.html --locale de my_resource.json
  ```

**Options:**

| Flag                                    | Alias | Description                                                                                                         |
|:----------------------------------------|:-----:|---------------------------------------------------------------------------------------------------------------------|
| `--config <path>`                       | `-c`  | Path to the YAML configuration file.                                                                                |
| `--modules-folder <path>`               |   -   | Folder containing Validation Modules as JAR files.                                                                  |
| `--module-name <name>`                  |   -   | The specific Validation Module to use.                                                                              |
| `--json`                                | `-j`  | Outputs logs and reports in JSON format.                                                                            |
| `--report <path>`                       | `-r`  | Specifies the file path where the validation report should be written.                                              |
| `--use-terminology-server`              |   -   | Enables the use of a terminology server to expand ValueSets during validation.                                      |
| `--fhir-version <version>`              | `-f`  | Specifies the FHIR version (e.g., `R4`, `R5`). Defaults to `R4`.                                                    |
| `--terminology-server <url>`            | `-t`  | The URL of the Terminology Service to be used.                                                                      |
| `--allow-example-urls`                  |   -   | Allows the validation to ignore non-resolvable example URLs in resources.                                           |
| `--implementation-guide <name#version>` |   -   | The name of the implementation guide to use for validating resources, without detecting it from the resource itself |
| `--profile <url>`                       | `-p`  | The canonical URL of the profile to validate against.                                                               |
| `--verbose`                             | `-v`  | Enables verbose logging (displays `INFORMATION` and `WARNING` messages).                                            |
| `--skip-validity-period-checks`         |   -   | Disables checks for the predefined validity periods of profiles.                                                    |
| `--online-mode`                         |   -   | Enables the downloading of remote dependencies.                                                                     |
| `--locale <locale>`                     |   -   | Sets the language for validation messages (e.g., `de`).                                                             |
| `--max-input-files <count>`             |   -   | Maximum resources in a batch. Defaults to `1000`; allowed range is `1`–`10000`.                                     |
| `--max-file-bytes <bytes>`              |   -   | Maximum size of one resource. Defaults to `52428800` (50 MiB); maximum is `104857600` (100 MiB).                    |
| `--max-total-bytes <bytes>`             |   -   | Maximum combined input size. Defaults to `524288000` (500 MiB); maximum is `1073741824` (1 GiB).                    |
| `--max-directory-depth <depth>`         |   -   | Maximum nested directory depth below each selected directory. Defaults to `32`; maximum is `64`.                    |
| `--parallel-threads <count>`            |   -   | Maximum validations in flight. Defaults to `5`; allowed range is `1`–`32`.                                          |

These limits apply after recursive directory expansion. Symbolic links and non-regular files are rejected. A batch
that exceeds any limit fails before package preloading or validation begins. Resource bytes are read with a bounded read
after discovery so a file that grows after the size check cannot bypass the per-file limit. Validation tasks run in
batches, with at most the configured number in flight; each task continues to construct an isolated `Validator` for
its own resolved package set.

---

### 3. Snapshot Generation (`generate-snapshot`)

This command generates snapshots from FHIR profiles or a collection of dependencies.

> [!NOTE]
> The `generate-snapshot` command **requires** access to internet, since it needs to fetch external dependencies and
reach for a remote Terminology server during the generation of snapshots. Although an offline generation of snapshots is
possible, it requires an already configured cache with all the available packages downloaded, but an internal expansion
of value sets or validation of codes won't be performed.

**Common usages:**

- **Generate snapshot from a remote package:**
  ```bash
  java -jar referencevalidator-cli.jar generate-snapshot -s my.package#1.0.0 -o ./snapshots --report out.html
  ```
- **Generate snapshots from a local directory of dependencies:**
  ```bash
  java -jar referencevalidator-cli.jar generate-snapshot -s ./my_dependencies_folder/ -o ./snapshots --report out.html
  ```
- **Generate snapshots from a local single `.tgz` file of dependencies:**
  ```bash
  java -jar referencevalidator-cli.jar generate-snapshot -s ./my_dependencies_folder/my_dependency.tgz -o ./snapshots --report out.html
  ```
- **Generate snapshots from module:**
  ```bash
  java -jar referencevalidator-cli.jar generate-snapshot --module-manifest ./my_modules_folder/META-INF/config.yaml  -o ./snapshots --report out.html
  ```

**Options:**

| Flag                                          | Alias | Description                                                                             |
|:----------------------------------------------|:------|:----------------------------------------------------------------------------------------|
| `--source <path>`                             | `-s`  | The source for snapshots: a remote package coordinate (`name#version`), a single `.tgz` |
| package path, or a directory of dependencies. |       |
| `--output-dir <path>`                         | `-o`  | The directory where snapshots or JSON reports will be saved.                            |
| `--report <path>`                             | `-r`  | Specifies the file path where the validation report should be written.                  |
| `--json`                                      | `-j`  | Outputs logs and reports in JSON format.                                                |
| `--config <path>`                             | `-c`  | Path to the YAML configuration file.                                                    |
| `--packages <list>`                           | `-p`  | A comma-separated list of specific package names to process. If omitted, all            |
| dependencies in the source are processed.     |       |
| `--modules-config <path>`                     | -     | Path to the YAML configuration file for a validation module                             |
| `--patches-dir <path>`                        | -     | Directory directly containing `<name#version>` folders with dependency patches.         |
| `--cache-dir <path>`                          | -     | Path to a directory containing cached dependencies for the snapshot generation.         |
| `--fhir-version <version>`                    | `-f`  | Specifies the FHIR version (e.g., `R4`, `R5`). Defaults to `R4`.                        |
| `--use-terminology-server`                    | -     | Enables the use of a terminology server for expanding ValueSets.                        |
| `--terminology-server <url>`                  | `-t`  | The URL of the Terminology Service to be used.                                          |
| `--offline-mode`                              | -     | Disables the downloading of remote dependencies.                                        |
| `--locale <locale>`                           | -     | Sets the language for validation messages (e.g., `de`).                                 |
