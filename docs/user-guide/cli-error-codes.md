# CLI Error Codes

The CLI prefixes command-level failures with a `REFV-` identifier. Validation and snapshot reports
also include these identifiers alongside individual messages. The identifier categorizes the issue;
the accompanying message contains the path, parser detail, or other context needed to resolve it.
Command-level failures return exit code `1`. Validation message codes do not, on their own, determine
the exit code; the message severity and the command's validation result do.

| Code       | Identifier                           | Meaning                                                                               |
|------------|--------------------------------------|---------------------------------------------------------------------------------------|
| `REFV-000` | `NO_ERROR`                           | No error was found or processing completed successfully.                              |
| `REFV-001` | `INIT_CONTEXT_FAILED`                | Configuration could not be loaded or the validation context could not be initialized. |
| `REFV-002` | `VALIDATION_ERROR`                   | A FHIR resource failed validation or a validation-related operation failed.           |
| `REFV-003` | `PROFILE_FILTER_MISMATCH`            | The resource's profile did not match the configured profile filter.                   |
| `REFV-004` | `NO_CREATION_DATE_IN_RESOURCE`       | A resource did not contain a creation date required by the validation rules.          |
| `REFV-005` | `PROFILE_OUTSIDE_OF_VALIDITY_PERIOD` | The selected profile is outside its configured validity period.                       |
| `REFV-006` | `PROFILE_PARSE_ERROR`                | A profile or input could not be parsed, or its format could not be identified.        |
| `REFV-007` | `UNSUPPORTED_PROFILE`                | The requested profile is not supported.                                               |
| `REFV-008` | `MISSING_PROFILE`                    | A required profile definition could not be found.                                     |
| `REFV-009` | `IO_ERROR`                           | A file or directory could not be found, read, or otherwise accessed.                  |
| `REFV-010` | `TERMINOLOGY_SERVER_ERROR`           | A terminology-server operation failed.                                                |
| `REFV-011` | `INTERNAL_ERROR`                     | An unexpected internal failure occurred.                                              |
| `REFV-012` | `COMPONENT_NOT_MATCHING`             | A component-matching check failed.                                                    |
| `REFV-013` | `PACKAGE_LOADING_ERROR`              | A package or its dependencies could not be loaded.                                    |
| `REFV-014` | `SNAPSHOT_GENERATION_ERROR`          | Snapshot generation failed.                                                           |
| `REFV-015` | `SNAPSHOT_ALREADY_PROCESSED`         | The snapshot was already processed and was not generated again.                       |
| `REFV-016` | `INTERNAL_CONTEXT`                   | An internal validation-context or message conversion failure occurred.                |
| `REFV-017` | `VALIDATION_INFO`                    | Informational validation result; this is not necessarily a failure.                   |

Configuration and initialization errors commonly use `REFV-001`; file-access failures use
`REFV-009`, while parsing or validation failures use `REFV-006` or `REFV-002` as appropriate.
Package, terminology, and snapshot failures use their respective category codes. Detailed causes
are included in the diagnostic when available. Run with `-d` to include debug logs and stack traces.
