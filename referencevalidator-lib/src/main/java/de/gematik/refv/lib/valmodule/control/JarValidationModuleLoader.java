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

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.gematik.refv.lib.exceptions.LoadModuleException;
import de.gematik.refv.lib.valmodule.boundary.ModuleConfigImporter;
import de.gematik.refv.lib.valmodule.boundary.ModuleLoader;
import de.gematik.refv.lib.valmodule.entity.ValidationModule;
import de.gematik.refv.valmodule.api.entity.ValidationModuleManifest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Module Loader for loading external JAR Validation Modules. */
public final class JarValidationModuleLoader implements ModuleLoader {
  private static final String MODULE_EXTENSION = ".jar";
  private static final Logger log = LoggerFactory.getLogger(JarValidationModuleLoader.class);

  private final ModuleConfigImporter moduleConfigImporter;

  /**
   * Constructs the class with the provided YAML Wrapper.
   *
   * @param mapper a YAML mapper for parsing the configuration
   */
  public JarValidationModuleLoader(@NonNull YAMLMapper mapper) {
    this.moduleConfigImporter = new ModuleConfigImporter(mapper);
  }

  @Override
  public @NonNull Optional<ValidationModule> forValidation(
      @NonNull Path directory, @NonNull String name) throws LoadModuleException {
    Objects.requireNonNull(directory, "The directory of modules must not be null");
    Objects.requireNonNull(directory, "The name of the module must not be null");
    if (!Files.exists(directory) || !Files.isDirectory(directory)) {
      throw new LoadModuleException(
          "The path '"
              + directory
              + "' does not point to a valid directory for locating the validation modules");
    }

    try (Stream<Path> jars = Files.list(directory)) {
      final var foundJarPaths =
          jars.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(MODULE_EXTENSION))
              .toList();

      for (var jarPath : foundJarPaths) {
        var validationModule = loadModuleJar(jarPath);
        if (validationModule.isPresent()
            && validationModule.get().configuration().name().equalsIgnoreCase(name)) {
          log.info("Loaded {} module from {}", name, directory);
          return validationModule;
        }
      }
      throw new LoadModuleException("Could not find a matching module matching the name: " + name);
    } catch (IOException e) {
      throw new LoadModuleException("Failed to look for the module " + name, e);
    }
  }

  /** Load a single plugin JAR. */
  private Optional<ValidationModule> loadModuleJar(@NonNull Path jarPath) {
    log.debug("Attempting to load Validation Module JAR: {}", jarPath);

    // Create a dedicated ClassLoader for this JAR
    try (final URLClassLoader classLoader = createForJar(jarPath)) {
      if (Objects.isNull(classLoader.findResource(MODULE_DEFINITION_PATH))) {
        throw new LoadModuleException(
            String.format("The module %s does not contain a manifest", jarPath));
      }

      // Read module configuration descriptor from JAR
      final var foundConfiguration = readConfiguration(classLoader);

      if (foundConfiguration.isEmpty()) {
        log.warn("No Module configuration found in JAR - skipping: {}", jarPath);
        return Optional.empty();
      }
      // short-handing
      var configuration = foundConfiguration.get();
      log.info(
          "Module configuration loaded: {} - v{}", configuration.name(), configuration.version());

      // Build runtime info
      return Optional.of(new ValidationModule(configuration, jarPath));
    } catch (Exception e) {
      log.warn(
          "Could not load Validation Module: {} - skipping (error: {})",
          jarPath,
          e.getLocalizedMessage());
      log.debug(e.getMessage(), e);
      return Optional.empty();
    }
  }

  private @NonNull URLClassLoader createForJar(@NonNull Path jarPath) throws IOException {
    final URL jarUrl = jarPath.toUri().toURL();
    final ClassLoader parent = Thread.currentThread().getContextClassLoader();
    return new URLClassLoader(new URL[] {jarUrl}, parent);
  }

  /**
   * Reads the YAML Configuration from the given JAR's ClassLoader. Expects the configuration to be
   * located at the fixed path defined by CONFIGURATION_PATH. If the configuration file is not
   * found, returns null.
   *
   * @param classLoader the {@link URLClassLoader} for the current class
   * @return an optional found {@link ValidationModuleManifest} instance, or nothing
   * @throws IOException in case of I/O errors
   */
  private @NonNull Optional<ValidationModuleManifest> readConfiguration(
      @NonNull URLClassLoader classLoader) throws IOException {
    try (final InputStream is = classLoader.getResourceAsStream(MANIFEST_PATH)) {
      return moduleConfigImporter.importFromStream(is);
    }
  }
}
