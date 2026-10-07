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
package de.gematik.refv.lib.fhir_context.entity;

import java.util.List;
import org.jspecify.annotations.NonNull;

/** FHIR R5 version (5.0.x). */
public record R5Version() implements FhirRelease {
  private static final String name = "hl7.fhir.r5.core";
  private static final String version = "5.0.0";

  @Override
  public @NonNull String alias() {
    return "R5";
  }

  @Override
  public @NonNull String canonical() {
    return "5.0";
  }

  @Override
  public @NonNull String corePackageName() {
    return name;
  }

  @Override
  public @NonNull String corePackageVersion() {
    return version;
  }

  @Override
  public int major() {
    return 5;
  }

  @Override
  public @NonNull List<String> packages() {
    return List.of(
        name + "#" + version,
        "hl7.terminology.r5#7.4.0",
        "hl7.fhir.uv.extensions.r5#5.3.0",
        "hl7.fhir.xver-extensions#0.1.0");
  }
}
