<img align="right" width="250" height="47" src="docs/img/Gematik_Logo_Flag.png"/> <br/>

# Release Notes Gematik Referenzvalidator

## Release 2.6.1

> [!IMPORTANT] 
> ERP module (version update to 2.6)

#### fixed

  - The ERP module evaluated `GEM_ERP_PR_Medication|1.4` resources as invalid if started on a machine with non-english locale (e.g. de_DE) and if the resources used the code system `http://standardterms.edqm.eu` to encode the `form` element (cf. [GitHub Issue 28](https://github.com/gematik/app-referencevalidator/issues/28)).  The behavior has been fixed. 
  - Plugins, which made use of the `ignoredValueSets` [configuration option](https://github.com/gematik/app-referencevalidator-plugins?tab=readme-ov-file#plugin-definitionsdatei-configyaml) could invalidate and produce error messages for resources, which used codes coming from the configured ValueSets. 

## Release 2.6.0

### ERP module (version update to 2.5):

#### added
  - integrated [de.gematik.erezept-patientenrechnung.r4 1.0.4](https://simplifier.net/packages/de.gematik.erezept-patientenrechnung.r4/1.0.4/~introduction) package (valid from 15.1.2025)
  - integrated [de.gematik.erezept-workflow.r4 1.4.3](https://simplifier.net/packages/de.gematik.erezept-workflow.r4/1.4.3) package (valid from 15.1.2025). See current limitations in [E-Rezept-Modul section of README.md](README.md#e-rezept-modul)

#### changed
  - validity period of [de.abda.erezeptabgabedatenpkv.1.2.0](https://simplifier.net/packages/de.abda.erezeptabgabedatenpkv/1.2.0) package extended till 30.6.2025  (cf. [Technische Anlage](https://www.abda.de/fileadmin/user_upload/assets/ehealth/PKV_Datenauschtausch/TA_DAV_PKV_004_20240801.pdf))
 
### EAU module (version update to 0.91):

#### added
  - integrated [KBV Schlüsseltabelle S_KBV_DMP v1.06](https://applications.kbv.de/S_KBV_DMP_V1.06.xhtml) (valid from 1.10.2024)
  - integrated [KBV Schlüsseltabelle S_KBV_PERSONENGRUPPE v1.03](https://applications.kbv.de/S_KBV_PERSONENGRUPPE_V1.03.xhtml) (valid from 1.4.2024)

#### changed
- EAU module performs selection of FHIR-packages based on the instance creation date (cf. [README.md](README.md#eau-modul))
- EAU module performs profile validity checks based on the instance creation date 

#### fixed
- End of validity period for the _KBV_PR_EAU_Bundle|1.0.2_ profile (31.12.2023)  (cf. [Technische_Anlage_eAU](https://update.kbv.de/ita-update/DigitaleMuster/eAU/KBV_ITA_VGEX_Technische_Anlage_eAU.pdf))  

### Experimental ERPTA7 module (version update to 0.3):

#### added
- support for _GKVSV_PR_TA7_Rechnung_Bundle|1.4_ profile added (valid from 1.11.2024)

## Release 2.5.1

### fixed
- declining XML resources with DTD instructions due to vulnerability to XML eXternal Entity injection and thus Server Side Request Forgery (SSRF) attacks

## Release 2.5.0

### added

- ERP module  (version update to 2.4):
  - integrated [de.abda.eRezeptAbgabedatenPKV#1.3.0](https://simplifier.net/packages/de.abda.erezeptabgabedatenpkv/1.3.0) package (valid from 1.11.2024)
  - integrated [KBV Schlüsseltabelle S_KBV_DARREICHUNGSFORM v1.14](https://applications.kbv.de/S_KBV_DARREICHUNGSFORM_V1.14.xhtml) (valid from 1.10.2024)
- Added warnings for the case, when Bundle.entry.fullUrl doesn't match the Bundle.entry.id (cf. [Bundle definitions](https://hl7.org/fhir/R4/bundle-definitions.html#Bundle.entry.fullUrl)). Notice: warnings are generally issued only if the validator has been started with the `--verbose` option.
 
## Release 2.4.0

### added

- validation option `profile-filter` to validate meta.profile references against a specified regular expression (cf. [README.md](README.md))
- ERP module  (version update to 2.3):
  -  integrated [KBV Schlüsseltabelle S_KBV_DMP v1.06](https://applications.kbv.de/S_KBV_DMP.xhtml) (valid from 1.10.2024)
  
### changed:

- ERP module (version update to 2.3):
  - validity of 1.3 profiles from de.abda.erezeptabgabedaten package extended till 15.4.2025
  - integrated [de.abda.erezeptabgabedaten#1.4.1](https://simplifier.net/packages/de.abda.erezeptabgabedaten/1.4.1) and [de.abda.erezeptabgabedatenbasis#1.4.1](https://simplifier.net/packages/de.abda.erezeptabgabedatenbasis/1.4.1), which replace the corresponding 1.4.0 packages 

## Release 2.3.0

### added:
- ERP module: new packages: 
  - [de.gkvsv.erezeptabrechnungsdaten#1.3.3](https://simplifier.net/packages/de.gkvsv.erezeptabrechnungsdaten/1.3.4)
  - [de.gkvsv.erezeptabrechnungsdaten#1.4.1](https://simplifier.net/packages/de.gkvsv.erezeptabrechnungsdaten/1.4.1)
  - [de.abda.erezeptabgabedaten#1.4.0](https://simplifier.net/packages/de.abda.erezeptabgabedaten/1.4.0)
  - [de.gematik.erezept-patientenrechnung.r4#1.0.3](https://simplifier.net/packages/de.gematik.erezept-patientenrechnung.r4/1.0.3)
  - [KBV Schlüsseltabelle S_KBV_DARREICHUNGSFORM v1.13](https://applications.kbv.de/S_KBV_DARREICHUNGSFORM_V1.13.xhtml)
  - [KBV Schlüsseltabelle S_KBV_PERSONENGRUPPE v1.03](https://applications.kbv.de/S_KBV_PERSONENGRUPPE_V1.03.xhtml)

### changed:
- ERP module: [gematik Workflow Package 1.3.0](https://simplifier.net/packages/de.gematik.erezept-workflow.r4/1.3.0) has been replaced by [gematik Workflow Package 1.3.1](https://simplifier.net/packages/de.gematik.erezept-workflow.r4/1.3.1) (valid from 1.11.2024)
- ERP module: for completeness purposes dependency lists for the `de.gematik.erezept-patientenrechnung.r4` specification have been extended with dependencies on `de.gematik.erezept-workflow.r4`-package referenced in the corresponding package.json manifest files. Although the current HAPI version (6.6.2) doesn't require these dependencies, this might change in the future. The main packages have been also updated with the latest versions from [simplifier.net](https://simplifier.net/packages/de.gematik.erezept-patientenrechnung.r4/), which have no changes but updated dependency references.
- ERP module: removed dependencies of `de.gkvsv.erezeptabrechnungsdaten`, `de.gkvsv.erezeptabgabedaten` on the KBV Schlüsseltabellen (were not used in the profiles)
  
## Release 2.2.0

### added
- multiple files and directories can be passed as CLI arguments separated with commas (cf. [README.md](README.md))
- validation results can be converted to OperationOutcome and written to a specified output file (cf. [README.md](README.md))
- ERP module: [gematik Workflow Package 1.3.0](https://simplifier.net/packages/de.gematik.erezept-workflow.r4/1.3.0) integrated (valid from 1.11.2024)

### changed
- ERPTA7 module: small performance optimizations (fullUrl uniqueness check) and extended logging

## Release 2.1.1

### fixed
- while using plugins the validator didn't recognize profiles without the pipe and version for the profile URL. The issue has been fixed. Now both piped and non-piped profile URLs are recognized correctly.

## Release 2.1.0

### added
- ERPTA7 module: experimental eRezept Abrechnungsdaten validation module with optimized performance

### changed
- if a validation plugin configuration allowed multiple profiles to be referenced in a single FHIR resource (which is the case in ISIK plugins for instance) and no of the referenced profiles in the resource was known to the plugin, the plugin performed validation against core structure definitions only. The behavior has been changed to produce an error message instead. The reason is that the above situation usually indicates a wrong usage of the plugin, e.g. a missing `--profile` parameter. For validation of resources against core structure definitions only, the `core` validation module should be used explicitly. 

### fixed
- the validator crashed for rare cases of FHIR resources with incorrectly declared extensions ([GitHub Issue 12](https://github.com/gematik/app-referencevalidator/issues/12)). The issue has been fixed.

## Release 2.0.2

### fixed
- optimized usage of internal caches to mediate the memory leak issue https://github.com/hapifhir/org.hl7.fhir.core/issues/1412
- optimized package loading mechanism to reduce memory consumption

## Release 2.0.1

### fixed
- fixed a bug in the snapshot generator (snapshot generation failed for dependencies with uppercase name parts)

## Release 2.0.0

### added
- Support for external validation modules (aka plugins)

### changed
- ISIK1, ISIK2, ISIK3, ISIP, DIGA have been extracted as external plugins (cf. https://github.com/gematik/app-referencevalidator-plugins)

## Release 1.1.0

### added
- New validation module: ISIK-3 (Informationstechnische Systeme in Krankenhäusern Stufe 3)

### changed
- ERP module: Breaking Change! UCUM validation is not obligatory anymore. Non-valid UCUM codes produce warnings but do not cause instances to become invalid
- ERP module: new packages de.abda.eRezeptAbgabedaten 1.3.2 and kbv.ita.erp.1.1.2 integrated
- ISIK-1 module: removed unnecessary package de.basisprofil.r4-0.9.13.tgz
- ISIK-2 module: removed unnecessary packages de.basisprofil.r4-0.9.13.tgz, dvmd.kdl.r4.2022-2022.1.1.tgz, hl7.fhir.uv.ips-1.1.0.tgz and ihe.mhd.fhir-4.0.1.tgz

### fixed
- fixed a bug, where comments in xml files resulted in misleading informational validation messages

## Release 1.0.0

### added
- core validation module: validation of FHIR core resources

### changed
- error message on a violated profile validity period has been extended with information on the profile validity period
- start parameter --errors-only removed as it already corresponds to the default behavior 

### fixed
- suppression of profiling errors from the de.abda.erezeptabgabedaten-1.0.3.tgz FHIR package now works independently of the current system locale
- comma-separated lists of accepted encodings were ignored. the behavior has been fixed
- ERP module: validity end date of the de.gematik.erezept-workflow.r4-1.0.3-1.tgz package has been set to 31.12.2021 (cf. [E-Rezept FHIR-Package Versionsmanagement](https://github.com/gematik/api-erp/blob/265d9718d1b06b805d7acaac36bdb751ec724cde/docs/erp_fhirversion.adoc))
- by calling the validator using the Java API the output contained all types of messages (INFO, WARNING, ERROR, FATAL). this was in contrast to the CLI, which contained ERROR and FATAL messages only by default. now, the API call returns ERROR and FATAL messages by default as well and its behavior can be adjusted using the ValidationOptions.setValidationMessagesFilter method.   

## Release 0.7.2

### changed
- setting the timezone for determining FHIR package lists to Europe/Berlin instead of default system timezone
- bumping dependencies

## Release 0.7.1

### added
- kbv.ita.eau 1.1.1 integrated
- de.gematik.erezept-workflow.r4 1.2.2 integrated
- de.abda.erezeptabgabedatenbasis 1.2.1 integrated

### changed
- corrections in ERP and EAU FHIR-Package dependencies
- resources with profiles from kbv.ita.erp#1.0.2 can now be validated if embedded into GEM_ERP_PR_MedicationDispense 1.2-resources
- fixed Java example in README.md for usage of ValidationOptions
- performance optimizations
- no INFO or WARNINGS if not in verbose mode

## Release 0.7.0

### added
- Profile validity period checks based on instance creation date
- New parameter to set a profile for validation
- Output of release version and help on empty input
- New parameter to print supported profiles and FHIR packages of a validation module
- New parameter to override module setting on accepted encodings 
- KBV_CS_SFHIR_KBV_DARREICHUNGSFORM_V1.12.xml integrated for ERP and EAU modules

### changed
- Usage of different FHIR package dependencies based on the instance creation date
- Performance optimization (memory consumption, multithreading)
- Module name and input file are passed directly to the command line tool instead of using -m and -i parameters respectively
- Upgrade to HAPI 6.6.1
- E-Rezept module accepts only XML encoding by default


## Release 0.6.1

### added
- New validation module: diga (Digitale Gesundheitsanwendungen)
- New command line arguments for filtering the output of the validation process

### changed
- output of found issues as a table for better readability
- removed packages de.gematik.erezept-patientenrechnung.r4-1.0.0.tgz, kbv.ita.erp-1.1.0.tgz and de.gematik.erezept-workflow.r4-1.2.0.tgz
- added packages de.gematik.erezept-patientenrechnung.r4-1.0.1.tgz, kbv.ita.erp-1.1.1.tgz and de.gematik.erezept-workflow.r4-1.2.1.tgz

## Release 0.5.0

### added
- support for the 1.2 version of profiles in de.abda.erezeptabgabedatenpkv (package de.abda.erezeptabgabedatenpkv-1.2.0)
- New validation module: isik2 (Informationstechnische Systeme in Krankenhäusern Stufe 2)
- New validation module: isik1 (Informationstechnische Systeme in Krankenhäusern Stufe 1)
- KBV_CS_SFHIR_KBV_DARREICHUNGSFORM_V1.11.xml integrated

### changed
- removed packages de.abda.erezeptabgabedaten-1.3.0, de.abda.erezeptabgabedatenpkv-1.1.0
- added packages (erp): de.abda.erezeptabgabedaten-1.3.1, de.abda.erezeptabgabedatenbasis-1.3.1 


## Release 0.4.1

### fixed
- Removed snapshot dependency in the core module 

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
