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
package de.gematik.refv.lib.valmodule.boundary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.refv.lib.config_parser.boundary.YAMLMapperProvider;
import de.gematik.refv.lib.exceptions.LoadModuleException;
import de.gematik.refv.lib.valmodule.control.JarValidationModuleLoader;
import de.gematik.refv.lib.valmodule.control.SnapshotValidationModuleLoader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;

@SuppressWarnings({"DataFlowIssue", "resource"})
class ModuleLoadingRequirementsTest {
  @TempDir Path modules;

  @ParameterizedTest(name = "{0}")
  @MethodSource("validationCases")
  void loadsValidationModules(String requirement) throws Exception {
    var loader = ModuleLoader.defaultLoader();
    switch (requirement) {
      case "R1.1" -> {
        var jar = writeJar("module.jar", manifest("example", "1.0"), true);
        var module = loader.forValidation(modules, "example").orElseThrow();
        assertEquals(jar, module.modulePath());
        assertEquals("example", module.configuration().name());
        assertEquals("1.0", module.configuration().version());
      }
      case "R1.2" -> {
        assertTrue(loader.forValidation(modules, "missing").isEmpty());
        writeJar("other.jar", manifest("other", "1.0"), true);
        assertTrue(loader.forValidation(modules, "missing").isEmpty());
      }
      case "R1.3" -> {
        var older = writeJar("older.jar", manifest("example", "1.0"), true);
        var newer = writeJar("newer.jar", manifest("example", "9.0"), true);
        try (var _ = mockStatic(Files.class, directoryListing(modules, older, newer))) {
          assertEquals(older, loader.forValidation(modules, "example").orElseThrow().modulePath());
        }
        try (var _ = mockStatic(Files.class, directoryListing(modules, newer, older))) {
          assertEquals(newer, loader.forValidation(modules, "example").orElseThrow().modulePath());
        }
      }
      case "R1.4" -> {
        writeJar("module.jar", manifest("ExAmPlE", "1.0"), true);
        assertEquals(
            "ExAmPlE",
            loader.forValidation(modules, "example").orElseThrow().configuration().name());
      }
      case "R1.5" -> {
        var nested = Files.createDirectory(modules.resolve("nested"));
        Files.move(
            writeJar("nested.jar", manifest("example", "1.0"), true), nested.resolve("module.jar"));
        writeJar("ignored.txt", manifest("example", "1.0"), true);
        Files.createDirectory(modules.resolve("directory.jar"));
        assertTrue(loader.forValidation(modules, "example").isEmpty());
        var jar = writeJar("module.JAR", manifest("example", "1.0"), true);
        assertEquals(jar, loader.forValidation(modules, "example").orElseThrow().modulePath());
      }
      case "R1.6" -> {
        writeJar("no-manifest.jar", null, true);
        writeJar("no-registration.jar", manifest("example", "1.0"), false);
        var parentRoot = Files.createDirectory(modules.resolve("parent"));
        var parentManifest = parentRoot.resolve(ModuleLoader.MANIFEST_PATH);
        Files.createDirectories(parentManifest.getParent());
        Files.writeString(parentManifest, manifest("example", "1.0"));
        var registration = parentRoot.resolve(ModuleLoader.MODULE_DEFINITION_PATH);
        Files.createDirectories(registration.getParent());
        Files.writeString(registration, "missing.Provider");
        var thread = Thread.currentThread();
        var previous = thread.getContextClassLoader();
        try (var parent = new URLClassLoader(new URL[] {parentRoot.toUri().toURL()}, previous)) {
          thread.setContextClassLoader(parent);
          assertTrue(loader.forValidation(modules, "example").isEmpty());
          var own = writeJar("own.jar", manifest("example", "2.0"), true);
          var module = loader.forValidation(modules, "example").orElseThrow();
          assertEquals(own, module.modulePath());
          assertEquals("2.0", module.configuration().version());
        } finally {
          thread.setContextClassLoader(previous);
        }
      }
      case "R1.7" -> verifiesCandidateDiagnostics(loader);
      case "R1.8" -> {
        var missing = modules.resolve("missing");
        assertThrows(LoadModuleException.class, () -> loader.forValidation(missing, "example"));
        var file = Files.writeString(modules.resolve("not-a-directory"), "content");
        assertThrows(LoadModuleException.class, () -> loader.forValidation(file, "example"));
      }
      case "R1.9" -> {
        assertThrows(NullPointerException.class, () -> loader.forValidation(null, "example"));
        assertThrows(NullPointerException.class, () -> loader.forValidation(modules, null));
      }
      case "R1.10" -> {
        var cause = new IOException("Directory listing denied");
        try (var listing = mockStatic(Files.class)) {
          listing.when(() -> Files.exists(modules)).thenReturn(true);
          listing.when(() -> Files.isDirectory(modules)).thenReturn(true);
          listing.when(() -> Files.list(modules)).thenThrow(cause);
          assertSame(
              cause,
              assertThrows(
                      LoadModuleException.class, () -> loader.forValidation(modules, "example"))
                  .getCause());
        }
      }
      case "R1.11" -> {
        var jar = writeJar("metadata-only.jar", manifest("example", "1.0"), true);
        assertEquals(jar, loader.forValidation(modules, "example").orElseThrow().modulePath());
      }
      case "R1.12" -> {
        var snapshotLoader = new SnapshotValidationModuleLoader(YAMLMapperProvider.getMapper());
        assertThrows(
            LoadModuleException.class, () -> snapshotLoader.forValidation(modules, "example"));
      }
      default -> fail("Unexpected requirement " + requirement);
    }
  }

