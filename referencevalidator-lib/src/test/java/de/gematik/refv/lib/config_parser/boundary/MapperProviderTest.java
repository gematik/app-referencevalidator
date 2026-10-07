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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapperProviderTest {

  @DisplayName(
      "Given the JSON mapper provider, when getting the mapper, then it is a non-null singleton")
  @Test
  void expectJsonMapperIsSingleton() {
    Assertions.assertNotNull(JSONMapperProvider.getMapper());
    Assertions.assertSame(JSONMapperProvider.getMapper(), JSONMapperProvider.getMapper());
  }

  @DisplayName(
      "Given the YAML mapper provider, when getting the mapper, then it is a non-null singleton")
  @Test
  void expectYamlMapperIsSingleton() {
    Assertions.assertNotNull(YAMLMapperProvider.getMapper());
    Assertions.assertSame(YAMLMapperProvider.getMapper(), YAMLMapperProvider.getMapper());
  }

  @DisplayName("Given the JSON mapper, when serializing a map, then valid JSON is produced")
  @Test
  void expectJsonMapperSerializes() throws Exception {
    final var json = JSONMapperProvider.getMapper().writeValueAsString(java.util.Map.of("k", "v"));
    Assertions.assertTrue(json.contains("\"k\""));
  }

  @DisplayName("Given the YAML mapper, when serializing a map, then valid YAML is produced")
  @Test
  void expectYamlMapperSerializes() throws Exception {
    final var yaml = YAMLMapperProvider.getMapper().writeValueAsString(java.util.Map.of("k", "v"));
    Assertions.assertTrue(yaml.contains("k"));
  }
}
