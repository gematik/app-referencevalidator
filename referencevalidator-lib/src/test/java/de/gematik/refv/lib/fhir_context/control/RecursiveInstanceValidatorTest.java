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
package de.gematik.refv.lib.fhir_context.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hl7.fhir.model.core.ElementDefinition;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.model.core.VersionResolutionRules;
import org.hl7.fhir.services.context.IWorkerContext;
import org.junit.jupiter.api.Test;

class RecursiveInstanceValidatorTest {
  @Test
  void keepsOnlyPermittedTypesWithoutChangingTheLoadedDefinition() {
    var context = mock(IWorkerContext.class);
    var validator = mock(RecursiveInstanceValidator.class);
    when(validator.getContext()).thenReturn(context);
    var definition = definition("patient-one", "patient-two", "observation");
    when(validator.typeOnlyDefinition(definition)).thenCallRealMethod();
    profile(context, "patient-one", "Patient");
    profile(context, "patient-two", "Patient");
    profile(context, "observation", "Observation");

    var result = validator.typeOnlyDefinition(definition);

    assertNotSame(definition, result);
    assertEquals(
        List.of("Patient", "Observation"),
        result.getTypeList().stream().map(ElementDefinition.TypeRefComponent::getCode).toList());
    assertFalse(
        result.getTypeList().stream().anyMatch(ElementDefinition.TypeRefComponent::hasProfile));
    assertEquals(3, definition.getTypeFirstRep().getProfileList().size());
    assertEquals(1, result.getMin());
    assertEquals("subject", result.getSliceName());
  }

  @Test
  void unresolvedProfileLeavesTheOriginalChecksIntact() {
    var context = mock(IWorkerContext.class);
    var validator = mock(RecursiveInstanceValidator.class);
    when(validator.getContext()).thenReturn(context);
    var definition = definition("missing", "other");
    when(validator.typeOnlyDefinition(definition)).thenCallRealMethod();

    assertSame(definition, validator.typeOnlyDefinition(definition));
  }

  private static ElementDefinition definition(String... profiles) {
    var definition =
        new ElementDefinition()
            .setPath("Parameters.parameter.resource")
            .setSliceName("subject")
            .setMin(1);
    var type = definition.addType().setCode("Resource");
    for (var profile : profiles) {
      type.addProfile(profile);
    }
    return definition;
  }

  private static void profile(IWorkerContext context, String canonical, String type) {
    doReturn(new StructureDefinition().setType(type))
        .when(context)
        .fetchResource(StructureDefinition.class, canonical, VersionResolutionRules.defaultRule());
  }
}