  static Stream<String> validationCases() {
    return Stream.of(
        "R1.1", "R1.2", "R1.3", "R1.4", "R1.5", "R1.6", "R1.7", "R1.8", "R1.9", "R1.10", "R1.11",
        "R1.12");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("snapshotCases")
  void loadsSnapshotModules(String requirement) throws Exception {
    var loader = new SnapshotValidationModuleLoader(YAMLMapperProvider.getMapper());
    var manifestPath = modules.resolve(ModuleLoader.MANIFEST_PATH);
    switch (requirement) {
      case "R2.1" -> {
        Files.createDirectories(manifestPath.getParent());
        Files.writeString(manifestPath, manifest("example", "1.0"));
        var module = loader.forSnapshotGeneration(modules).orElseThrow();
        assertEquals(modules, module.modulePath());
        assertEquals("example", module.configuration().name());
      }
      case "R2.2" -> {
        assertInstanceOf(
            IOException.class,
            assertThrows(LoadModuleException.class, () -> loader.forSnapshotGeneration(modules))
                .getCause());
        Files.createDirectories(manifestPath);
        assertInstanceOf(
            IOException.class,
            assertThrows(LoadModuleException.class, () -> loader.forSnapshotGeneration(modules))
                .getCause());
      }
      case "R2.3" -> {
        Files.createDirectories(manifestPath.getParent());
        Files.writeString(manifestPath, "name: [invalid");
        var failure =
            assertThrows(LoadModuleException.class, () -> loader.forSnapshotGeneration(modules));
        assertInstanceOf(IOException.class, failure.getCause());
        assertTrue(failure.getMessage().contains(manifestPath.toString()));
        assertTrue(failure.getMessage().contains(failure.getCause().getLocalizedMessage()));
      }
      case "R2.4" ->
          assertThrows(NullPointerException.class, () -> loader.forSnapshotGeneration(null));
      case "R2.5" -> {
        var validationLoader = ModuleLoader.defaultLoader();
        assertThrows(
            LoadModuleException.class, () -> validationLoader.forSnapshotGeneration(modules));
      }
      default -> fail("Unexpected requirement " + requirement);
    }
  }

  static Stream<String> snapshotCases() {
    return Stream.of("R2.1", "R2.2", "R2.3", "R2.4", "R2.5");
  }

  void verifiesCandidateDiagnostics(ModuleLoader loader) throws Exception {
    var corrupt = Files.writeString(modules.resolve("corrupt.jar"), "not a JAR");
    var invalid = writeJar("invalid.jar", "name: [invalid", true);
    var valid = writeJar("valid.jar", manifest("example", "1.0"), true);
    var diagnostics = new ArrayList<String>();
    var appender = mock(Appender.class);
    when(appender.getName()).thenReturn("valmodule-requirements");
    when(appender.isStarted()).thenReturn(true);
    doAnswer(
            invocation ->
                diagnostics.add(
                    invocation.getArgument(0, LogEvent.class).getMessage().getFormattedMessage()))
        .when(appender)
        .append(any());
    var logger = (Logger) LogManager.getLogger(JarValidationModuleLoader.class);
    var previousLevel = logger.getLevel();
    logger.setLevel(Level.WARN);
    logger.addAppender(appender);
    try (var _ =
        mockStatic(Files.class, directoryListing(modules, corrupt, invalid, valid))) {
      assertEquals(valid, loader.forValidation(modules, "example").orElseThrow().modulePath());
      assertTrue(diagnostics.stream().anyMatch(message -> identifiesFailure(message, corrupt)));
      assertTrue(diagnostics.stream().anyMatch(message -> identifiesFailure(message, invalid)));
    } finally {
      logger.removeAppender(appender);
      logger.setLevel(previousLevel);
    }
  }

  static boolean identifiesFailure(String diagnostic, Path candidate) {
    return diagnostic.contains(candidate.toString()) && diagnostic.contains("error:");
  }

  static Answer<Object> directoryListing(Path directory, Path... entries) {
    return invocation ->
        invocation.getMethod().getName().equals("list")
                && directory.equals(invocation.getArgument(0))
            ? Stream.of(entries)
            : invocation.callRealMethod();
  }

  Path writeJar(String name, String configuration, boolean registered) throws IOException {
    var path = modules.resolve(name);
    try (var archive = new JarOutputStream(Files.newOutputStream(path))) {
      if (registered) {
        writeEntry(archive, ModuleLoader.MODULE_DEFINITION_PATH, "missing.Provider");
      }
      if (configuration != null) {
        writeEntry(archive, ModuleLoader.MANIFEST_PATH, configuration);
      }
      // Loading metadata must not instantiate the provider or process this invalid package.
      writeEntry(archive, ModuleLoader.PACKAGE_PATH + "unreadable-package.tgz", "not a package");
    }
    return path;
  }

  static void writeEntry(JarOutputStream archive, String path, String content) throws IOException {
    archive.putNextEntry(new JarEntry(path));
    archive.write(content.getBytes(StandardCharsets.UTF_8));
    archive.closeEntry();
  }

  static String manifest(String name, String version) {
    return """
        configSpecVersion: "3.0"
        name: "%s"
        version: "%s"
        author: "example publisher"
        description: "module requirements fixture"
        packageGroups:
          example:
            packages: ["example#1.0.0"]
        profileFamilies:
          example:
            canonicalBase: "https://example.org/fhir/StructureDefinition"
            versions:
              "1.0":
                groups:
                  - packageGroupName: "example"
        """
        .formatted(name, version);
  }
}
