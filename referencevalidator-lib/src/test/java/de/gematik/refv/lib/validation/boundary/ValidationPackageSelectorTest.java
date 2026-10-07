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
package de.gematik.refv.lib.validation.boundary;

import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.ValidationModuleIndex;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.valmodule.entity.ValidationModule;
import de.gematik.refv.valmodule.api.entity.PackageGroup;
import de.gematik.refv.valmodule.api.entity.PackageGroupReference;
import de.gematik.refv.valmodule.api.entity.ProfileDefinition;
import de.gematik.refv.valmodule.api.entity.ProfileFamily;
import de.gematik.refv.valmodule.api.entity.ProfileVersion;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidationPackageSelectorTest {

  private static final String BASE = "http://example.org/fhir/StructureDefinition";

  @DisplayName("Given a null module index, when created, then a NullPointerException is thrown")
  @Test
  void expectNullIndexThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new ValidationPackageSelector(null));
  }

  @DisplayName(
      "Given a resource with a supported profile, when selecting dependencies, then the package set is returned")
  @Test
  void expectPackagesSelectedForSupportedProfile() {
    final var index = indexWithRemotePackages("MyProfile");
    final var selector = new ValidationPackageSelector(index);
    final var resource =
        FhirResource.fromJson(
            "{\"resourceType\":\"Patient\",\"meta\":{\"profile\":[\""
                + BASE
                + "/MyProfile|1.0\"]}}");
    final var packageCategories =
        Assertions.assertDoesNotThrow(
            () ->
                selector.getPackagesForResource(
                    resource, FhirRelease.asR4(), ValidationOptions.defaultConfiguration()));
    Assertions.assertNotNull(packageCategories);
    Assertions.assertFalse(packageCategories.isEmpty());
  }

  @DisplayName(
      "Given a resource without a profile, when selecting dependencies, then an exception is thrown")
  @Test
  void expectMissingProfileThrows() {
    final var index = indexWithRemotePackages("MyProfile");
    final var selector = new ValidationPackageSelector(index);
    final var resource = FhirResource.fromJson("{\"resourceType\":\"Patient\"}");
    Assertions.assertThrows(
        Exception.class,
        () ->
            selector.getPackagesForResource(
                resource, FhirRelease.asR4(), ValidationOptions.defaultConfiguration()));
  }

  private static ValidationModuleIndex indexWithRemotePackages(String profileName) {
    // remote package coordinates (name#version) so no JAR lookup is needed
    final var groups = Map.of("g1", new PackageGroup(List.of("hl7.fhir.r4.core#4.0.1"), null));
    final var periods =
        new ProfileVersion(
            List.of(new PackageGroupReference("g1", LocalDate.of(2025, 1, 1), null)));
    final var families =
        Map.of(
            "fam",
            new ProfileFamily(
                BASE,
                "1.0",
                Map.of("1.0", periods),
                Map.of(profileName, new ProfileDefinition(null, null))));
    final var config =
        new ValidationModuleManifest(
            "4.0", "m", "1.0", "a", "d", "", true, false, false, List.of(), groups, Map.of(),
            families, List.of(), List.of());
    return new ValidationModuleIndex(new ValidationModule(config, Path.of("/modules/m.jar")));
  }
}
