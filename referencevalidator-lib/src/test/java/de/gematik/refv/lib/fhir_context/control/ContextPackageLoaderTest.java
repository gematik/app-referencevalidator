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
import org.hl7.fhir.model.core.StructureDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContextPackageLoaderTest {

  @DisplayName(
      "Given a loader, when extracting the profile prefix from an empty set, then a SnapshotGenerationException is thrown")
  @Test
  void expectExtractProfilePrefixFromEmptySetThrows() {
    final var noDefinitions = List.<StructureDefinition>of();
    Assertions.assertThrows(
        SnapshotGenerationException.class,
        () -> ContextPackageLoader.extractProfilePrefixFromStructureDefinition(noDefinitions));
  }

  @DisplayName(
      "Given a structure definition, when extracting the profile prefix, then the parent URL is returned")
  @Test
  void expectExtractProfilePrefixReturnsParent() {
    final var sd = new StructureDefinition();
    sd.setUrl("http://example.org/fhir/StructureDefinition/MyProfile");
    final var prefix =
        ContextPackageLoader.extractProfilePrefixFromStructureDefinition(List.of(sd));
    Assertions.assertEquals("http://example.org/fhir/StructureDefinition/", prefix);
  }
}
