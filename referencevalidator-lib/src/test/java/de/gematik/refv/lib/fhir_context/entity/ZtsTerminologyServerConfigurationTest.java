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
package de.gematik.refv.lib.fhir_context.entity;

import java.net.URI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZtsTerminologyServerConfigurationTest {

  @DisplayName(
      "Given a base URI, when building the config, then all endpoints are derived correctly")
  @Test
  void expectFromUriDerivesEndpoints() {
    final var config = ZtsTerminologyServerConfiguration.fromUri(URI.create("https://example.org"));
    Assertions.assertEquals(URI.create("https://example.org"), config.terminologyServerUri());
    Assertions.assertEquals(
        URI.create("https://example.org/api/generate-token"), config.tokenEndpointUri());
    Assertions.assertEquals(
        URI.create("https://example.org/packages"), config.packageEndpointUri());
    Assertions.assertEquals(
        URI.create("https://example.org/packages/catalog"), config.catalogEndpointUri());
    Assertions.assertEquals(
        URI.create("https://example.org/tx/fhir"), config.expansionEndpointUri());
  }

  @DisplayName(
      "Given a base URI with a custom port, when building the config, then the port is preserved")
  @Test
  void expectFromUriWithPortPreservesPort() {
    final var config =
        ZtsTerminologyServerConfiguration.fromUri(URI.create("https://example.org:8443"));
    Assertions.assertEquals(
        URI.create("https://example.org:8443/tx/fhir"), config.expansionEndpointUri());
  }

  @DisplayName("Given a null URI, when building the config, then a NullPointerException is thrown")
  @Test
  void expectFromUriNullThrows() {
    Assertions.assertThrows(
        NullPointerException.class, () -> ZtsTerminologyServerConfiguration.fromUri(null));
  }

  @DisplayName("Given the default config, when created, then the bfarm terminology server is used")
  @Test
  void expectDefaultConfig() {
    final var config = ZtsTerminologyServerConfiguration.defaultConfig();
    Assertions.assertEquals(
        URI.create("https://terminologien.bfarm.de"), config.terminologyServerUri());
  }
}
