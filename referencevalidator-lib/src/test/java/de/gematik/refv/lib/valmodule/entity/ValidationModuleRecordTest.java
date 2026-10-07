/*-
 * #%L
 * Validation Module API
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
package de.gematik.refv.lib.valmodule.entity;

import de.gematik.refv.valmodule.api.boundary.FhirValidationModule;
import de.gematik.refv.valmodule.api.entity.PackageGroup;
import de.gematik.refv.valmodule.api.entity.PackageGroupReference;
import de.gematik.refv.valmodule.api.entity.ProfileFamily;
import de.gematik.refv.valmodule.api.entity.ProfileVersion;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidationModuleRecordTest {

  private static ValidationModuleManifest minimalConfig() {
    return new ValidationModuleManifest(
        "4.0",
        "m",
        "1.0",
        "a",
        "d",
        "",
        true,
        false,
        false,
        List.of(),
        Map.of("p1", new PackageGroup(List.of("test#1.0.0"), null)),
        Map.of(),
        Map.of(
            "family",
            new ProfileFamily(
                "https://example.org/fhir",
                null,
                Map.of(
                    "1.0.0",
                    new ProfileVersion(List.of(new PackageGroupReference("p1", null, null)))),
                null)),
        List.of(),
        List.of());
  }

  @DisplayName("Given a valid config and path, when created, then the accessors return the values")
  @Test
  void expectValidModuleCreation() {
    final var module = new ValidationModule(minimalConfig(), Path.of("/modules/m.jar"));
    Assertions.assertEquals("m", module.configuration().name());
    Assertions.assertEquals(Path.of("/modules/m.jar"), module.modulePath());
  }

  @DisplayName("Given a null configuration, when created, then a NullPointerException is thrown")
  @Test
  void expectNullConfigurationThrows() {
    final var modulePath = Path.of("/modules/m.jar");
    Assertions.assertThrows(
        NullPointerException.class, () -> new ValidationModule(null, modulePath));
  }

  @DisplayName("Given a null module path, when created, then a NullPointerException is thrown")
  @Test
  void expectNullModulePathThrows() {
    final var configuration = minimalConfig();
    Assertions.assertThrows(
        NullPointerException.class, () -> new ValidationModule(configuration, null));
  }

  @DisplayName(
      "Given a FhirValidationModule implementation, when called, then name and packages path are returned")
  @Test
  void expectFhirValidationModuleSpiContract() {
    final FhirValidationModule spi =
        new FhirValidationModule() {
          @Override
          public String getName() {
            return "my-module";
          }
        };
    Assertions.assertEquals("my-module", spi.getName());
  }
}
