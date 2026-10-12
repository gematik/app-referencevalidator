<img align="right" width="250" alt="gematik GmbH" src="https://raw.githubusercontent.com/gematik/gematik.github.io/master/Gematik_Logo_Flag_With_Background.png" />

# Elektronische Arbeitsunfähigkeitsbescheinigung (eAU) Validation Module

[![Latest GitHub release](https://img.shields.io/github/v/release/gematik/app-referencevalidator-module-eau?label=release&logo=github)](https://github.com/gematik/app-referencevalidator-module-eau/releases) [![Maven Central](https://img.shields.io/maven-central/v/de.gematik.refv.valmodule/eau.svg)](https://search.maven.org/artifact/de.gematik.refv.valmodule/eau) [![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

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

## About

This Validation Module defines the Implementation Guide for validating FHIR Resources Context of the Elektronische
Arbeitsunfähigkeitsbescheinigung (eAU). This Module is intended to be used with
the [gematik Reference Validator](https://github.com/gematik/app-referencevalidator).

### Release notes

See the [Release Notes](ReleaseNotes.md) file for information about changes.

## Getting started

### Prerequisites

In order to build the Validation Module, you need the following tools and frameworks:

* Java JDK 25 or later
* Apache Maven 3.9+
* gematik Reference Validator 3.0.0+ (automatically downloaded through Maven)

Additionally, you need internet access, to fetch the FHIR Implementation Guide for ISiK from the official FHIR registry.

### Installation

You can build the Validation Module with the following command:

```
mvn clean install
```

The result will be a .JAR File located in the `target` directory, which can be used with the gematik Reference Validator
to validate FHIR Resources.

If you don't want to build the Validation Module yourself, you can download the latest version directly from
the [GitHub Release page](https://github.com/gematik/app-referencevalidator-module-eau/releases).

## Usage

Follow the documentation at the [gematik Reference Validator](https://github.com/gematik/app-referencevalidator)
repository for usage instructions.

> [!NOTE]
> You need to define the `module-name` parameter or, in the configuration YAML file, you need to set the key
`module.name` as `eau`

See as an example the [`validator.config.yaml`](./validator.config.yaml) file in this repository.

### Validation

* Instances of FHIR Profiles that contain unknown Canonicals or Extensions result in an unsuccessful Validation Result.
* ICD-10-Codes (CodeSystems `http://fhir.de/CodeSystem/dimdi/icd-10-gm` and `http://fhir.de/CodeSystem/bfarm/icd-10-gm`)
are not validated.

## License

Copyright 2026 gematik GmbH

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
