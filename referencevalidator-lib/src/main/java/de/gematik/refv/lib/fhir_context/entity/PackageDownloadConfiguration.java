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

import de.gematik.refv.lib.exceptions.InitializationException;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Defines the Configuration for downloading packages.
 *
 * @param remoteDownloadPolicy defines it a package could be retrieved from a remote server
 * @param cachePath defines a custom path for the caching/downloading of packages (default:
 *     ~/.fhir/packages)
 */
public record PackageDownloadConfiguration(
    @NonNull RemoteDownloadPolicy remoteDownloadPolicy, @NonNull Path cachePath) {

  public enum RemoteDownloadPolicy {
    ALLOWED,
    DISALLOWED
  }

  public PackageDownloadConfiguration {
    Objects.requireNonNull(
        remoteDownloadPolicy, "The 'remoteDownloadPolicy' parameter must be defined");
    cachePath =
        Objects.requireNonNull(cachePath, "The 'cachePath' parameter must be defined")
            .toAbsolutePath()
            .normalize();
  }

  public PackageDownloadConfiguration(@NonNull RemoteDownloadPolicy remoteLoadingPolicy) {
    this(remoteLoadingPolicy, Path.of(System.getProperty("user.home"), ".fhir", "packages"));
  }

  public static PackageDownloadConfiguration offlineMode() {
    return new PackageDownloadConfiguration(RemoteDownloadPolicy.DISALLOWED);
  }

  /**
   * Creates a Configuration that needs to be used specifically for the Snapshot Generation.
   *
   * <p>During the generation of snapshots, the original package resources are modified, thus the
   * original one should be checked in a temporary folder and then later on only the resulting new
   * package resources should be imported in the FHIR Context, for the validation operation. <br>
   *
   * <p>The Snapshot Generation operation requires access to online package registries, so missing
   * dependencies can be easily fetched.
   *
   * @return a new {@link PackageDownloadConfiguration} instance specific for snapshot generation
   *     <p>The package cache is persistent and remains available to later library invocations. The
   *     caller controls its retention and deletion like any other configured package cache.
   * @throws InitializationException retained for source compatibility
   */
  public static PackageDownloadConfiguration snapshotGenerationMode()
      throws InitializationException {
    final var packageCache = Path.of(System.getProperty("user.home"), ".fhir", "snapshot-packages");
    return new PackageDownloadConfiguration(RemoteDownloadPolicy.ALLOWED, packageCache);
  }

  public static PackageDownloadConfiguration defaultConfiguration() {
    return offlineMode();
  }
}
