![gematik GmbH](docs/img/Gematik_Logo_Flag.png)

# Release Notes Gematik Referenzvalidator

## Release 0.4.0

### added
- New validation module: isip1 (Informationstechnische Systeme in der Pflege Stufe 1)

### changed
- Updated snapshot packages of validation modules erp and eau due to some errors in profiles

### fixed
- Error correction on validation module eau: for StructureDefinition Extension.seitenlokalisation ([GitHub Issue #3](https://github.com/gematik/app-referencevalidator/issues/3))

## Release 0.3.0

### added
- New validation module: eau (Elektronische Arbeitsunfähigkeitsbescheinigungen)

## Release 0.2.0

### added
- Support for released FHIR-packages (12/2022)
- Support for all profiles of the included packages (except of DAV-PR-ERP-AbgabedatenComposition, DAV-PKV-PR-ERP-AbgabedatenComposition, which do not enable reference resolution)
- Support for input files in JSON format

### changed
- Profiles from the included packages have been converted to snapshots to decrease start-up and validation time
- Bumped dependencies. 

> **Warning**
> Staring from the version 5.6.39 of the HL7 FHIR Validator and the version 6.2.0 of HAPI FHIR Server the evaluation algorithm of regular expressions [has changed](https://github.com/hapifhir/org.hl7.fhir.core/pull/769/commits/535a9cd9c90dc0c9c9a1cb145e8059eba947468a)! Regular expressions without wrapping ^ and $ are now applied to parts of the input and not to the whole string. Instances, which were not valid before, may become valid with the newest versions of the HL7 FHIR Validator. 

- Explicitly excluded CodeSystems in a validation module lead to informational validation messages
- Relative references, which do not exactly match any fullUrl of any entry in a bundle, lead to error validation messages and to invalid instances (see [Reference resolution rules in a bundle](https://build.fhir.org/bundle.html#references))
- Change of the ValidationResult object:
  - Intermediate HAPI validation results removed from the final results object (external manipulation of FHIR context is otherwise possible. Intermediate results can still be found in log output of the validator)
  - Method name to get validation messages changed to getValidationMessages
- Missing ValueSet-definitions in an included package lead to error validation messages and to invalid instances 
- Required changes in profiles are stored separately as "patches" (see Anpassungen der Packages in [README.md](README.md))

### fixed
- piped canonicals in meta.profile of core resources are now resolved correctly and do not lead to invalid instances

## Release 0.1.3

### changed
- configuration for maven assembly and maven central deployment

## Release 0.1.2

### changed
- javadoc and source code packaging for maven central releases

## Release 0.1.1

### changed
- Updates of dependencies
- README.md: examples of maven integration and usage of reference validator

## Release 0.1.0

### added

- Concept with Use Cases, Requirements, Architecture, Development Process
- Generic validation infrastructure
- First version of ErpModule
