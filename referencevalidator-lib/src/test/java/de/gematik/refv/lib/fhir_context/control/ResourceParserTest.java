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

import de.gematik.refv.lib.exceptions.ParsingException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResourceParserTest {

  private static final String PATIENT_JSON = "{\"resourceType\":\"Patient\",\"id\":\"p1\"}";

  @DisplayName("Given a valid R4 JSON, when parsing as R4, then a resource is returned")
  @Test
  void expectParseResourceR4Works() {
    final var resource =
        Assertions.assertDoesNotThrow(() -> ResourceParser.parseResourceR4(PATIENT_JSON));
    Assertions.assertNotNull(resource);
  }

  @DisplayName("Given an invalid R4 JSON, when parsing as R4, then a ParsingException is thrown")
  @Test
  void expectParseResourceR4InvalidThrows() {
    Assertions.assertThrows(
        ParsingException.class, () -> ResourceParser.parseResourceR4("{invalid json"));
  }

  @DisplayName("Given a valid R4 JSON, when parsing as R5, then a converted resource is returned")
  @Test
  void expectParseR4ResourceAsR5Works() {
    final var resource =
        Assertions.assertDoesNotThrow(() -> ResourceParser.parseR4ResourceAsR5(PATIENT_JSON));
    Assertions.assertNotNull(resource);
  }

  @DisplayName("Given a valid R5 JSON, when parsing as R5, then a resource is returned")
  @Test
  void expectParseResourceR5Works() {
    final var resource =
        Assertions.assertDoesNotThrow(() -> ResourceParser.parseResourceR5(PATIENT_JSON));
    Assertions.assertNotNull(resource);
  }

  @DisplayName("Given an invalid R5 JSON, when parsing as R5, then a ParsingException is thrown")
  @Test
  void expectParseResourceR5InvalidThrows() {
    Assertions.assertThrows(
        ParsingException.class, () -> ResourceParser.parseResourceR5("{invalid"));
  }

  @DisplayName(
      "Given a JSON stream and a version, when parsing to resource, then a resource is returned")
  @Test
  void expectParseJsonToResourceWorks() {
    final var stream = new ByteArrayInputStream(PATIENT_JSON.getBytes(StandardCharsets.UTF_8));
    final var resource =
        Assertions.assertDoesNotThrow(() -> ResourceParser.parseJsonToResource(stream, "R4"));
    Assertions.assertNotNull(resource);
  }

  @DisplayName(
      "Given an XML stream and a version, when parsing to resource, then a resource is returned")
  @Test
  void expectParseXmlToResourceWorks() {
    final String xml = "<Patient xmlns=\"http://hl7.org/fhir\"><id value=\"p1\"/></Patient>";
    final var stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    final var resource =
        Assertions.assertDoesNotThrow(() -> ResourceParser.parseXmlToResource(stream, "R4"));
    Assertions.assertNotNull(resource);
  }

  @DisplayName("Given R4 content, when parseAndConvert with R4, then a resource is returned")
  @Test
  void expectParseAndConvertR4() {
    final var resource =
        Assertions.assertDoesNotThrow(
            () ->
                ResourceParser.parseAndConvert(
                    PATIENT_JSON.getBytes(StandardCharsets.UTF_8), FhirRelease.asR4()));
    Assertions.assertNotNull(resource);
  }

  @DisplayName("Given R5 content, when parseAndConvert with R5, then a resource is returned")
  @Test
  void expectParseAndConvertR5() {
    final var resource =
        Assertions.assertDoesNotThrow(
            () ->
                ResourceParser.parseAndConvert(
                    PATIENT_JSON.getBytes(StandardCharsets.UTF_8), FhirRelease.asR5()));
    Assertions.assertNotNull(resource);
  }
}
