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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.exceptions.ValidationException;
import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.ValidationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationRequest;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ValidatorRequirementsTest {
  @ParameterizedTest(name = "{0}")
  @MethodSource("creationCases")
  void createsValidators(String requirement) throws Exception {
    var configuration = ContextConfiguration.defaultConfiguration();
    var context = mock(ValidationContext.class);
    var provider = mock(ContextProvider.class);
    var packages = List.of("example#1.0.0");
    try (var providers = mockStatic(ContextProvider.class)) {
      providers.when(ContextProvider::defaultProvider).thenReturn(provider);
      when(provider.validationContext(configuration)).thenReturn(context);
      when(provider.validationContext(configuration, packages)).thenReturn(context);
      when(context.validate(any(), any())).thenReturn(success());
      switch (requirement) {
        case "R1.1" -> {
          try (var validator = ValidatorFactory.withCoreDefinitions(configuration)) {
            validator.validate(request(), ValidationOptions.defaultConfiguration());
            verify(provider).validationContext(configuration);
            verifyNoMoreInteractions(provider);
            verify(context).validate(any(), eq(List.of()));
          }
        }
        case "R1.2" -> {
          try (var validator = ValidatorFactory.withCustomPackages(configuration, packages)) {
            validator.validate(request(), ValidationOptions.defaultConfiguration());
            var order = inOrder(provider, context);
            order.verify(provider).validationContext(configuration, packages);
            order.verify(context).validate(any(), any());
          }
        }
        case "R1.3" -> {
          try (var validator = ValidatorFactory.withValidationContext(context)) {
            validator.validate(request(), ValidationOptions.defaultConfiguration());
            verify(context).validate(any(), any());
          }
          verify(context, never()).close();
          providers.verifyNoInteractions();
        }
        case "R1.4" -> {
          var cause = new IllegalStateException("initialization failed");
          when(provider.validationContext(configuration)).thenThrow(cause);
          assertSame(
              cause,
              assertThrows(
                      InitializationException.class,
                      () -> ValidatorFactory.withCoreDefinitions(configuration))
                  .getCause());
          assertInstanceOf(
              NullPointerException.class,
              assertThrows(
                      InitializationException.class,
                      () -> ValidatorFactory.withCoreDefinitions(null))
                  .getCause());
          assertInstanceOf(
              NullPointerException.class,
              assertThrows(
                      InitializationException.class,
                      () -> ValidatorFactory.withValidationContext(null))
                  .getCause());
          assertThrows(
              InitializationException.class,
              () -> ValidatorFactory.withCustomPackages(configuration, null));
        }
        default -> fail("Unexpected requirement " + requirement);
      }
    }
  }

  static Stream<String> creationCases() {
    return Stream.of("R1.1", "R1.2", "R1.3", "R1.4");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validationCases")
  void validatesResources(String requirement) throws Exception {
    var context = mock(ValidationContext.class);
    when(context.validate(any(), any())).thenReturn(success());
    try (var validator = ValidatorFactory.withValidationContext(context)) {
      switch (requirement) {
        case "R2.1" -> {
          for (var resource :
              List.of(
                  request().resource(),
                  FhirResource.fromXml("<Patient xmlns=\"http://hl7.org/fhir\"/>"))) {
            var result =
                validator.validate(
                    new ValidationRequest(resource), ValidationOptions.defaultConfiguration());
            assertEquals(success(), result);
            verify(context).validate(same(resource), eq(List.of()));
          }
        }
        case "R2.2" -> {
          var profile = ProfileCanonical.fromCanonical("http://example.org/Profile|1.2");
          var options =
              new ValidationOptions(
                  profile,
                  null,
                  ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
                  ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE);
          var request = request();
          validator.validate(request, options);
          verify(context).validate(same(request.resource()), eq(List.of(profile)));
        }
        case "R2.3" -> {
          var request = request();
          validator.validate(request, ValidationOptions.defaultConfiguration());
          verify(context).validate(same(request.resource()), eq(List.of()));
        }
        case "R2.4" -> {
          var request = request();
          var options = ValidationOptions.defaultConfiguration();
          assertInstanceOf(
              NullPointerException.class,
              assertThrows(ValidationException.class, () -> validator.validate(null, options))
                  .getCause());
          assertInstanceOf(
              NullPointerException.class,
              assertThrows(ValidationException.class, () -> validator.validate(request, null))
                  .getCause());
          var cause = new IllegalArgumentException("engine failure");
          when(context.validate(any(), any())).thenThrow(cause);
          assertSame(
              cause,
              assertThrows(ValidationException.class, () -> validator.validate(request, options))
                  .getCause());
          doReturn(null).when(context).validate(any(), any());
          assertInstanceOf(
              NullPointerException.class,
              assertThrows(ValidationException.class, () -> validator.validate(request, options))
                  .getCause());
        }
        default -> fail("Unexpected requirement " + requirement);
      }
    }
  }

  static Stream<String> validationCases() {
    return Stream.of("R2.1", "R2.2", "R2.3", "R2.4");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("lifecycleCases")
  void closesOnlyOwnedContexts(String requirement, boolean owned) throws Exception {
    var context = mock(ValidationContext.class);
    var provider = mock(ContextProvider.class);
    var configuration = ContextConfiguration.defaultConfiguration();
    try (var providers = mockStatic(ContextProvider.class)) {
      providers.when(ContextProvider::defaultProvider).thenReturn(provider);
      when(provider.validationContext(configuration)).thenReturn(context);
      when(provider.validationContext(eq(configuration), anyCollection())).thenReturn(context);
      if (owned) {
        ValidatorFactory.withCoreDefinitions(configuration).close();
        ValidatorFactory.withCustomPackages(configuration, List.of("example#1.0.0")).close();
        verify(context, times(2)).close();
      } else {
        ValidatorFactory.withValidationContext(context).close();
        verify(context, never()).close();
      }
    }
  }

  static Stream<Arguments> lifecycleCases() {
    return Stream.of(Arguments.of("R7.1", true), Arguments.of("R7.2", false));
  }

  static ValidationRequest request() {
    return new ValidationRequest(FhirResource.fromJson("{\"resourceType\":\"Patient\"}"));
  }

  static ValidationResult success() {
    return ValidationResult.forMessage(IssueSeverity.INFORMATION, "REFV-OK", "valid");
  }
}
