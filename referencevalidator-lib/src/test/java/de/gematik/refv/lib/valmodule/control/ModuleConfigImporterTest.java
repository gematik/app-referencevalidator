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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("DataFlowIssue")
class ModuleConfigImporterTest {

  private final ModuleConfigImporter importer =
      new ModuleConfigImporter(YAMLMapperProvider.getMapper());

  @ParameterizedTest(name = "{0}")
  @MethodSource("importCases")
  void importsManifests(String requirement) throws Exception {
    switch (requirement) {
      case "R3.1" -> {
        var result = importer.importFromStream(source(validManifest())).orElseThrow();
        Assertions.assertEquals("erp", result.name());
        Assertions.assertEquals("1.0", result.version());
      }
      case "R3.2" -> Assertions.assertTrue(importer.importFromStream(null).isEmpty());
      case "R3.3" -> {
        for (var invalid :
            Stream.of(
                    "",
                    "null",
                    "name: [invalid",
                    validManifest() + "unknownField: true\n",
                    validManifest().replace("version: \"1.0\"", "version: [invalid]"))
                .toList()) {
          var invalidSource = source(invalid);
          var failure =
              Assertions.assertThrows(
                  IOException.class, () -> importer.importFromStream(invalidSource));
          Assertions.assertFalse(failure.getMessage().isBlank());
        }
        var unknownSource = source(validManifest() + "unknownField: true\n");
        var unknownField =
            Assertions.assertThrows(
                IOException.class, () -> importer.importFromStream(unknownSource));
        Assertions.assertTrue(unknownField.getMessage().contains("unknownField"));
      }
      case "R3.4" ->
          Assertions.assertThrows(NullPointerException.class, () -> new ModuleConfigImporter(null));
      default -> Assertions.fail("Unexpected requirement " + requirement);
    }
  }

  static Stream<String> importCases() {
    return Stream.of("R3.1", "R3.2", "R3.3", "R3.4");
  }

  static ByteArrayInputStream source(String yaml) {
    return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
  }

  static String validManifest() {
    return """
        configSpecVersion: "3.0"
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
  }
}
