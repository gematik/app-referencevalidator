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

import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SnapshotGeneratorLifecycleRequirementsTest {

  @ParameterizedTest(name = "{0}")
  @MethodSource("closeCases")
  void closingGeneratorFollowsLifecycleRequirement(String requirementLabel, int closeCount) {
    try (var generator =
        SnapshotGeneratorFactory.fromConfiguration(ContextConfiguration.defaultConfiguration())) {
      Assertions.assertDoesNotThrow(generator::close);
      if (closeCount > 1) {
        Assertions.assertDoesNotThrow(generator::close);
      }
    }
  }

  private static Stream<Arguments> closeCases() {
    return Stream.of(
        Arguments.of("R3.1 — closing the generator releases its owned resources", 1),
        Arguments.of("R3.2 — closing an already closed generator has no additional effect", 2));
  }
}
