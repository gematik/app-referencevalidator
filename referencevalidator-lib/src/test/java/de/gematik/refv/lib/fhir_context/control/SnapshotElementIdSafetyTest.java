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

import de.gematik.refv.lib.exceptions.SnapshotGenerationException;
import java.util.List;
import org.hl7.fhir.model.core.ElementDefinition;
import org.hl7.fhir.model.core.StructureDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SnapshotElementIdSafetyTest {

  @Test
  void duplicateElementIdsFailWithStructureDefinitionIdentity() {
    var definition = new StructureDefinition();
    definition.setUrl("http://example.org/StructureDefinition/ObservationProfile");
    definition.setVersion("1.3.2");
    var elements =
        List.of(
            element("Observation.dataAbsentReason", "Observation.dataAbsentReason"),
            element("Observation.dataAbsentReason", "Observation.dataAbsentReason"));

    var exception =
        Assertions.assertThrows(
            SnapshotGenerationException.class,
            () ->
                DefaultSnapshotGenerationContext.assertUniqueElementIds(
                    definition, elements, "snapshot"));

    Assertions.assertTrue(exception.getMessage().contains("ObservationProfile|1.3.2"));
    Assertions.assertTrue(exception.getMessage().contains("Observation.dataAbsentReason"));
  }

  @Test
  void uniqueElementIdsPass() {
    var definition = new StructureDefinition();
    definition.setUrl("http://example.org/StructureDefinition/ObservationProfile");
    var elements =
        List.of(
            element("Observation", "Observation"),
            element("Observation.dataAbsentReason", "Observation.dataAbsentReason"));

    Assertions.assertDoesNotThrow(
        () ->
            DefaultSnapshotGenerationContext.assertUniqueElementIds(
                definition, elements, "differential"));
  }

  private ElementDefinition element(String id, String path) {
    var element = new ElementDefinition();
    element.setId(id);
    element.setPath(path);
    return element;
  }
}
