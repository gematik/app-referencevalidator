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

/** Base exception for all internal Reference Validator failures. */
public sealed class RefValException extends RuntimeException
    permits ConfigurationException,
        CyclicDependencyException,
        DependencyLoadFailedException,
        InitializationException,
        InvalidDateFormatException,
        InvalidModuleConfigurationException,
        LoadModuleException,
        MissingProfileDefinitionException,
        PackageDownloadException,
        PackageLoadFailedException,
        PackageParsingFailedException,
        ParsingException,
        ProfileMismatchException,
        ProfileOutsideValidityPeriodException,
        ProfilePatchException,
        SnapshotGenerationException,
        TerminologyServerException,
        UnsupportedFileTypeException,
        UnsupportedProfileException,
        ValidationException {

  private final ContextFailure.FailureCategory category;

  protected RefValException(String message, ContextFailure.FailureCategory category) {
    super(message);
    this.category = category;
  }

  protected RefValException(
      String message, Throwable cause, ContextFailure.FailureCategory category) {
    super(message, cause);
    this.category = category;
  }

  public ContextFailure.FailureCategory getCategory() {
    return category;
  }
}
