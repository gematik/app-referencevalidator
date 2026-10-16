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

import de.gematik.refv.lib.exceptions.ParsingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FhirResourceTest {

  @TempDir Path tempDir;

  @DisplayName(
      "Given JSON content, when creating a resource, then the content is readable from the stream")
  @Test
  void expectJsonFromContentReadable() throws Exception {
    final var resource = FhirResource.fromJson("{\"resourceType\":\"Patient\"}");
    Assertions.assertInstanceOf(JsonFhirResource.class, resource);
    try (var is = resource.inputStream()) {
      final String read = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      Assertions.assertTrue(read.contains("Patient"));
    }
  }

  @DisplayName(
      "Given XML content, when creating a resource, then the content is readable from the stream")
  @Test
  void expectXmlFromContentReadable() throws Exception {
    final var resource = FhirResource.fromXml("<Patient xmlns=\"http://hl7.org/fhir\"/>");
    Assertions.assertInstanceOf(XmlFhirResource.class, resource);
    try (var is = resource.inputStream()) {
      final String read = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      Assertions.assertTrue(read.contains("Patient"));
    }
  }

  @DisplayName("Given a JSON file path, when creating a resource, then the content is readable")
  @Test
  void expectJsonFromPathReadable() throws Exception {
    final var file = tempDir.resolve("patient.json");
    Files.writeString(file, "{\"resourceType\":\"Patient\"}");
    final var resource = FhirResource.fromJson(file);
    Assertions.assertInstanceOf(JsonFhirResource.class, resource);
    Assertions.assertNotNull(resource.inputStream());
  }

  @DisplayName("Given an XML file path, when creating a resource, then the content is readable")
  @Test
  void expectXmlFromPathReadable() throws Exception {
    final var file = tempDir.resolve("patient.xml");
    Files.writeString(file, "<Patient xmlns=\"http://hl7.org/fhir\"/>");
    final var resource = FhirResource.fromXml(file);
    Assertions.assertInstanceOf(XmlFhirResource.class, resource);
  }

  @DisplayName(
      "Given a missing file path, when creating a JSON resource, then a ParsingException is thrown")
  @Test
  void expectJsonFromMissingPathThrows() {
    final var missingPath = tempDir.resolve("missing.json");
    Assertions.assertThrows(ParsingException.class, () -> FhirResource.fromJson(missingPath));
  }

  @DisplayName(
      "Given a missing file path, when creating an XML resource, then a ParsingException is thrown")
  @Test
  void expectXmlFromMissingPathThrows() {
    final var missingPath = tempDir.resolve("missing.xml");
    Assertions.assertThrows(ParsingException.class, () -> FhirResource.fromXml(missingPath));
  }

  @DisplayName(
      "Given null content, when creating a resource, then a NullPointerException is thrown")
  @Test
  void expectNullContentThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> FhirResource.fromJson((String) null));
    Assertions.assertThrows(NullPointerException.class, () -> FhirResource.fromXml((String) null));
  }
}
