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
package de.gematik.refv.lib.valmodule.control;

import de.gematik.refv.lib.config_parser.boundary.YAMLMapperProvider;
import de.gematik.refv.lib.valmodule.boundary.ModuleConfigImporter;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModuleConfigImporterTest {

  private final ModuleConfigImporter importer =
      new ModuleConfigImporter(YAMLMapperProvider.getMapper());

  @DisplayName("Given a null stream, when importing, then an empty result is returned")
  @Test
  void expectNullStreamReturnsEmpty() throws Exception {
    Assertions.assertTrue(importer.importFromStream(null).isEmpty());
  }

  @DisplayName("Given a valid YAML stream, when importing, then the configuration is parsed")
  @Test
  void expectValidYamlParsed() throws Exception {
    final String yaml =
        """
        configSpecVersion: "4.0"
        name: "erp"
        version: "1.0"
        author: "gematik"
        description: "test module"
        errorOnUnknownProfile: true
        anyExtensionsAllowed: false
        requireExpansionBeforeValidation: false
        packageGroups:
          mypkg:
             packages:
               - "my.pkg-1.0.0.tgz"
        profileFamilies:
          mypkg:
            canonicalBase: "https://example.org/fhir"
            versions:
              "1.0.0":
                 groups:
                   - packageGroupName: mypkg
        """;
    final var result =
        importer.importFromStream(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    Assertions.assertTrue(result.isPresent());
    Assertions.assertEquals("erp", result.get().name());
    Assertions.assertEquals("1.0", result.get().version());
  }

  @DisplayName("Given an invalid YAML stream, when importing, then an exception is thrown")
  @Test
  void expectInvalidYamlThrows() {
    final var invalid = new ByteArrayInputStream("{{{ not yaml".getBytes(StandardCharsets.UTF_8));
    Assertions.assertThrows(Exception.class, () -> importer.importFromStream(invalid));
  }
}
