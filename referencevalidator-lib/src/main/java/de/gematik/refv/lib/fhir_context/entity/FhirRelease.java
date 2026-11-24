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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/** Represents a FHIR specification release version. */
@JsonDeserialize(using = FhirRelease.FhirReleaseDeserializer.class)
@JsonSerialize(using = FhirRelease.FhirReleaseSerializer.class)
public sealed interface FhirRelease permits R4Version, R5Version {

  /** The Alias for the version (e.g. R4 for 4.0.1, R5 for 5.0.0 */
  @NonNull String alias();

  /** The canonical version string (e.g., "4.0", "5.0"). */
  @NonNull String canonical();

  /** The core package name for this release. */
  @NonNull String corePackageName();

  /** The core package version for this release. */
  @NonNull String corePackageVersion();

  /** The major release version number. */
  int major();

  /** The list of packages that defines the specific release. */
  @NonNull List<String> packages();

  /** The core package name and version for this release, in the form <code>name#version</code>. */
  default String corePackageId() {
    return corePackageName() + "#" + corePackageVersion();
  }

  /**
   * Constructs a valid instance of {@link FhirRelease} from a given release version string.
   *
   * @param version the FHIR Version to be used (e.g. R4,R5 or 4.0.1,5.0.0)
   * @return a valid instance of {@link FhirRelease} or an exception in case of bad parameters
   */
  static FhirRelease ofVersion(String version) {
    if (Objects.isNull(version) || version.isBlank()) {
      throw new IllegalArgumentException("The release version must be defined");
    }

    return switch (version) {
      case "R5", "5.0", "5.0.0" -> FhirRelease.asR5();
      default -> FhirRelease.asR4();
    };
  }

  /**
   * Constructs a {@link FhirRelease} instance for FHIR R4.
   *
   * @return the FHIR R4 Version.
   */
  static FhirRelease asR4() {
    return new R4Version();
  }

  /**
   * Constructs a {@link FhirRelease} instance for FHIR R5.
   *
   * @return the FHIR R5 Version.
   */
  static FhirRelease asR5() {
    return new R5Version();
  }

  /**
   * Custom Jackson deserializer that maps a scalar release string (e.g. "R4") to the corresponding
   * {@link FhirRelease} implementation.
   */
  class FhirReleaseDeserializer extends JsonDeserializer<FhirRelease> {
    @Override
    public FhirRelease deserialize(@NonNull JsonParser p, DeserializationContext ignored)
        throws IOException {
      Objects.requireNonNull(p, "The JsonParser cannot be null");
      return FhirRelease.ofVersion(p.getValueAsString());
    }
  }

  class FhirReleaseSerializer extends JsonSerializer<FhirRelease> {
    @Override
    public void serialize(
        @NonNull FhirRelease value, @NonNull JsonGenerator gen, SerializerProvider ignored)
        throws IOException {
      Objects.requireNonNull(value, "The FhirRelease field cannot be null");
      Objects.requireNonNull(gen, "The JsonGenerator cannot be null");
      gen.writeString(value.alias());
    }
  }
}
