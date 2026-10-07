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
package de.gematik.refv.lib.fhir_context.boundary;

import de.gematik.refv.lib.exceptions.ValidationException;
import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import java.util.Collection;
import org.jspecify.annotations.NonNull;

/**
 * Represents an initialized and reusable FHIR processing context.
 *
 * <p>The context encapsulates the concrete FHIR engine, loaded packages, terminology configuration,
 * and any implementation-specific state required for validation.
 *
 * <p>A context can be reused for multiple processing operations, provided that the configured
 * context supports the calling concurrency model.
 *
 * <p>The context must be closed when it is no longer required.
 */
public interface ValidationContext extends FhirContext {

  /**
   * Performs the validation of a given resource against custom-supplied profiles or the profile
   * defined in the resource itself.
   *
   * @param fhirResource the FHIR resource to be validated
   * @param profiles a list of {@link ProfileCanonical} to validate against. If empty, it falls back
   *     to validate core HL7 FHIR structures
   * @return a {@link ResultMessage} containing the information about the validation status of the
   *     resource
   * @throws ValidationException in case of internal failures
   */
  @NonNull ValidationResult validate(
      @NonNull FhirResource fhirResource, @NonNull Collection<ProfileCanonical> profiles)
      throws ValidationException;

  /**
   * Clones this {@link ValidationContext} instance.
   *
   * @param resolvedPackages a list of packages that need to be imported in the new cloned context
   * @return a clone of the existing instance
   */
  @NonNull ValidationContext cloneContext(@NonNull Collection<ResolvedPackage> resolvedPackages);
}
