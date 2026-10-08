<img align="right" width="250" alt="gematik GmbH" src="https://raw.githubusercontent.com/gematik/gematik.github.io/master/Gematik_Logo_Flag_With_Background.png" />

# gematik Reference Validator

[![Latest GitHub release](https://img.shields.io/github/v/release/gematik/app-referencevalidator?label=release&logo=github)](https://github.com/gematik/app-referencevalidator/releases) [![Maven Central](https://img.shields.io/maven-central/v/de.gematik.refv/referencevalidator.svg)](https://search.maven.org/artifact/de.gematik.refv/referencevalidator) [![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

## Contents

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about">About</a>
       <ul>
        <li><a href="#release-notes">Release Notes</a></li>
      </ul>     
    </li>
    <li>
      <a href="#features">Features</a>
    </li>
    <li>
      <a href="#getting-started">Getting started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#additional-notes-and-disclaimer-from-gematik-gmbh">Additional notes and disclaimer from gematik GmbH</a></li>
    <li><a href="#contributing-and-acknowledgements">Contributing and acknowledgements</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>

For the German README, please follow this link: [README.de.md](README.de.md)

## About

The Reference Validator performs advanced validation of FHIR resources used by applications in Germany's Healthcare
space, including the Telematikinfrastruktur (TI). It provides an authoritative assessment of data validity and can serve
as a reference for other FHIR validators used in TI applications.

See [Use cases, requirements, architecture, and development process](docs/concept/concept.md) for more information.

> [!WARNING]
> Users are responsible for operating the Reference Validator within their applications and system landscapes. gematik
takes appropriate measures to support the security and performance of the Reference Validator but accepts no
responsibility for damage arising from its integration into production systems. See the limitation of liability in
the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). Users must assess the security of the complete
system in light of the technical and organizational conditions of their operating environment. Performance also depends
significantly on that environment.

> [!WARNING]
> The binding role of the Reference Validator's `e-Prescription` (`erp`) module in billing processes is defined
in [Technical Annex 7, Annex 2, to the Medicinal Products Billing Agreement under Section 300 (3) of Book V of the German Social Code](https://www.gkv-datenaustausch.de/leistungserbringer/apotheken/apotheken.jsp).
See
also [E-prescription: technical processability, adjudicator role, and dispute resolution](docs/concept/concept.md#e-rezept-technische-verarbeitbarkeit-schiedsrichter-rolle-und-probleml%C3%B6sungsverfahren).

> [!WARNING]
> The Reference Validator is not intended for checking KIM payloads in production environments (PU).

> [!NOTE]
> The binding status of other validation modules has not been established; gematik recommends their use.

### Release notes

See the [Release Notes file](ReleaseNotes.md) for information about changes.

## Features

- Validate FHIR resources against their referenced profiles.
- Validation coverage follows the [HL7 Java validator](https://www.hl7.org/fhir/validation.html), including:
    - **Structure:** Every instance element must be defined by the referenced profile.
    - **Cardinality:** Minimum and maximum cardinalities are enforced.
    - **Value domains:** Property value domains, including enumerated codes, are checked.
    - **Coding and CodeableConcept bindings:** Instance codes are checked against the code-system definitions in the
      profile.
    - **Constraints and invariants:** Rules defined for profile properties are enforced.
- Check the validity periods of profiles referenced by instances.
- Select FHIR package dependencies based on the instance creation date, for example,
  date-dependent [KBV code tables](https://applications.kbv.de/overview.xhtml).

### Validation modules

Starting with Version 3.0, the Validation Modules `erp`, `eau` and the experimental `erpta7` aren't shipped together
with the main validator CLI application. Only core definitions for FHIR R4 and R5 are included.

Modules have their own Repository and they can be loaded at runtime, by storing them on the filesystem and passing to
the CLI as arguments the path to the folder containing the plugins and the desired module name of the desired plugin.
It is not possible to load multiple plugins at the same time within a single execution of the application.

See the [Module User Guide](./docs/user-guide/module-user-guide.md) for more information about developing your own
validation module.

**TODO**: add list of plugins here

> [!WARNING] The Validation Modules available
> at [this repository](https://github.com/gematik/app-referencevalidator-plugins/releases) aren't incompatible with the
> version 3.0 of the Validator, due to internal format changes and snapshot generation process.

## Getting started

### Prerequisites

The Reference Validator is distributed as a Java library and a command-line application.

The following tools are required for building this project:

* Java JDK 25 or later
* Apache Maven 3.9+

### Installation

#### Command-line application

Download `referencevalidator-cli-X.Y.Z.jar` from
the [GitHub releases](https://github.com/gematik/app-referencevalidator/releases) and place it in a directory of your
choice.

#### Java library

The Reference Validator is published
to [Maven Central](https://search.maven.org/artifact/de.gematik.refv/referencevalidator). Add the library to your
project:

```xml

<dependency>
    <groupId>de.gematik.refv</groupId>
    <artifactId>referencevalidator-lib</artifactId>
    <version>${version.referencevalidator}</version>
</dependency>
```

Replace `${version.referencevalidator}` with the version to use.

> [!WARNING]
> Use the dependency versions specified by the Reference Validator Bill-of-Material (BOM), particularly for HL7 Core
(`ca.uhn.hapi.fhir`).
Different runtime versions may produce different or unexpected validation results.

## Usage

### Command Line

See the [Command-Line Documentation](./docs/user-guide/cli-user-guide.md) for usage instructions.

#### Using plugins with the command-line application

Place plugin `.jar` files in a `plugins` directory or another configured directory. Pass its path using
`--modules-folder`, then select a module with `--module-name`, or, alternatively, configure the parameters in the YAML
configuration.

Example directory layout:

```text
referencevalidator/
├── plugins/
│   └── valmodule-erp-3.0.0.jar
├── referencevalidator-cli-X.Y.Z.jar
└── test-bundle.xml
```

### Java library

See the [Library Documentation](./docs/user-guide/library-user-guide.md) for usage instructions.

## Contributing

If you want to contribute, please check our [CONTRIBUTING.md](./CONTRIBUTING.md).

## License

Copyright 2022-2026 gematik GmbH

Apache License, Version 2.0

See the [LICENSE](./LICENSE.md) for the specific language governing permissions and limitations under the License

## Additional Notes and Disclaimer from gematik GmbH

1. Copyright notice: Each published work result is accompanied by an explicit statement of the license conditions for
   use. These are regularly typical conditions in connection with open source or free software. Programs
   described/provided/linked here are free software, unless otherwise stated.
2. Permission notice: Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
   associated documentation files (the "Software"), to deal in the Software without restriction, including without
   limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
   Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
3. The copyright notice (Item 1) and the permission notice (Item 2) shall be included in all copies or substantial
   portions of the Software.
4. The software is provided "as is" without warranty of any kind, either express or implied, including, but not limited
   to, the warranties of fitness for a particular purpose, merchantability, and/or non-infringement. The authors or
   copyright holders shall not be liable in any manner whatsoever for any damages or other claims arising from, out of
   or in connection with the software or the use or other dealings with the software, whether in an action of contract,
   tort, or otherwise.
5. We take open source license compliance very seriously. We are always striving to achieve compliance at all times and
   to improve our processes. If you find any issues or have any suggestions or comments, or if you see any other ways in
   which we can improve, please reach out to: ospo@gematik.de
6. Parts of this software and - in isolated cases - content such as text or images may have been developed using the
   support of AI tools. They are subject to the same reviews, tests, and security checks as any other contribution. The
   functionality of the software itself is not based on AI decisions.

## Contributing and acknowledgements

Contributions, suggestions, bug reports, and feature requests are welcome. Submit them
through [GitHub Issues](https://github.com/gematik/app-referencevalidator/issues) or by email
to [referenzvalidator@gematik.de](mailto:referenzvalidator@gematik.de).

Parts of this project are based on
the [ABDA E-prescription Reference Validator](https://github.com/DAV-ABDA/eRezept-Referenzvalidator/), copyright 2022
Deutscher Apothekerverband (DAV), licensed under
the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Contact

For questions, suggestions, bug reports, or feature requests,
use [GitHub Issues](https://github.com/gematik/app-referencevalidator/issues) or
email [referenzvalidator@gematik.de](mailto:referenzvalidator@gematik.de).


