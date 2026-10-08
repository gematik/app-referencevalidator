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
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.R4Version;
import de.gematik.refv.lib.fhir_context.entity.R5Version;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_40_N;
import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.model.core.Resource;
import org.hl7.fhir.model.core.formats.JsonParser;
import org.hl7.fhir.model.utilities.formats.OutputStyle;
import org.hl7.fhir.validation.ValidatorUtils;
import org.jspecify.annotations.NonNull;

/** Utility class to perform Parsing of FHIR Structures using the HL7 Json Parser. */
final class ResourceParser {
  private ResourceParser() {}

  /**
   * Parse a JSON String as FHIR R4 Resource.
   *
   * @param json a string containing a FHIR Resource to be parsed as a FHIR R4 one
   * @return an instance of {@link org.hl7.fhir.r4.model.Resource}
   * @throws ParsingException in case of parsing errors
   */
  static org.hl7.fhir.r4.model.Resource parseResourceR4(String json) throws ParsingException {
    return parseResourceR4(json.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Parse a JSON String as FHIR R4 Resource.
   *
   * @param json a byte array containing a FHIR Resource to be parsed as a FHIR R4 one
   * @return an instance of {@link org.hl7.fhir.r4.model.Resource}
   * @throws ParsingException in case of parsing errors
   */
  static org.hl7.fhir.r4.model.Resource parseResourceR4(byte[] json) throws ParsingException {
    var parser =
        new org.hl7.fhir.r4.formats.JsonParser()
            .setOutputStyle(org.hl7.fhir.r4.formats.IParser.OutputStyle.PRETTY);
    try {
      return parser.parse(json);
    } catch (IOException e) {
      throw new ParsingException("Failed to parse resource R4", e);
    }
  }

  /**
   * Parse a JSON String as FHIR R5 Resource.
   *
   * @param json a byte array containing a FHIR Resource to be parsed as a FHIR R5 one
   * @return an instance of {@link Resource}
   * @throws ParsingException in case of parsing errors
   */
  static Resource parseR4ResourceAsR5(String json) throws ParsingException {
    var resourceR4 = parseResourceR4(json);
    return VersionConvertorFactory_40_N.convertResource(resourceR4);
  }

  /**
   * Parse a JSON String as FHIR R5 Resource.
   *
   * @param json a byte array containing a FHIR Resource to be parsed as a FHIR R5 one
   * @return an instance of {@link Resource}
   * @throws ParsingException in case of parsing errors
   */
  static Resource parseR4ResourceAsR5(byte[] json) throws ParsingException {
    var resourceR4 = parseResourceR4(json);
    return VersionConvertorFactory_40_N.convertResource(resourceR4);
  }

  /**
   * Parse a JSON String as FHIR R4 Resource.
   *
   * @param json a string containing a FHIR Resource to be parsed as a FHIR R4 one
   * @return an instance of {@link org.hl7.fhir.r4.model.Resource}
   * @throws ParsingException in case of parsing errors
   */
  static Resource parseResourceR5(String json) throws ParsingException {
    return parseResourceR5(json.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Parse a JSON String as FHIR R4 Resource.
   *
   * @param json a byte array containing a FHIR Resource to be parsed as a FHIR R4 one
   * @return an instance of {@link org.hl7.fhir.r4.model.Resource}
   * @throws ParsingException in case of parsing errors
   */
  static Resource parseResourceR5(byte[] json) throws ParsingException {
    var parser = new JsonParser(ModelContext.minimalContext()).setOutputStyle(OutputStyle.PRETTY);
    try {
      return parser.parse(json);
    } catch (Exception e) {
      throw new ParsingException("Failed to parse resource R5", e);
    }
  }

  /**
   * Parses the input stream according to the FHIR Version specified in the {@link
   * ContextConfiguration} configuration.
   *
   * @param inputStream the stream containing the FHIR content to be parsed
   * @param fhirVersion version of FHIR to use for parsing
   * @return an instance of {@link Resource}
   * @throws IOException in case of I/O Errors
   */
  static Resource parseJsonToResource(@NonNull InputStream inputStream, @NonNull String fhirVersion)
      throws IOException {
    final var loader =
        ValidatorUtils.loaderForVersion(
            ModelContext.minimalContext(), Objects.requireNonNull(fhirVersion));
    return loader.loadResource(Objects.requireNonNull(inputStream), true);
  }

  /**
   * Parses the input stream according to the FHIR Version specified in the {@link
   * ContextConfiguration} configuration.
   *
   * @param inputStream the stream containing the FHIR content to be parsed
   * @param fhirVersion version of FHIR to use for parsing
   * @return an instance of {@link Resource}
   * @throws IOException in case of I/O Errors
   */
  static Resource parseXmlToResource(@NonNull InputStream inputStream, @NonNull String fhirVersion)
      throws IOException {
    final var loader =
        ValidatorUtils.loaderForVersion(
            ModelContext.minimalContext(), Objects.requireNonNull(fhirVersion));
    return loader.loadResource(Objects.requireNonNull(inputStream), false);
  }

  static Resource parseAndConvert(byte[] content, FhirRelease version) {

    switch (version) {
      case R4Version _ -> {
        return parseR4ResourceAsR5(content);
      }

      case R5Version _ -> {
        return parseResourceR5(content);
      }

      default -> throw new IllegalStateException();
    }
  }
}
