/*-
 * #%L
 * Reference Validator Library
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
package de.gematik.refv.lib.validation.boundary;

import de.gematik.refv.lib.exceptions.ParsingException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.ProfileValidity;
import de.gematik.refv.lib.fhir_context.entity.ValidationModuleIndex;
import de.gematik.refv.lib.validation.control.DefaultProfileValidityExtractor;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import org.jspecify.annotations.NonNull;

/** Interface to extract the profile validity from a given FHIR Resource. */
public interface ProfileValidityExtractor {
  /**
   * Extracts the profile information from a given {@link FhirResource} and searches the packages
   * that are supporting it, from a given Validation Module configuration.
   *
   * @param fhirResource the FHIR Resource to parse
   * @param fhirRelease the FHIR Release
   * @param validationOptions custom validation options
   * @param validationModuleIndex the index containing the Validation Module configuration
   * @return a valid {@link ProfileValidity} instance
   * @throws ParsingException in case of errors while parsing the given resource
   */
  @NonNull ProfileValidity extractProfileValidity(
      @NonNull FhirResource fhirResource,
      @NonNull FhirRelease fhirRelease,
      @NonNull ValidationOptions validationOptions,
      @NonNull ValidationModuleIndex validationModuleIndex)
      throws ParsingException;

  /** Default implementation of the ProfileGroupExtractor */
  static ProfileValidityExtractor defaultExtractor() {
    return new DefaultProfileValidityExtractor();
  }
}
