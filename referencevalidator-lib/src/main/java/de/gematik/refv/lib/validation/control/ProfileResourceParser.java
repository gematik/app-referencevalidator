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
package de.gematik.refv.lib.validation.control;

import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.validation.ValidatorUtils;
import org.jspecify.annotations.NonNull;

/** Utility class to perform Parsing of FHIR Structures using the HL7 Json Parser. */
final class ProfileResourceParser {

  private ProfileResourceParser() {}

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
    final var loader = ValidatorUtils.loaderForVersion(Objects.requireNonNull(fhirVersion));
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
    final var loader = ValidatorUtils.loaderForVersion(Objects.requireNonNull(fhirVersion));
    return loader.loadResource(Objects.requireNonNull(inputStream), false);
  }
}
