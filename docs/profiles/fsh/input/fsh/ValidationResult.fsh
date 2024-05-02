Profile: REFVValidationResult
Parent: OperationOutcome
Id: REFVValidationResult
Description: "Dieses Profil beschreibt die Struktur des Validierungsergebnisses vom Referenzvalidator"
* insert Meta
* meta MS
  * profile 1..* MS
* issue MS
  * severity MS
  * code MS
  * diagnostics 1..1 MS
  * location MS

Extension: REFVResourceValidityEX
Id: REFVResourceValidityEX
* insert Meta
* value[x] only Coding
* valueCoding 1..1 MS
* valueCoding from REFVResourceValidityVS (required)

ValueSet: REFVResourceValidityVS
Id: REFVResourceValidityVS
* insert Meta
* include codes from system REFVResourceValidityCS

CodeSystem: REFVResourceValidityCS
Id: REFVResourceValidityCS
Title: "Reference validator resource validity codes"
* ^status = #active
* ^content = #complete
* ^count = 2
* #valid "valid"
* #invalid "invalid"

Extension: REFVSourceFile
Id: REFVSourceFile
Title: "OperationOutcome Source File (Backport to FHIR R4 by gematik)"
* value[x] only string
* valueString 1..1 MS
