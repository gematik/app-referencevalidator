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

import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PackageDownloadConfigurationTest {

  @DisplayName("Given the offline mode, when created, then remote downloads are disallowed")
  @Test
  void expectOfflineModeDisallowsRemote() {
    final var config = PackageDownloadConfiguration.offlineMode();
    Assertions.assertEquals(
        PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED,
        config.remoteDownloadPolicy());
    Assertions.assertNotNull(config.cachePath());
  }

  @DisplayName("Given the default configuration, when created, then it matches the offline mode")
  @Test
  void expectDefaultIsOffline() {
    Assertions.assertEquals(
        PackageDownloadConfiguration.RemoteDownloadPolicy.DISALLOWED,
        PackageDownloadConfiguration.defaultConfiguration().remoteDownloadPolicy());
  }

  @DisplayName(
      "Given the snapshot generation mode, when created, then a persistent cache with remote access is used")
  @Test
  void expectSnapshotGenerationModeAllowsRemoteAndPersistentCache() {
    final var config =
        Assertions.assertDoesNotThrow(PackageDownloadConfiguration::snapshotGenerationMode);
    Assertions.assertEquals(
        PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED, config.remoteDownloadPolicy());
    Assertions.assertEquals(
        Path.of(System.getProperty("user.home"), ".fhir", "snapshot-packages"), config.cachePath());
  }

  @DisplayName("Given a null policy, when created, then a NullPointerException is thrown")
  @Test
  void expectNullPolicyThrows() {
    final var cachePath = Path.of("/tmp");
    Assertions.assertThrows(
        NullPointerException.class, () -> new PackageDownloadConfiguration(null, cachePath));
  }

  @DisplayName("Given a null cache path, when created, then a NullPointerException is thrown")
  @Test
  void expectNullCachePathThrows() {
    Assertions.assertThrows(
        NullPointerException.class,
        () ->
            new PackageDownloadConfiguration(
                PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED, null));
  }

  @DisplayName("Given only a policy, when created, then the default FHIR cache path is used")
  @Test
  void expectSingleArgumentConstructorUsesDefaultCache() {
    final var config =
        new PackageDownloadConfiguration(PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED);
    Assertions.assertTrue(config.cachePath().toString().contains(".fhir"));
  }
}
