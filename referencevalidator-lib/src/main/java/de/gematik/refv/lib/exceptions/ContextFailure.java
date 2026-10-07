/*-
 * #%L
 * Validation Core Library
 * %%
 * Copyright (C) 2024 - 2026 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.lib.exceptions;

/** System-level failure. Notice: no Throwable exposed! The 'errorCode' links to internal logs. */
public record ContextFailure(
    String errorCode, // e.g., "ERR-TX-TIMEOUT-9876"
    String message, // Safe, human-readable summary
    FailureCategory category) {

  public enum FailureCategory {
    INITIALIZATION, // NPM packages missing/corrupt
    CONFIGURATION, // Configuration Errors
    NETWORK, // Terminology server unreachable
    PARSING, // Invalid JSON/XML input
    INTERNAL, // Unhandled HL7 engine error
    VALIDATION, // HL7 validation error
    SNAPSHOT, // HL7 snapshot generation error
    PACKAGE_LOADING // Import of Packages from Module error
  }

  public String completeErrorMessage() {
    return String.format("%s: %s", errorCode, message);
  }
}
