# Developing a Validation Module

This guide explains how to develop, build, and test a validation module for the gematik Reference
Validator. Start from the [validation module template repository](https://github.com/gematik/app-referencevalidator-module-template)
and follow the steps below.

A validation module is a JAR containing a manifest (`META-INF/config.yaml`) and FHIR packages with
generated snapshots (`package/*.tgz`). The manifest defines which packages to load for a profile,
version, and reference date. The project also includes a small Java service-provider class (SPI).
No custom validation code is needed for a module that supplies profiles and configuration only.

The examples use a fictional module named `dummy`. Replace the example canonical URLs, package
coordinates, version numbers, and resource files with values for your implementation guide.

## 1. Prepare the project

You need:

- JDK 25 or later and Maven 3.9 or later for the current Reference Validator version.
- A matching release of `validation-module-parent` and the Reference Validator CLI, available from
  your Maven repository or installed locally. The examples use this checkout's `3.0.0-SNAPSHOT`.
- FHIR packages containing your profiles and dependencies, either as `.tgz` archives or registry
  coordinates (`name#version`).
- JSON or XML FHIR instances to test the module, including the appropriate `meta.profile` values.
- Network access for Maven dependencies and, when enabled, package downloads and terminology lookups.

Create a repository from the [module template](https://github.com/gematik/app-referencevalidator-module-template)
using GitHub's **Use this template** action, then clone your repository. Replace its placeholder
names, descriptions, versions, URLs, and package references with your own values. Check that the
template's parent version matches the Reference Validator release you intend to use.

The build has two separate configurations:

| Purpose | Configuration | Maven phase | Network policy |
| --- | --- | --- | --- |
| Generate package snapshots | `snapshot.config.yaml` | `generate-resources` | Package registry and terminology access can be enabled |
| Validate test instances using the built JAR | `validator.config.yaml` | `verify` | Disable downloads and terminology-server access to test offline operation |

An offline validation run relies on the packages shipped in the JAR; it does not require a registry
or terminology server. This does not mean that every external terminology can be checked offline.

## 2. Understand the project layout

```text
dummy-validation-module/
├── pom.xml
├── snapshot.config.yaml
├── validator.config.yaml
├── README.md
├── ReleaseNotes.md
└── src/
    ├── main/
    │   ├── java/de/gematik/refv/valmodule/DummyValidationModule.java
    │   └── resources/
    │       ├── META-INF/
    │       │   ├── config.yaml
    │       │   └── services/
    │       │       └── de.gematik.refv.valmodule.api.boundary.FhirValidationModule
    │       └── src-package/                  # needed only for bundled source packages
    │           ├── dummy.ig-1.0.0.tgz
    │           └── patches/dummy.ig#1.0.0/   # optional replacement resources
    └── test/resources/fhir/
        └── example-patient.json
```

The parent POM includes `META-INF/**` from `src/main/resources` and generated archives from
`target/generated-resources/package/` in the JAR. Source archives and patches in `src-package/`
are build inputs and are not shipped as separate source resources.

### Register the Java service provider

Keep the template's implementation of `FhirValidationModule`, rename it for your module, and set
`getName()` to your module name:

```java
package de.gematik.refv.valmodule;

import de.gematik.refv.valmodule.api.boundary.FhirValidationModule;

public final class DummyValidationModule implements FhirValidationModule {
    @Override
    public String getName() {
        return "dummy";
    }
}
```

Register the fully qualified class name in
`src/main/resources/META-INF/services/de.gematik.refv.valmodule.api.boundary.FhirValidationModule`:

```text
de.gematik.refv.valmodule.DummyValidationModule
```

Keep `getName()`, the manifest's `name`, and `module.name` in `validator.config.yaml` identical.
The current JAR loader reads the manifest and selects modules by its `name`; it does not discover
or call SPI implementations through `ServiceLoader`. The SPI files remain part of the recommended
project setup, but do not add validation behavior by themselves.

## 3. Define the module manifest

The manifest is a YAML file at `META-INF/config.yaml`. The required top-level values are `configSpecVersion`,
`name`, `version`, `author`, `description`, `packageGroups`, and `profileFamilies`. The `configSpecVersion` entry
identifies the manifest format, not the version of your module (the current manifest format is `3.0`), while a module's
own `version` is chosen by its publisher.

- **`packageGroups`** name complete sets of package archives or package coordinates that are loaded
  together. Each group must contain at least one package. A package entry can be an archive filename
  such as `dummy.ig-1.0.0.tgz` or a package coordinate such as `dummy.ig#1.0.0`.
- **`profileFamilies`** connect profile canonicals to package groups. A family has a
  `canonicalBase`, optional `defaultVersion`, supported `versions`, and optional per-profile
  settings. A profile name is appended to the base URL to form its canonical.
- A version's ordered `groups` list maps validity periods to package groups. `validFrom` and
  `validTill` are inclusive. The first matching period wins. An entry without `validTill` is
  open-ended and is used when no reference date can be determined. If all periods are closed and
  the resource has no usable date, selection fails.
- `profiles.<profile-name>.validityDateSource` is an optional FHIRPath expression used to obtain a
  reference date from a resource. `validityDateSourceByVersion` can override it for particular
  versions. Leave the profile settings empty (`{}`) if no date is needed.
- `errorOnUnknownProfile`, `anyExtensionsAllowed`, and `requireExpansionBeforeValidation` control
  validation policy. `ignoredCodeSystems` and `ignoredValueSets` list terminology canonicals to
  ignore. Use these when necessary, since they affect the validation behavior.
- `globalSuppressionRules` can suppress matching messages using a rule ID, message pattern, and
  reason. `messageTransformations` declares named severity rewrites. A package group may refer to
  those names through `messageTransformations`. Each rewrite sets a target severity and can match
  a source severity, locator string, message-location regular expression, and/or message ID.

The loader checks the manifest structure and cross-references. Every version must have at least one
`groups` entry and at most one open-ended entry. Each entry must reference an existing package
group, and each transformation named by a package group must be declared at the top level.

### `src/main/resources/META-INF/config.yaml`

This small example declares one package group and one profile family. It uses an open-ended group,
so the package group can be selected even when the resource contains no date understood by the
module.

```yaml
configSpecVersion: "3.0"
name: dummy
author: Example team
description: Dummy Validation Module used to demonstrate module layout
version: "1.0.0"
specUrl: "https://example.org/fhir/ImplementationGuide/dummy"

errorOnUnknownProfile: true
anyExtensionsAllowed: false
requireExpansionBeforeValidation: false

ignoredCodeSystems: [ ]
ignoredValueSets: [ ]
globalSuppressionRules: [ ]
messageTransformations: { }

packageGroups:
  dummy.ig.1.0.0:
    packages:
      - "dummy.ig-1.0.0.tgz"

profileFamilies:
  dummy:
    canonicalBase: "https://example.org/fhir/StructureDefinition"
    defaultVersion: "1.0.0"
    versions:
      "1.0.0":
        groups:
          - packageGroupName: dummy.ig.1.0.0
    profiles:
      DummyPatient: { }
```

For this example, the profile canonical is
`https://example.org/fhir/StructureDefinition/DummyPatient|1.0.0`. `defaultVersion` also allows
selection when the resource's canonical omits the `|version` part. The package group points to the
archive filename in `src-package`; use package coordinates instead when the package is resolved from
the configured package cache or a permitted remote source.

### Example with dated versions

When package content changes over time, define separate package groups and validity periods. This
example uses closed periods for an older release and an open-ended group for the current release:

```yaml
packageGroups:
  dummy.ig.1.0.0:
    packages: [ "dummy.ig-1.0.0.tgz" ]
  dummy.ig.1.1.0:
    packages: [ "dummy.ig-1.1.0.tgz" ]

profileFamilies:
  dummy:
    canonicalBase: "https://example.org/fhir/StructureDefinition"
    defaultVersion: "1.0.0"
    versions:
      "1.0.0":
        groups:
          - packageGroupName: dummy.ig.1.0.0
            validFrom: 2024-01-01
            validTill: 2024-12-31
          - packageGroupName: dummy.ig.1.1.0
            validFrom: 2025-01-01
      "1.1.0":
        groups:
          - packageGroupName: dummy.ig.1.1.0
    profiles:
      DummyPatient:
        validityDateSource: "birthDate"
```

For version `1.0.0`, a resource date from 2024 selects the first group and a date from 2025 onward
selects the second. If the date cannot be extracted, the open-ended group is used. The example's
profile validity source is a FHIRPath expression; choose an expression that yields the date relevant
to your implementation guide. Period bounds are inclusive, and declaration order is significant if
periods overlap.

### Optional message handling example

The following fragment shows how a group can refer to a reusable severity transformation and how a
global suppression rule is recorded. Message suppression and severity changes can hide or alter
validation findings, so document the rationale and keep the scope as narrow as possible.

```yaml
globalSuppressionRules:
  - ruleId: "DUMMY-KNOWN-WARNING-001"
    messagePattern: ".*known legacy warning.*"
    reason: "Accepted during the documented migration period"

messageTransformations:
  known_warning_to_information:
    - severityLevelFrom: "warning"
      severityLevelTo: "information"
      messageId: "example-message-id"

packageGroups:
  dummy.ig.1.0.0:
    packages: [ "dummy.ig-1.0.0.tgz" ]
    messageTransformations: [ "known_warning_to_information" ]
```

## 4. Choose how to supply FHIR packages

Both approaches produce snapshotted `.tgz` files under `package/` in the final JAR.

| Approach | Manifest entry | What you provide |
| --- | --- | --- |
| Registry coordinates | `"dummy.ig#1.0.0"` | The package name and version; snapshot generation resolves transitive dependencies |
| Bundled archives | `"dummy.ig-1.0.0.tgz"` | Local archives for all required packages, explicitly listed in each group |

Start with registry coordinates if the implementation guide has reliable dependency metadata:

```yaml
packageGroups:
  dummy.ig.1.0.0:
    packages: [ "dummy.ig#1.0.0" ]
```

Omit `module.sourcePackagesPath` from `snapshot.config.yaml` for this approach and allow package
downloads. The first build needs registry access unless all dependencies are already cached.

Use bundled archives when you need explicit control over dependency versions or offline package
resolution. Place the archives in `src/main/resources/src-package/`, list their filenames in
`packageGroups`, and set `module.sourcePackagesPath` accordingly. Each archive must be a valid FHIR
NPM package, with a `name-version.tgz` filename matching its `package/package.json` identity. Include
transitive dependencies in each group rather than relying on undeclared downloads.

Registry coordinates keep the repository smaller but depend on registry availability and upstream
dependency declarations. Bundled archives keep the source packages under your control but require
manual updates. Either approach supports patches; see step 7.

## 5. Configure the Maven build

The parent POM centralizes Java/build settings and configures two CLI executions:

1. `build-snapshot` runs in the Maven `generate-resources` phase, reads the module/snapshot configuration, and
   writes generated package archives under `target/generated-resources/package/`.
2. `validate-module` runs in the Maven `verify` phase and validates the FHIR files under
   `src/test/resources/fhir/` using the module JAR built by the project.

The executions are configured in the parent's `pluginManagement`. Declare the exec plugin in the
child project's `<build><plugins>` so those managed executions are enabled. A minimal project POM
is:

```xml

<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>de.gematik.refv</groupId>
        <artifactId>validation-module-parent</artifactId>
        <version>3.0.0-SNAPSHOT</version>
        <relativePath/>
    </parent>

    <artifactId>dummy-validation-module</artifactId>
    <packaging>jar</packaging>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>de.gematik.refv</groupId>
                <artifactId>bom</artifactId>
                <version>${project.parent.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>de.gematik.refv</groupId>
            <artifactId>validation-module-api</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>com.diffplug.spotless</groupId>
                <artifactId>spotless-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

The version shown matches this repository's current `3.0.0-SNAPSHOT` version. Replace it with the
concrete version of the Reference Validator release you use. Maven requires a literal version in the
parent declaration, so a POM property cannot be used there. For a locally built Reference Validator
checkout, install the parent and its parent/BOM artifacts first, or build the module in a reactor
that contains them. For a published release, use matching published parent, CLI, and library
versions.

The parent sets `blockSystemExit=true`: a non-zero CLI exit code fails the Maven execution. Keep
the managed exec plugin enabled so both snapshot generation and validation run as part of the build.

## 6. Configure snapshot generation and offline validation

The parent executions refer to `snapshot.config.yaml` and `validator.config.yaml` in the project
directory. The snapshot CLI configuration contains the context, module paths, and report settings.
For example:

```yaml
context:
  fhirRelease: "R4"
  locale: "en-GB"
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
    cachePath: "target/cache-snapshot-fhir-packages/"
  terminology:
    remoteLoadingPolicy: "ALLOWED"
    serverUri: "https://tx.fhir.org"
    cachePath: "target/cache-refv-tx/"
  validationPolicy:
    validationLevelPolicy: "SHOW_WARNINGS_AND_ERRORS"
    unknownCodeSystemsPolicy: "ALLOWED"
    exampleCodeSystemsUsagePolicy: "DISALLOWED"
    exampleUrlsUsagePolicy: "ALLOWED"
    exampleRestReferencesPolicy: "ALLOWED"
    recursiveModePolicy: "DISALLOWED"
    anyExtensionPolicy: "ALLOWED"
module:
  manifestPath: "./src/main/resources/META-INF/config.yaml"
  sourcePackagesPath: "./src/main/resources/src-package"
  patchesPath: "./src/main/resources/src-package/patches"
report:
  filePath: "./target/snapshot-report.html"
  format: "HTML"
```

Paths are interpreted relative to the Maven process working directory (normally the module project
directory), not relative to the YAML file. For registry coordinates, omit `sourcePackagesPath`;
omit `patchesPath` if no patches are needed. Set `remoteDownloadPolicy` to `DISALLOWED` if all package
dependencies are already available locally. Terminology-server access is configured separately
under `context.terminology`. Choose cache paths outside `target/` if they should survive `mvn clean`.

The validation configuration points the CLI at the built module JAR. The directory is the folder
containing the JAR, and `name` must match the manifest's `name`:

```yaml
context:
  fhirRelease: "R4"
  locale: "en-GB"
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
    cachePath: "target/cache-validator-fhir-packages/"
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
  directory: "./target"
  name: "dummy"
validationOptions:
  validationMessagesFilterStrategy: "KEEP_ALL"
  profileValidityPeriodCheckStrategy: "VALIDATE"
report:
  filePath: "./target/validation-report.html"
  format: "HTML"
```

Place real JSON or XML FHIR resources under `src/test/resources/fhir/`: the parent passes that
directory to the CLI validation command. Files are found recursively, so subdirectories such as
`fhir/DummyPatient/1.0.0/` are supported. See the [CLI User Guide](cli-user-guide.md) for the complete
CLI configuration and policy options.

### Plan for offline terminology validation

The validation configuration above disables terminology-server access. Terminology checks can only
use content available offline; generating snapshots does not make every external terminology
available locally.

- Where necessary, list unsupported external code systems and value sets in the manifest's
  `ignoredCodeSystems` and `ignoredValueSets`.
- If a known terminology-service message must be downgraded, use a narrowly scoped message
  transformation and explain why it is necessary.
- Document excluded terminology checks and their consequences in the module's README.
- Review snapshot-generation reports even when terminology access is enabled during the build.

## 7. Add patches if needed

Patch files replace files inside an unpacked FHIR package before snapshot generation. The patch file
must have the same basename as the package file it replaces, and it is copied to that package's
`package/` directory. The configured patches directory directly contains a directory named with
the package coordinates (`name#version`). For example, for `dummy.ig#1.0.0`:

```text
src/main/resources/src-package/
├── dummy.ig-1.0.0.tgz
└── patches/
  └── dummy.ig#1.0.0/
    ├── StructureDefinition-DummyPatient.json
    └── changes.txt
```

Set `module.patchesPath` to `./src/main/resources/src-package/patches`, as in the snapshot
configuration above. Use the exact package name and version. A patch replaces the package file with
the same filename; it does not perform a line-based diff or merge. Record each change and its reason
in `changes.txt`. Typical fixes include incorrect FHIRPath constraints or inconsistent slicing.
Patches can be used with either bundled archives or registry coordinates.

## 8. Build and test the module

Run from the module directory:

```bash
mvn clean verify
```

The build performs these steps:

1. Reads all packages declared in the manifest and resolves their sources and dependencies.
2. Applies any configured patches and generates missing `StructureDefinition` snapshots.
3. Writes generated archives to `target/generated-resources/package/` and a snapshot report to
   `target/snapshot-report.html`.
4. Packages the manifest, Java class, service registration, and generated archives into
   `target/<artifactId>-<version>.jar`.
5. Loads the built JAR and validates the test instances with downloads and terminology access
   disabled, writing `target/validation-report.html`.

Review both reports. Resolve snapshot errors before investigating downstream validation failures.
The validation command returns a non-zero exit code if the overall result is invalid, which fails
the parent's Maven execution. Keep expected-invalid instances out of this positive-test directory.

`mvn package` stops before the validation step. `mvn clean install` also runs verification and then
installs the module in the local Maven repository.

Inspect the generated JAR, replacing the filename with your artifact's actual name:

```bash
jar tf target/dummy-validation-module-3.0.0-SNAPSHOT.jar
```

Confirm that it contains `META-INF/config.yaml`, the SPI class and service registration, and the
expected `package/*.tgz` archives—not the source archives under `src-package/`.

### When to add JUnit tests

The parent's CLI execution is suitable for positive examples and verifies the shipped JAR. Add
JUnit tests using `referencevalidator-lib` when you need expected failures, assertions on individual
messages, or regression coverage for particular rules. See the
[Library User Guide](library-user-guide.md) for programmatic initialization and validation.

## 9. Use and distribute the module

After `mvn verify`, the JAR in `target/` can be loaded by the CLI. The module name is the manifest's
`name`, not necessarily the Maven artifact ID:

```bash
java -jar referencevalidator-cli.jar validate \
  --modules-folder ./target \
  --module-name dummy \
  --config ./validator.config.yaml \
  --report ./target/validation-report.html \
  ./src/test/resources/fhir/
```

The CLI scans JAR files directly inside the module directory and selects the manifest whose `name`
matches `--module-name`. For applications embedding the library, the same module JAR is loaded from a
module directory and the caller selects the package group appropriate to the resource before
creating the validator. See the [Library User Guide](library-user-guide.md) for the library API
example.

Distribute the module JAR together with its version, supported profiles, and any documented
validation limitations. Users do not need the source archives or build-time configuration files.

## Release checklist and troubleshooting

- All template placeholders have been replaced, including module names, versions, and canonical URLs.
- The parent and Reference Validator versions match, and both managed exec executions are enabled.
- The manifest is at the exact JAR path `META-INF/config.yaml`, and `configSpecVersion` is `"3.0"`.
- The JAR contains the SPI class and `META-INF/services/de.gematik.refv.valmodule.api.boundary.FhirValidationModule`
  definition.
- The module's `name` matches the CLI `module.name` or `--module-name` value.
- Every group name referenced under `profileFamilies` exists under `packageGroups`.
- Every supported version has at least one group entry and at most one without `validTill`.
- Date-dependent profiles define a suitable `validityDateSource` or version-specific expression.
- Every package group contains at least one package, and every referenced transformation is declared.
- Each source archive filename uses the `name-version.tgz` schema and its package identity and version match the
  manifest entry.
- The generated module JAR contains `META-INF/config.yaml` and the expected generated `.tgz` files.
- `snapshot.config.yaml` includes its own `module` section when the parent passes `--config`.
- Both HTML reports have been reviewed, and the positive test instances pass offline validation.
- Patches have a `changes.txt` record; terminology exclusions are documented in the module README.
- The module README and `ReleaseNotes.md` describe the supported release and known limitations.

## Limitations

- Do not rely on an offline run to retrieve and validate externally referenced resources.
- Terminology validation is limited to the code systems and value sets available offline. Ignored
  terminology and suppressed messages reduce the scope of validation and must be documented.
