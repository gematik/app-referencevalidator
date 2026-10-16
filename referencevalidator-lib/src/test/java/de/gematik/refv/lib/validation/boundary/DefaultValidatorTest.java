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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.refv.lib.fhir_context.boundary.ValidationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.MessageId;
import de.gematik.refv.lib.validation.control.DefaultValidator;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationRequest;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultValidatorTest {
  @DisplayName("Given a null context, when created, then a NullPointerException is thrown")
  @Test
  void expectNullContextThrows() {
    Assertions.assertThrows(
        NullPointerException.class, () -> new DefaultValidator((ContextConfiguration) null));
  }

  @DisplayName("Given a context that throws, when validating, then a ValidationException is thrown")
  @Test
  void expectValidateWrapsErrorsInValidationException() {
    try (var validator = new DefaultValidator(ContextConfiguration.defaultConfiguration())) {
      final var request =
          new ValidationRequest(FhirResource.fromJson("{\"resourceType\":\"Patient\"}"));
      Assertions.assertDoesNotThrow(
          () -> validator.validate(request, ValidationOptions.defaultConfiguration()));
    }
  }

  @DisplayName("Uses the caller-supplied isolated context without cloning or closing it")
  @Test
  void expectSuppliedValidationContextIsUsedDirectly() throws Exception {
    var context = mock(ValidationContext.class);
    var resource = FhirResource.fromJson("{\"resourceType\":\"Patient\"}");
    var expected =
        ValidationResult.forMessage(
            IssueSeverity.INFORMATION, MessageId.VALIDATION_ERROR.getCode(), "valid");
    when(context.validate(resource, List.of())).thenReturn(expected);

    try (context) {
      var validator = ValidatorFactory.withValidationContext(context);
      var result =
          validator.validate(
              new ValidationRequest(resource), ValidationOptions.defaultConfiguration());

      assertEquals(expected, result);
      verify(context).validate(resource, List.of());
      verify(context, never()).cloneContext(anyCollection());
      verify(context, never()).close();
    }
  }
}
