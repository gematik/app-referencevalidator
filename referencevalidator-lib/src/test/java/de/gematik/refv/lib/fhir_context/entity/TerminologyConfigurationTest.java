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

class TerminologyConfigurationTest {

  @DisplayName(
      "Given the offline mode, when created, then remote loading is disallowed and no server is set")
  @Test
  void expectOfflineModeValues() {
    final var config = TerminologyConfiguration.offlineMode();
    Assertions.assertEquals(
        TerminologyConfiguration.RemoteLoadingPolicy.DISALLOWED, config.remoteLoadingPolicy());
    Assertions.assertNull(config.serverUri());
  }

  @DisplayName("Given the default configuration, when created, then it matches the offline mode")
  @Test
  void expectDefaultIsOffline() {
    Assertions.assertEquals(
        TerminologyConfiguration.RemoteLoadingPolicy.DISALLOWED,
        TerminologyConfiguration.defaultConfiguration().remoteLoadingPolicy());
  }

  @DisplayName(
      "Given ALLOWED without a server URI, when created, then a NullPointerException is thrown")
  @Test
  void expectAllowedWithoutServerUriThrows() {
    final var policy = TerminologyConfiguration.RemoteLoadingPolicy.ALLOWED;
    Assertions.assertThrows(
        NullPointerException.class, () -> new TerminologyConfiguration(policy, null, null));
  }

  @DisplayName("Given ALLOWED with a server URI, when created, then the configuration is valid")
  @Test
  void expectAllowedWithServerUriSucceeds() {
    final var config =
        Assertions.assertDoesNotThrow(
            () ->
                new TerminologyConfiguration(
                    TerminologyConfiguration.RemoteLoadingPolicy.ALLOWED,
                    URI.create("https://tx.fhir.org")));
    Assertions.assertEquals(URI.create("https://tx.fhir.org"), config.serverUri());
    Assertions.assertNull(config.cachePath());
  }

  @DisplayName("Given a null policy, when created, then a NullPointerException is thrown")
  @Test
  void expectNullPolicyThrows() {
    final var serverUri = URI.create("https://tx.fhir.org");
    Assertions.assertThrows(
        NullPointerException.class, () -> new TerminologyConfiguration(null, serverUri, null));
  }
}
