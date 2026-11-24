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

import de.gematik.refv.lib.exceptions.InitializationException;
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SnapshotGeneratorFactoryTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("generatorCreationCases")
  void generatorCreationFollowsConfigurationOutcome(
      String requirementLabel, ContextConfiguration configuration, boolean expectFailure) {
    if (expectFailure) {
      Assertions.assertThrows(
          InitializationException.class,
          () -> SnapshotGeneratorFactory.fromConfiguration(configuration));
      return;
    }

    try (var generator = SnapshotGeneratorFactory.fromConfiguration(configuration)) {
      Assertions.assertNotNull(generator);
    }
  }

  @DisplayName("R1.12 — public snapshot boundaries do not expose concrete HL7 types")
  @Test
  void boundarySignaturesDoNotExposeHl7Types() {
    var boundaryMethods =
        Stream.concat(
            Arrays.stream(SnapshotGenerator.class.getMethods()),
            Arrays.stream(SnapshotGeneratorFactory.class.getMethods()));

    Assertions.assertTrue(
        boundaryMethods
            .flatMap(SnapshotGeneratorFactoryTest::signatureTypes)
            .noneMatch(type -> type.getPackageName().startsWith("org.hl7.fhir")));
  }

  private static Stream<Class<?>> signatureTypes(Method method) {
    return Stream.concat(
        Stream.of(method.getReturnType()),
        Stream.concat(
            Arrays.stream(method.getParameterTypes()), Arrays.stream(method.getExceptionTypes())));
  }

  private static Stream<Arguments> generatorCreationCases() {
    return Stream.of(
        Arguments.of(
            "R2.1 — supplied context configuration creates a generator",
            ContextConfiguration.defaultConfiguration(),
            false),
        Arguments.of("R2.2 — initialization failure is reported", null, true));
  }
}
