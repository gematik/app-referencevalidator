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
package de.gematik.refv.lib.validation.control;

import de.gematik.refv.lib.exceptions.ValidationException;
import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.ValidationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.validation.boundary.Validator;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationRequest;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/** Default FHIR Validator Implementation of the Reference Validator. */
public class DefaultValidator implements Validator {
  private final ValidationContext fhirContext;
  private final boolean ownsContext;

  public DefaultValidator(@NonNull ContextConfiguration contextConfiguration) {
    Objects.requireNonNull(contextConfiguration, "The FHIR Context configuration must be defined");
    this.fhirContext = ContextProvider.defaultProvider().validationContext(contextConfiguration);
    this.ownsContext = true;
  }

  public DefaultValidator(
      @NonNull ContextConfiguration contextConfiguration,
      @NonNull Collection<String> packagesToLoad) {
    Objects.requireNonNull(contextConfiguration, "The FHIR Context configuration must be defined");
    this.fhirContext =
        ContextProvider.defaultProvider().validationContext(contextConfiguration, packagesToLoad);
    this.ownsContext = true;
  }

  /** Uses an already initialized and resource-isolated validation context. */
  public static @NonNull DefaultValidator withValidationContext(
      @NonNull ValidationContext validationContext) {
    return new DefaultValidator(validationContext);
  }

  private DefaultValidator(ValidationContext validationContext) {
    this.fhirContext =
        Objects.requireNonNull(validationContext, "The FHIR Context must be defined");
    this.ownsContext = false;
  }

  @Override
  public void close() {
    if (ownsContext) {
      fhirContext.close();
    }
  }

  @Override
  public @NonNull ValidationResult validate(
      @NonNull ValidationRequest request, @NonNull ValidationOptions validationOptions) {
    try {
      Objects.requireNonNull(request, "The request cannot be null");
      Objects.requireNonNull(validationOptions, "The validation options cannot be null");
      final List<ProfileCanonical> profilesToValidate =
          Objects.nonNull(validationOptions.profileToValidate())
              ? List.of(validationOptions.profileToValidate())
              : List.of();
      final ValidationResult validationResult =
          fhirContext.validate(request.resource(), profilesToValidate);
      // Apply the module-declared message transformations and suppression rules
      final var transformed =
          MessageTransformationApplier.apply(validationResult, validationOptions);
      return ValidationResultFilter.apply(
          transformed.messages(), validationOptions.validationMessagesFilterStrategy());
    } catch (Exception e) {
      throw new ValidationException("Failed to validate the resource", e);
    }
  }
}
