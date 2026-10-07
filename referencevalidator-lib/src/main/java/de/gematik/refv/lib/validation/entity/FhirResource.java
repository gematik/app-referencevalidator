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
package de.gematik.refv.lib.validation.entity;

import java.io.InputStream;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;

/**
 * Immutable representation of a FHIR resource. Completely encapsulates the underlying HL7
 * representation.
 */
public sealed interface FhirResource permits JsonFhirResource, XmlFhirResource {
  /**
   * Exposes the Resource as an Input Stream.
   *
   * @return a valid {@link InputStream}
   */
  InputStream inputStream();

  /** Factory method for creating a JSON resource from a file path. */
  static FhirResource fromJson(@NonNull Path filePath) {
    return JsonFhirResource.fromPath(filePath);
  }

  /** Factory method for creating a JSON resource from a string containing the content. */
  static FhirResource fromJson(@NonNull String jsonContent) {
    return JsonFhirResource.fromContent(jsonContent);
  }

  /** Factory method for creating a XML resource from a file path. */
  static FhirResource fromXml(@NonNull Path filePath) {
    return XmlFhirResource.fromPath(filePath);
  }

  /** Factory method for creating a XML resource from a string containing the content. */
  static FhirResource fromXml(@NonNull String xmlContent) {
    return XmlFhirResource.fromContent(xmlContent);
  }
}
