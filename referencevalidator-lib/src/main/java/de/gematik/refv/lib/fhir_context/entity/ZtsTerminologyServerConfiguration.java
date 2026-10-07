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
import java.util.Objects;
import jdk.jfr.Experimental;
import org.jspecify.annotations.NonNull;

/**
 * ZTS Terminology Server configuration.
 *
 * @param terminologyServerUri the terminology URI
 * @param tokenEndpointUri the URI representing the endpoint for retrieving a token
 * @param packageEndpointUri the URI representing the endpoint for retrieving a package
 * @param catalogEndpointUri the URI representing the endpoint for retrieving the catalog of
 *     packages
 */
@Experimental
public record ZtsTerminologyServerConfiguration(
    @NonNull URI terminologyServerUri,
    @NonNull URI tokenEndpointUri,
    @NonNull URI packageEndpointUri,
    @NonNull URI catalogEndpointUri,
    @NonNull URI expansionEndpointUri) {

  public static ZtsTerminologyServerConfiguration fromUri(@NonNull URI uri) {
    Objects.requireNonNull(uri, "The 'uri' must be defined");
    final String baseHost =
        String.format(
            "%s://%s",
            uri.getScheme(),
            uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort());
    final URI baseURI = URI.create(baseHost);
    return new ZtsTerminologyServerConfiguration(
        uri,
        baseURI.resolve("/api/generate-token"),
        baseURI.resolve("/packages"),
        baseURI.resolve("/packages/catalog"),
        baseURI.resolve("/tx/fhir"));
  }

  public static ZtsTerminologyServerConfiguration defaultConfig() {
    return ZtsTerminologyServerConfiguration.fromUri(URI.create("https://terminologien.bfarm.de"));
  }
}
