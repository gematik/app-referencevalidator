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
import de.gematik.refv.lib.exceptions.LoadModuleException;
import de.gematik.refv.lib.valmodule.boundary.ModuleLoader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JarValidationModuleLoaderTest {

  @TempDir Path modulesDir;

  private final JarValidationModuleLoader loader =
      new JarValidationModuleLoader(YAMLMapperProvider.getMapper());

  @DisplayName("Expect that the load does not implement the method 'forSnapshotGeneration'")
  @Test
  void expectMethodNotImplemented() {
    final Path notExisting = Path.of("notExisting");
    Assertions.assertThrows(
        LoadModuleException.class, () -> loader.forSnapshotGeneration(notExisting));
  }

  @DisplayName(
      "Given a directory with a valid module JAR, when loading by name, then the module index is returned")
  @Test
  void expectValidModuleJarLoads() throws Exception {
    writeJar("erp-module.jar", configFor("erp"), true);
    final var validationModule =
        Assertions.assertDoesNotThrow(() -> loader.forValidation(modulesDir, "erp"));
    Assertions.assertNotNull(validationModule);
    Assertions.assertTrue(validationModule.isPresent());
    Assertions.assertEquals("erp", validationModule.get().configuration().name());
  }

  @DisplayName(
      "Given a module JAR, when loading by name with different case, then it still matches")
  @Test
  void expectModuleNameMatchIsCaseInsensitive() throws Exception {
    writeJar("erp-module.jar", configFor("ERP"), true);
    final var validationModule =
        Assertions.assertDoesNotThrow(() -> loader.forValidation(modulesDir, "erp"));
    Assertions.assertNotNull(validationModule);
    Assertions.assertTrue(validationModule.isPresent());
    Assertions.assertEquals("ERP", validationModule.get().configuration().name());
  }

  @DisplayName(
      "Given a JAR without a config, when loading, then it is skipped and a LoadModuleException is thrown")
  @Test
  void expectJarWithoutConfigIsSkipped() throws Exception {
    writeJar("no-config.jar", null, true);
    Assertions.assertThrows(
        LoadModuleException.class, () -> loader.forValidation(modulesDir, "erp"));
  }

  @DisplayName(
      "Given a JAR without a manifest, when loading, then it is skipped and a LoadModuleException is thrown")
  @Test
  void expectJarWithoutManifestIsSkipped() throws Exception {
    writeJar("no-manifest.jar", configFor("erp"), false);
    Assertions.assertThrows(
        LoadModuleException.class, () -> loader.forValidation(modulesDir, "erp"));
  }

  @DisplayName("Given a directory with a non-JAR file, when loading, then it is ignored")
  @Test
  void expectNonJarFilesIgnored() throws Exception {
    Files.writeString(modulesDir.resolve("readme.txt"), "not a jar");
    Assertions.assertThrows(
        LoadModuleException.class, () -> loader.forValidation(modulesDir, "erp"));
  }

  @DisplayName(
      "Given a corrupt JAR, when loading, then it is skipped and a LoadModuleException is thrown")
  @Test
  void expectCorruptJarIsSkipped() throws Exception {
    Files.writeString(modulesDir.resolve("corrupt.jar"), "not a real jar content");
    Assertions.assertThrows(
        LoadModuleException.class, () -> loader.forValidation(modulesDir, "erp"));
  }

  @DisplayName(
      "Given multiple JARs, when loading a specific module, then the matching one is returned")
  @Test
  void expectMatchingModuleAmongMany() throws Exception {
    writeJar("isik-module.jar", configFor("isik"), true);
    writeJar("erp-module.jar", configFor("erp"), true);
    final var validationModule =
        Assertions.assertDoesNotThrow(() -> loader.forValidation(modulesDir, "isik"));
    Assertions.assertNotNull(validationModule);
    Assertions.assertTrue(validationModule.isPresent());
    Assertions.assertEquals("isik", validationModule.get().configuration().name());
  }

  /** Writes a JAR with the given entries into the modules directory. */
  private Path writeJar(String jarName, String configYaml, boolean withManifest)
      throws IOException {
    final var jarPath = modulesDir.resolve(jarName);
    try (OutputStream fos = Files.newOutputStream(jarPath);
        JarOutputStream jos = new JarOutputStream(fos)) {
      if (withManifest) {
        jos.putNextEntry(new JarEntry(ModuleLoader.MODULE_DEFINITION_PATH));
        jos.write("test.Plugin".getBytes());
        jos.closeEntry();
      }
      if (configYaml != null) {
        jos.putNextEntry(new JarEntry(ModuleLoader.MANIFEST_PATH));
        jos.write(configYaml.getBytes());
        jos.closeEntry();
      }
    }
    return jarPath;
  }

  private static String configFor(String name) {
    return """
        configSpecVersion: "3.0"
        name: "%s"
        version: "1.0"
        author: "gematik"
        description: "test module"
        errorOnUnknownProfile: true
        anyExtensionsAllowed: false
        requireExpansionBeforeValidation: false
        packageGroups:
          my-group-id:
            packages:
              - "de.basisprofil.r4-1.5.4.tgz"
        profileFamilies:
          my-family-id:
            canonicalBase: "https://gematik.de/example/fhir"
            versions:
              "1.0.0":
                groups:
                  - packageGroupName: "my-group-id"
        """
        .formatted(name);
  }
}
