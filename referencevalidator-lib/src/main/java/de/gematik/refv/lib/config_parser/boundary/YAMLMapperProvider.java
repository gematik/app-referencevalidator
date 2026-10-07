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
package de.gematik.refv.lib.config_parser.boundary;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Utility Class to define a YAML Mapper for loading the configuration of Validation Modules. */
public final class YAMLMapperProvider {

  private static final YAMLMapper MAPPER =
      YAMLMapper.builder()
          .addModule(new JavaTimeModule())
          // Tell Jackson to write/read dates as ISO-8601 strings ("2025-10-01"), not arrays
          // Jackson 3:
          // .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          // Fail if there is a typo in the YAML that does not match the configuration
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          // add indentation
          .enable(SerializationFeature.INDENT_OUTPUT)
          // add pretty printing
          .defaultPrettyPrinter(new DefaultPrettyPrinter())
          .build();

  private YAMLMapperProvider() {}

  /** Returns the current {@link YAMLMapper} for parsing YAML files. */
  public static YAMLMapper getMapper() {
    return MAPPER;
  }
}
