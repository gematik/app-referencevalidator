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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import java.util.List;
import org.hl7.fhir.model.core.ElementDefinition;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.services.elementmodel.Element;
import org.hl7.fhir.services.elementmodel.Property;
import org.junit.jupiter.api.Test;

class EmbeddedResourceExtractorTest {
  @Test
  void extractsNestedContainersAndPartsInDocumentOrder() {
    var bundle = element("Bundle", "Bundle");
    var parameters = element("resource", "Parameters");
    add(add(bundle, element("entry", "BackboneElement")), parameters);
    var parameter = add(parameters, element("parameter", "BackboneElement"));
    var part = add(parameter, element("part", "BackboneElement"));
    var nestedPart = add(part, element("part", "BackboneElement"));
    var patient = add(nestedPart, element("resource", "Patient"));
    add(patient, element("contained", "Observation"));
    add(add(bundle, element("entry", "BackboneElement")), element("resource", "Patient"));

    var extracted = EmbeddedResourceExtractor.extract(bundle);

    assertEquals(
        List.of(
            "Bundle.entry[0].resource",
            "Bundle.entry[0].resource.parameter[0].part[0].part[0].resource",
            "Bundle.entry[1].resource"),
        extracted.stream().map(EmbeddedResourceExtractor.EmbeddedResource::path).toList());
    assertEquals(
        List.of("Parameters", "Patient", "Patient"),
        extracted.stream().map(child -> child.resource().fhirType()).toList());
  }

  @Test
  void emptyContainersAndResourceLessComponentsHaveNoChildren() {
    assertTrue(EmbeddedResourceExtractor.extract(element("Bundle", "Bundle")).isEmpty());
    var parameters = element("Parameters", "Parameters");
    add(parameters, element("parameter", "BackboneElement"));
    assertTrue(EmbeddedResourceExtractor.extract(parameters).isEmpty());
    assertTrue(EmbeddedResourceExtractor.extract(element("Patient", "Patient")).isEmpty());
  }

  @Test
  void preservesAllDistinctVersionedProfilesWithoutInheritingParentProfiles() {
    var patient = element("resource", "Patient");
    var child =
        new EmbeddedResourceExtractor.EmbeddedResource(patient, "Parameters.parameter[0].resource");
    assertEquals(List.of(), child.profiles());
    var meta = add(patient, element("meta", "Meta"));
    for (var value :
        List.of(
            "http://example.org/one|1.0",
            "http://example.org/two",
            "http://example.org/one|1.0",
            " ")) {
      var profile = add(meta, element("profile", "canonical"));
      profile.setValue(value);
    }
    assertEquals(
        List.of("http://example.org/one|1.0", "http://example.org/two"),
        child.profiles().stream().map(ProfileCanonical::value).toList());
  }

  private static Element element(String name, String type) {
    var definition = new ElementDefinition().setPath(name);
    // Child lookup needs definition metadata but does not access a worker context.
    var property = new Property(null, definition, new StructureDefinition(), null, null);
    return new Element(name, property, type, null);
  }

  private static Element add(Element parent, Element child) {
    parent.getChildList().add(child);
    return child;
  }
}
