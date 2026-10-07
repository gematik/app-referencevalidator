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

import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.fhir_context.boundary.ValidationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.validation.control.DefaultValidator;
import java.util.Collection;
import org.jspecify.annotations.NonNull;

/** Factory record that builds a new {@link Validator} instance, based on input parameters */
public final class ValidatorFactory {
  private ValidatorFactory() {}

  /**
   * Initializes a new {@link Validator} instance with the provided configuration.
   *
   * <p>The validator loads <strong>only</strong> the specific core FHIR packages for the given FHIR
   * Release Version.
   *
   * <p>The returned validator owns its context and must be closed when validation is complete.
   *
   * @param contextConfiguration the {@link ContextConfiguration} configuration, defining the
   *     behavior of the Validator
   * @return a new {@link Validator} instance in case of success
   * @throws InitializationException in case of initialization errors
   */
  public static @NonNull Validator withCoreDefinitions(
      @NonNull ContextConfiguration contextConfiguration) throws InitializationException {
    try {
      return new DefaultValidator(contextConfiguration);
    } catch (Exception e) {
      throw new InitializationException(e.getLocalizedMessage(), e);
    }
  }

  /**
   * Initializes a new {@link Validator} instance with the provided configuration and set of FHIR
   * implementation Guides to be loaded for the validation of resource.
   *
   * <p>It loads the specified packages into the context <strong>before</strong> attempting to
   * execute the validation of resources.
   *
   * <p>The returned validator owns its context and must be closed when validation is complete.
   *
   * @param contextConfiguration the {@link ContextConfiguration} configuration, defining the
   *     behavior of the Validator
   * @param packagesToLoad the packages to be loaded for the validation
   * @return a new {@link Validator} instance in case of success
   * @throws InitializationException in case of initialization errors
   */
  public static @NonNull Validator withCustomPackages(
      @NonNull ContextConfiguration contextConfiguration,
      @NonNull Collection<String> packagesToLoad)
      throws InitializationException {
    try {
      return new DefaultValidator(contextConfiguration, packagesToLoad);
    } catch (Exception e) {
      throw new InitializationException(e.getLocalizedMessage(), e);
    }
  }

  /**
   * Creates a validator around an already initialized, resource-isolated validation context.
   *
   * <p>The caller retains ownership of the supplied context and must keep it open for the lifetime
   * of this validator.
   *
   * @param validationContext initialized context used exclusively by this validator
   * @return a validator backed by the supplied context
   * @throws InitializationException if the validator cannot be initialized
   */
  public static @NonNull Validator withValidationContext(
      @NonNull ValidationContext validationContext) throws InitializationException {
    try {
      return DefaultValidator.withValidationContext(validationContext);
    } catch (Exception e) {
      throw new InitializationException(
          "Failed to initialize a validator with the supplied validation context: "
              + e.getLocalizedMessage(),
          e);
    }
  }
}
