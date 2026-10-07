# Regressions detected from the Version 2.17.0 to 3.0.0

With the upgrade of the Reference Validator to 3.0.0, we have swapped the core FHIR Engine from the HAPI FHIR Framework
to use the HL7 Core Validation library.

The upgrade brings some breaking changes and also strong regressions in the existing tests: we collect them in this
document as future reference and help also with the migration to the newer version of the Validator.

## R1 - No allowed double quotes in FHIR Path Expressions

The newer version of the Validator does not allow FHIR Path Expressions that are using double quotes (""), since it
isn't conform to the standard. Older FHIR Profiles that use such Expressions won't be able to be validated.

## R2 - Unknown Code Systems are not marked as Error

If a Code System can't be resolved during the validation operation (e.g. in offline mode, without an existing
Terminology Server), the Validator returns a `WARNING` message instead of `ERROR`.

## R3 - References in Bundle are marked as Error when not found

If a Bundle contains external URLs or references to other Resources, which aren't available or don't match in the Bundle
itself, these are marked as `ERROR`.

## R4 - CodeSystems and ValueSets resolution

If CodeSystems and ValueSets in FHIR Profile don't contain a declared version, they are automatically matched against
the latest FHIR Release Version (for the time being, R5).
If you want to suppress the validation error that might come from this, you need to add a Message Transformation entry
in your configuration file, like:

```yaml
...
validationOptions:
  validationMessagesFilterStrategy: "KEEP_ALL"
  profileValidityPeriodCheckStrategy: "VALIDATE"
  messageTransformations: 
    -  severityLevelFrom: "error"
       severityLevelTo: "information"
       locatorString: "http://hl7.org/fhir/encounter-status#finished"
       messageId: "Unknown_Code_in_Version"
...
```