# Validation Module User Guide

This guide explains how to create a Validation Module for the Reference Validator.

A Validation Module is primarily a manifest plus FHIR package references or local package archives.
The manifest declares the module name, the package sets to load, and (optionally) the profiles and
validity periods that select those package sets. A Java class implementing the module SPI is
required for the generation of the JAR archive, but it is not needed for ordinary manifest-based validation.

The examples use a fictional module named `dummy`. Replace the example canonical URLs, package
coordinates, version numbers, and resource files with values for your implementation guide.

## Prerequisites

- Use the Reference Validator version that matches the `validation-module-parent` version. The
  parent and validator CLI artifacts must be available from your Maven repository (or installed in
  the local Maven repository when building locally).
- Use the Java version required by that Reference Validator release (Java 25 for the current
  implementation).
- Have one or more FHIR NPM package archives (`.tgz`) containing the profiles and dependencies you
  want the validator to use, or define remote package coordinates in the form `name#version`.

## What the module manifest does

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
  those names through `messageTransformations`. Each rewrite may match the source and target
  severity, a locator string, a message-location regular expression, and/or a message ID.

The Module Loader in the Reference Validator checks the YAML manifest structure and cross-references: a profile validity
period
must name an existing package group, and a package group may only name declared message transformations.

## A minimal dummy module

Start with this project layout. The package archive must be a valid FHIR NPM package; the filename
must follow the `name-version.tgz` convention and agree with its `package/package.json` identity.

```text
dummy-validation-module/
├── pom.xml
├── snapshot.config.yaml
├── validator.config.yaml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── de/gematik/refv/valmodule/
│   │   │       └── DummyValidationModule.java  # SPI implementation
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   ├── config.yaml
│   │       │   └── services/                   # SPI registration
│   │       │       └── de.gematik.refv.valmodule.api.boundary.FhirValidationModule
│   │       └── src-package/
│   │           └── dummy.ig-1.0.0.tgz
│   └── test/
│       └── resources/
│           └── fhir/
│               └── example-patient.json
└── target/                         # created by Maven
    └── generated-resources/package/ # generated package snapshots
```

The parent POM copies `META-INF/**` from `src/main/resources` into the module JAR. It also copies
`*.tgz` files under `target/generated-resources/package/` into the JAR. Keep source archives in
`src/main/resources/src-package/`; they are build inputs, while the generated snapshot archives are
the module's packaged validation inputs.

### Java class: `DummyValidationModule`

The API defines `FhirValidationModule` as a small SPI with one method, `getName()`. A module may
implement it like this:

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

If you include this implementation, add the `validation-module-api` artifact as a dependency in your
module POM. Register the implementation using Java's service-provider file at
`src/main/resources/META-INF/services/de.gematik.refv.valmodule.api.boundary.FhirValidationModule`;
the file contains the implementation's fully qualified class name on its own line:

```text
de.gematik.refv.valmodule.DummyValidationModule
```

The class name and service file are optional for the current manifest-based module loading path. In
the current Reference Validator implementation, the Module Loader reads the `META-INF/config.yaml` file and
selects modules using the manifest's `name`. It does not discover or call `FhirValidationModule`
implementations. Consequently `getName()` does not replace the manifest name, and adding the class
does not add validation behavior by itself. Include it still explicitly, for future use or if a consumer
requires it.

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

## Build with `validation-module-parent`

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

Run `mvn clean verify` from the module directory. `mvn package` runs snapshot generation and packages
the module, but does not reach the parent's `verify`-phase validation execution. After a successful
build, inspect the JAR to confirm that it contains `META-INF/config.yaml` and the generated package
archives, for example with `jar tf target/dummy-validation-module-*.jar`.

## Snapshot and validator CLI configurations

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
  manifestPath: "./src/main/resources/META-INF/config.yaml"
  sourcePackagesPath: "./src/main/resources/src-package"
  patchesPath: "./src/main/resources/src-package/patches"
report:
  filePath: "./target/snapshot-report.html"
  format: "HTML"
```

Paths are interpreted relative to the Maven process working directory (normally the module project
directory), not relative to the YAML file. Set `remoteDownloadPolicy` to `DISALLOWED` if all package
dependencies are already available locally. Terminology-server access is configured separately
under `context.terminology` and usually, it should be enabled for at least the Snapshot Generation.

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
directory to the CLI validation command. Adjust package download and terminology settings for your
environment. See the [CLI User Guide](cli-user-guide.md) for the complete CLI configuration and
policy options.

## Patches

Patch files replace files inside an unpacked FHIR package before snapshot generation. The patch file
must have the same basename as the package file it replaces, and it is copied to that package's
`package/` directory. The patch directory is a root containing a `patches/` directory, then a
directory named with the package coordinates (`name#version`). For example, for
`dummy.ig#1.0.0`:

```text
src/main/resources/src-package/
├── dummy.ig-1.0.0.tgz
└── patches/
	└── patches/
		└── dummy.ig#1.0.0/
			└── StructureDefinition-DummyPatient.json
```

The nested `patches/patches/` is intentional in the current implementation: the configured patches
root is `src-package/patches`, and the patch applier looks below that root for `patches/<name>#<version>/`.
Use the exact package name and version. A patch replaces the package file with the same filename;
it does not perform a line-based diff or merge.

## Use the module from the CLI

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

## Troubleshooting checklist

- The manifest is at the exact JAR path `META-INF/config.yaml`, and `configSpecVersion` is `"3.0"`.
- The JAR contains the SPI class and `META-INF/services/de.gematik.refv.valmodule.api.boundary.FhirValidationModule`
  definition.
- The module's `name` matches the CLI `module.name` or `--module-name` value.
- Every group name referenced under `profileFamilies` exists under `packageGroups`.
- Every package group contains at least one package, and every referenced transformation is declared.
- Each source archive filename uses the `name-version.tgz` schema and its package identity and version match the
  manifest entry.
- The generated module JAR contains `META-INF/config.yaml` and the expected generated `.tgz` files.
- `snapshot.config.yaml` includes its own `module` section when the parent passes `--config`.
- Check the Maven/CLI report for package resolution and snapshot errors; a completed CLI invocation
  can still report validation or snapshot errors.

## Limitations

- FHIR Resources containing references to other external resources cannot be validated.
- Codes in FHIR Resources cannot be validated without proper access to a Terminology Server containing such
  terminologies, unless they are available offline completely.