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
package de.gematik.refv.lib.fhir_context.control;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import de.gematik.refv.lib.config_parser.boundary.JSONMapperProvider;
import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.exceptions.SnapshotGenerationException;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.package_resolver.entity.PackageId;
import de.gematik.refv.lib.package_resolver.entity.PackageResolution;
import de.gematik.refv.lib.package_resolver.entity.ResolvedPackage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.model.ModelContext;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.model.core.VersionResolutionRules;
import org.hl7.fhir.standalone.context.SimpleWorkerContext;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.validation.ValidationEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContextPackageLoaderTest {
  @TempDir Path temporaryDirectory;

  @Test
  void generatedPackageReplacesOriginalWithoutChangingTemplate() throws Exception {
    var configuration = ContextConfiguration.defaultConfiguration();
    var original = profilePackage("original", "OriginalProfile");
    var generated = profilePackage("generated", "GeneratedProfile");
    var templateEngine = emptyEngine();
    var executionEngine = emptyEngine();
    try (var factory = mockStatic(ValidationEngineFactory.class)) {
      factory
          .when(() -> ValidationEngineFactory.createNewCoreEngine(configuration))
          .thenReturn(
              new ValidationEngineFactory.EngineSession(templateEngine, null),
              new ValidationEngineFactory.EngineSession(executionEngine, null));
      try (var template =
              new DefaultSnapshotGenerationContext(
                  configuration, new PackageResolution(List.of(original)));
          var execution = template.cloneContext(List.of(generated))) {
        Assertions.assertNotSame(template, execution);
        Assertions.assertEquals("GeneratedProfile", loadedProfile(executionEngine).getName());
        Assertions.assertEquals("OriginalProfile", loadedProfile(templateEngine).getName());
      }
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void alreadyLoadedBuiltinsMustMatchGeneratedContent(boolean changed) throws Exception {
    var engine = emptyEngine();
    ContextPackageLoader.loadPackageInContext(
        engine, profilePackage("original", "OriginalProfile"));
    var generated = profilePackage("generated", changed ? "ChangedProfile" : "OriginalProfile");
    var generatedPackages = List.of(generated);
    if (changed) {
      Assertions.assertThrows(
          InitializationException.class,
          () -> ContextPackageLoader.loadGeneratedPackagesInContext(engine, generatedPackages));
    } else {
      Assertions.assertDoesNotThrow(
          () -> ContextPackageLoader.loadGeneratedPackagesInContext(engine, generatedPackages));
    }
    Assertions.assertEquals("OriginalProfile", loadedProfile(engine).getName());
  }

  @Test
  void generatedCoreBootstrapsTheExecutionEngineInsteadOfComparingToCachedCore() throws Exception {
    var configuration = ContextConfiguration.defaultConfiguration();
    var coreId = PackageId.parse(configuration.fhirRelease().corePackageId());
    var original = corePackage("original-core", "OriginalProfile", coreId);
    var generated = corePackage("generated-core", "GeneratedProfile", coreId);
    var templateEngine = emptyEngine();
    var executionEngine = emptyEngine();
    ContextPackageLoader.loadPackageInContext(executionEngine, generated);

    try (var factory = mockStatic(ValidationEngineFactory.class)) {
      factory
          .when(() -> ValidationEngineFactory.createNewCoreEngine(configuration))
          .thenReturn(new ValidationEngineFactory.EngineSession(templateEngine, null));
      factory
          .when(
              () ->
                  ValidationEngineFactory.createNewCoreEngine(
                      configuration, generated.packagePath()))
          .thenReturn(new ValidationEngineFactory.EngineSession(executionEngine, null));
      try (var template =
              new DefaultSnapshotGenerationContext(
                  configuration, new PackageResolution(List.of(original)));
          var execution = template.cloneContext(List.of(generated))) {
        Assertions.assertNotSame(template, execution);
        Assertions.assertEquals("GeneratedProfile", loadedProfile(executionEngine).getName());
        Assertions.assertEquals("OriginalProfile", loadedProfile(templateEngine).getName());
        factory.verify(
            () ->
                ValidationEngineFactory.createNewCoreEngine(
                    configuration, generated.packagePath()));
      }
    }
  }

  @Test
  void localCoreSelectorLoadsCoordinateNamedDirectoriesWithoutRegistryLookup() throws Exception {
    var coreId = PackageId.parse("hl7.fhir.r4.core#4.0.1");
    var core = corePackage(coreId.coordinates(), "GeneratedProfile", coreId);
    var cache = Files.createDirectories(temporaryDirectory.resolve("package-cache"));
    var manager =
        new FilesystemPackageCacheManager.Builder().withCacheFolder(cache.toString()).build();

    var loaded =
        manager.loadPackage(ValidationEngineFactory.localCoreSource(core.packagePath()), null);

    Assertions.assertEquals(coreId.name(), loaded.name());
    Assertions.assertEquals(coreId.version(), loaded.version());
    Assertions.assertEquals(core.packagePath(), Path.of(loaded.getPath()).normalize());
  }

  @Test
  void repeatedIndexRebuildsKeepResourceListsUniqueInEveryFolder() throws Exception {
    var profile = profilePackage("indexed", "TestProfile");
    var examples = Files.createDirectories(profile.packagePath().resolve("examples"));
    Files.writeString(
        examples.resolve("Patient-example.json"),
        """
      {"resourceType":"Patient","id":"example"}
      """);
    var npmPackage = NpmPackage.fromFolder(profile.packagePath().toString(), false);

    for (int rebuild = 0; rebuild < 2; rebuild++) {
      DefaultSnapshotGenerationContext.rebuildPackageIndexes(npmPackage, profile.packagePath());

      Assertions.assertEquals(
          List.of("StructureDefinition-test.json"), npmPackage.listResourcesInFolder("package"));
      Assertions.assertEquals(
          List.of("Patient-example.json"), npmPackage.listResourcesInFolder("package/examples"));
    }
  }

  @Test
  void indexRebuildDropsRemovedResourcesAndPreviousResourceTypes() throws Exception {
    var profile = profilePackage("patched-index", "TestProfile");
    var packageFolder = profile.packagePath().resolve("package");
    var oldPatient = packageFolder.resolve("Patient-old.json");
    Files.writeString(oldPatient, "{\"resourceType\":\"Patient\",\"id\":\"old\"}");
    var npmPackage = NpmPackage.fromFolder(profile.packagePath().toString(), false);

    Files.delete(oldPatient);
    Files.writeString(
        packageFolder.resolve("StructureDefinition-test.json"),
        """
      {"resourceType":"ValueSet","id":"test","url":"http://example.org/ValueSet/test",
       "version":"1.0.0","status":"active"}
      """);
    Files.writeString(
        packageFolder.resolve("Patient-new.json"),
        """
      {"resourceType":"Patient","id":"new"}
      """);

    DefaultSnapshotGenerationContext.rebuildPackageIndexes(npmPackage, profile.packagePath());

    Assertions.assertEquals(
        List.of(), npmPackage.listResourcesInFolder("package", Set.of("StructureDefinition")));
    Assertions.assertEquals(
        List.of("StructureDefinition-test.json"),
        npmPackage.listResourcesInFolder("package", Set.of("ValueSet")));
    Assertions.assertEquals(
        List.of("Patient-new.json"),
        npmPackage.listResourcesInFolder("package", Set.of("Patient")));
  }

  private ResolvedPackage corePackage(String directory, String profileName, PackageId coreId)
      throws Exception {
    var profile = profilePackage(directory, profileName);
    var manifestPath = profile.packagePath().resolve("package/package.json");
    var mapper = JSONMapperProvider.getMapper();
    var manifest = mapper.createObjectNode();
    manifest.put("name", coreId.name());
    manifest.put("version", coreId.version());
    manifest.putArray("fhirVersions").add("4.0.1");
    mapper.writeValue(manifestPath.toFile(), manifest);
    return new ResolvedPackage(coreId, profile.packagePath(), List.of());
  }

  private ValidationEngine emptyEngine() throws Exception {
    var engine = mock(ValidationEngine.class);
    var context =
        new SimpleWorkerContext.SimpleWorkerContextBuilder(ModelContext.fullCoreContext())
            .fromNothing();
    when(engine.getContext()).thenReturn(context);
    return engine;
  }

  private StructureDefinition loadedProfile(ValidationEngine engine) {
    return engine
        .getContext()
        .fetchResource(
            StructureDefinition.class,
            "http://example.org/StructureDefinition/test|1.0.0",
            VersionResolutionRules.defaultRule());
  }

  private ResolvedPackage profilePackage(String directory, String profileName) throws Exception {
    var root = Files.createDirectories(temporaryDirectory.resolve(directory).resolve("package"));
    Files.writeString(
        root.resolve("package.json"),
        """
        {"name":"test.profile","version":"1.0.0","fhirVersions":["5.0.0"]}
        """);
    Files.writeString(
        root.resolve("StructureDefinition-test.json"),
        """
        {"resourceType":"StructureDefinition","id":"test",
         "url":"http://example.org/StructureDefinition/test","version":"1.0.0",
         "name":"%s","status":"active","kind":"resource","abstract":false,
         "type":"Patient","baseDefinition":"http://hl7.org/fhir/StructureDefinition/Patient",
         "derivation":"constraint"}
        """
            .formatted(profileName));
    return new ResolvedPackage(new PackageId("test.profile", "1.0.0"), root.getParent(), List.of());
  }

  @DisplayName(
      "Given a loader, when extracting the profile prefix from an empty set, then a SnapshotGenerationException is thrown")
  @Test
  void expectExtractProfilePrefixFromEmptySetThrows() {
    final var noDefinitions = List.<StructureDefinition>of();
    Assertions.assertThrows(
        SnapshotGenerationException.class,
        () -> ContextPackageLoader.extractProfilePrefixFromStructureDefinition(noDefinitions));
  }

  @DisplayName(
      "Given a structure definition, when extracting the profile prefix, then the parent URL is returned")
  @Test
  void expectExtractProfilePrefixReturnsParent() {
    final var sd = new StructureDefinition();
    sd.setUrl("http://example.org/fhir/StructureDefinition/MyProfile");
    final var prefix =
        ContextPackageLoader.extractProfilePrefixFromStructureDefinition(List.of(sd));
    Assertions.assertEquals("http://example.org/fhir/StructureDefinition/", prefix);
  }
}
