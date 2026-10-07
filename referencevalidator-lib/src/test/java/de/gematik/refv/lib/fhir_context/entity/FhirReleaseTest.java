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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FhirReleaseTest {

  @DisplayName(
      "Given a supported version alias, when resolving, then the matching release is returned")
  @ParameterizedTest
  @ValueSource(strings = {"R4", "4.0", "4.0.1", "R5", "5.0", "5.0.0"})
  void expectOfVersionResolvesSupportedAliases(String version) {
    // Given a supported version string
    // When resolving the release
    final var release = Assertions.assertDoesNotThrow(() -> FhirRelease.ofVersion(version));
    // Then a release is returned
    Assertions.assertNotNull(release);
  }

  @DisplayName("Given the R4 aliases, when resolving, then an R4Version is returned")
  @ParameterizedTest
  @ValueSource(strings = {"R4", "4.0", "4.0.1"})
  void expectOfVersionR4ReturnsR4(String version) {
    Assertions.assertEquals(FhirRelease.asR4(), FhirRelease.ofVersion(version));
    Assertions.assertEquals("R4", FhirRelease.ofVersion(version).alias());
    Assertions.assertEquals(4, FhirRelease.ofVersion(version).major());
  }

  @DisplayName("Given the R5 aliases, when resolving, then an R5Version is returned")
  @ParameterizedTest
  @ValueSource(strings = {"R5", "5.0", "5.0.0"})
  void expectOfVersionR5ReturnsR5(String version) {
    Assertions.assertEquals(FhirRelease.asR5(), FhirRelease.ofVersion(version));
    Assertions.assertEquals(5, FhirRelease.ofVersion(version).major());
  }

  @DisplayName("Given an unknown version, when resolving, then it falls back to R4")
  @Test
  void expectOfVersionUnknownFallsBackToR4() {
    Assertions.assertEquals(FhirRelease.asR4(), FhirRelease.ofVersion("R3"));
    Assertions.assertEquals(FhirRelease.asR4(), FhirRelease.ofVersion("something-else"));
  }

  @DisplayName("Given a null or blank version, when resolving, then an exception is thrown")
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void expectOfVersionNullOrBlankThrows(String version) {
    Assertions.assertThrows(IllegalArgumentException.class, () -> FhirRelease.ofVersion(version));
  }

  @DisplayName("Given the releases, when reading core package id, then name and version are joined")
  @Test
  void expectCorePackageIdIsCorrect() {
    Assertions.assertEquals("hl7.fhir.r4.core#4.0.1", FhirRelease.asR4().corePackageId());
    Assertions.assertEquals("hl7.fhir.r5.core#5.0.0", FhirRelease.asR5().corePackageId());
  }

  @DisplayName(
      "Given the releases, when reading metadata, then canonical and packages are consistent")
  @Test
  void expectReleaseMetadataIsConsistent() {
    Assertions.assertEquals("4.0", FhirRelease.asR4().canonical());
    Assertions.assertEquals("hl7.fhir.r4.core", FhirRelease.asR4().corePackageName());
    Assertions.assertEquals("4.0.1", FhirRelease.asR4().corePackageVersion());
    Assertions.assertFalse(FhirRelease.asR4().packages().isEmpty());

    Assertions.assertEquals("5.0", FhirRelease.asR5().canonical());
    Assertions.assertFalse(FhirRelease.asR5().packages().isEmpty());
  }

  @DisplayName("Given a release, when serializing with Jackson, then the alias is written")
  @Test
  void expectSerializerWritesAlias() throws Exception {
    final var mapper = new ObjectMapper();
    Assertions.assertEquals("\"R4\"", mapper.writeValueAsString(FhirRelease.asR4()));
    Assertions.assertEquals("\"R5\"", mapper.writeValueAsString(FhirRelease.asR5()));
  }

  @DisplayName("Given a JSON scalar, when deserializing with Jackson, then the release is resolved")
  @Test
  void expectDeserializerReadsAlias() throws Exception {
    final var mapper = new ObjectMapper();
    Assertions.assertEquals(FhirRelease.asR4(), mapper.readValue("\"R4\"", FhirRelease.class));
    Assertions.assertEquals(FhirRelease.asR5(), mapper.readValue("\"5.0.0\"", FhirRelease.class));
  }
}
