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
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Defines the Configuration for accessing a terminology server for expanding value sets.
 *
 * @param remoteLoadingPolicy defines it a package could be retrieved from a remote server
 * @param serverUri defines the URI of a remote terminology server.
 * @param cachePath optional caller-managed path for the terminology cache; null requests a
 *     context-owned temporary cache
 */
public record TerminologyConfiguration(
    @NonNull RemoteLoadingPolicy remoteLoadingPolicy,
    @Nullable URI serverUri,
    @Nullable Path cachePath) {

  public enum RemoteLoadingPolicy {
    ALLOWED,
    DISALLOWED
  }

  public TerminologyConfiguration {
    Objects.requireNonNull(
        remoteLoadingPolicy, "The 'remoteDownloadPolicy' parameter must be defined");
    if (remoteLoadingPolicy.equals(RemoteLoadingPolicy.ALLOWED)) {
      Objects.requireNonNull(
          serverUri,
          "The 'serverUri' must be defined when the 'remoteDownloadPolicy' is set to 'ALLOWED'");
    }
  }

  public TerminologyConfiguration(
      @NonNull RemoteLoadingPolicy remoteLoadingPolicy, @Nullable URI terminologyServerUri) {
    this(remoteLoadingPolicy, terminologyServerUri, null);
  }

  public static TerminologyConfiguration offlineMode() {
    return new TerminologyConfiguration(RemoteLoadingPolicy.DISALLOWED, null, null);
  }

  public static TerminologyConfiguration defaultConfiguration() {
    return offlineMode();
  }
}
