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

import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.services.elementmodel.Element;

/** Extracts Resources (children) from FHIR Containers such as Bundle or Parameters. */
final class EmbeddedResourceExtractor {
  private EmbeddedResourceExtractor() {}

  static List<EmbeddedResource> extract(Element root) {
    var resources = new ArrayList<EmbeddedResource>();
    extract(root, root.fhirType(), resources);
    return resources;
  }

  private static void extract(Element resource, String path, List<EmbeddedResource> resources) {
    switch (resource.fhirType()) {
      case "Bundle" -> extractComponents(resource, "entry", path, resources);
      case "Parameters" -> extractComponents(resource, "parameter", path, resources);
      default -> {
        // Contained resources need their owning resource for reference resolution.
      }
    }
  }

  private static void extractComponents(
      Element parent, String name, String path, List<EmbeddedResource> resources) {
    if (!parent.hasChildren()) {
      return;
    }
    var components = parent.getChildrenByName(name);
    for (int index = 0; index < components.size(); index++) {
      var component = components.get(index);
      var componentPath = path + "." + name + "[" + index + "]";
      var child = component.getNamedChild("resource", false);
      if (child != null) {
        var childPath = componentPath + ".resource";
        resources.add(new EmbeddedResource(child, childPath));
        extract(child, childPath, resources);
      }
      if (!"entry".equals(name)) {
        extractComponents(component, "part", componentPath, resources);
      }
    }
  }

  record EmbeddedResource(Element resource, String path) {
    List<ProfileCanonical> profiles() {
      var meta = resource.getNamedChild("meta", false);
      return meta == null || !meta.hasChildren()
          ? List.of()
          : meta.getChildrenByName("profile").stream()
              .map(Element::getValue)
              .filter(Objects::nonNull)
              .filter(value -> !value.isBlank())
              .map(ProfileCanonical::fromCanonical)
              .distinct()
              .toList();
    }
  }
}
