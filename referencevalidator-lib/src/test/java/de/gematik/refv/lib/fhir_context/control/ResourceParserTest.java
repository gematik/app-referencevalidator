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

import de.gematik.refv.lib.config_parser.boundary.JSONMapperProvider;
import de.gematik.refv.lib.exceptions.ParsingException;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceParserTest {

  private static final String PATIENT_JSON = "{\"resourceType\":\"Patient\",\"id\":\"p1\"}";

  @ParameterizedTest
  @ValueSource(strings = {"4.0.1", "5.0.0"})
  void snapshotSerializationPreservesTheDeclaredRelease(String version) throws Exception {
    var release = FhirRelease.ofVersion(version);
    var versionSpecificConstraint =
        version.equals("4.0.1")
            ? "\"constraint\":[{\"key\":\"test\",\"severity\":\"error\",\"human\":\"Test\",\"expression\":\"true\",\"xpath\":\"f:Patient\"}]"
            : "\"binding\":{\"strength\":\"required\",\"valueSet\":\"http://example.org/ValueSet/base\",\"additional\":[{\"purpose\":\"maximum\",\"valueSet\":\"http://example.org/ValueSet/extra\"}]}";
    var content =
        """
        {"resourceType":"StructureDefinition","id":"test","url":"http://example.org/test",
         "version":"1.0.0","name":"Test","status":"active","fhirVersion":"%s",
         "kind":"resource","abstract":false,"type":"Patient",
         "baseDefinition":"http://hl7.org/fhir/StructureDefinition/Patient","derivation":"constraint",
         "snapshot":{"element":[{"id":"Patient","path":"Patient","min":0,"max":"*"}]},
         "differential":{"element":[{"id":"Patient","path":"Patient",%s}]}}
        """
            .formatted(version, versionSpecificConstraint);

    try (var input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
      var parsed = ResourceParser.parseJsonToResource(input, version);
      var output = ResourceParser.composeForRelease(parsed, release);
      var json = JSONMapperProvider.getMapper().readTree(output);
      Assertions.assertEquals(version, json.path("fhirVersion").asText());
      Assertions.assertEquals(
          "Patient", json.path("snapshot").path("element").get(0).path("path").asText());
      var element = json.path("differential").path("element").get(0);
      if (version.equals("4.0.1")) {
        var nativeResource = new org.hl7.fhir.r4.formats.JsonParser().parse(output);
        Assertions.assertInstanceOf(
            org.hl7.fhir.r4.model.StructureDefinition.class, nativeResource);
        Assertions.assertEquals(
            "f:Patient", element.path("constraint").get(0).path("xpath").asText());
      } else {
        var nativeResource = new org.hl7.fhir.r5.formats.JsonParser().parse(output);
        Assertions.assertInstanceOf(
            org.hl7.fhir.r5.model.StructureDefinition.class, nativeResource);
        Assertions.assertEquals(
            "http://example.org/ValueSet/extra",
            element.path("binding").path("additional").get(0).path("valueSet").asText());
      }
    }
  }

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
