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
package de.gematik.refv.lib.snapshot.boundary;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;
import de.gematik.refv.lib.fhir_context.boundary.SnapshotGenerationContext;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.package_resolver.control.DefaultPackageResolver;
import de.gematik.refv.lib.package_resolver.entity.PackageResolution;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SnapshotGeneratorLifecycleRequirementsTest {

  @TempDir Path tempDir;

  @ParameterizedTest(name = "{0}")
  @MethodSource("closeCases")
  void closingGeneratorFollowsLifecycleRequirement(String requirementLabel, int closeCount)
      throws Exception {
    var configuration = ContextConfiguration.defaultConfiguration();
    var corePackages = new PackageResolution(List.of());
    var contextProvider = mock(ContextProvider.class);
    var context = mock(SnapshotGenerationContext.class);
    var ownedResource = Files.createDirectory(tempDir.resolve("owned-resource"));
    when(context.cachePath()).thenReturn(tempDir.resolve("package-cache"));
    when(contextProvider.snapshotGenerationContext(configuration, corePackages))
        .thenReturn(context);
    doAnswer(_ -> Files.deleteIfExists(ownedResource)).when(context).close();

    try (var providers = mockStatic(ContextProvider.class);
        var _ =
            mockConstruction(
                DefaultPackageResolver.class,
                (resolver, _) ->
                    when(resolver.loadCore(configuration.fhirRelease()))
                        .thenReturn(corePackages))) {
      providers.when(ContextProvider::defaultProvider).thenReturn(contextProvider);
      try (var generator = SnapshotGeneratorFactory.fromConfiguration(configuration)) {
        Assertions.assertTrue(Files.isDirectory(ownedResource));
        Assertions.assertDoesNotThrow(generator::close);
        Assertions.assertFalse(Files.exists(ownedResource));
        verify(context).close();
        if (closeCount > 1) {
          Assertions.assertDoesNotThrow(generator::close);
          Assertions.assertFalse(Files.exists(ownedResource));
          verify(context).close();
        }
      }
      verify(context).close();
    }
  }

  private static Stream<Arguments> closeCases() {
    return Stream.of(
        Arguments.of("R3.1 — closing the generator releases its owned resources", 1),
        Arguments.of("R3.2 — closing an already closed generator has no additional effect", 2));
  }
}
