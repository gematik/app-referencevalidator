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
package de.gematik.refv.lib.validation.entity;

import de.gematik.refv.lib.exceptions.ParsingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/** JSON-based FHIR resource. */
public record JsonFhirResource(byte @NonNull [] rawContent) implements FhirResource {

  public JsonFhirResource {
    Objects.requireNonNull(rawContent, "The JSON resource cannot be null");
  }

  @Override
  public InputStream inputStream() {
    return new ByteArrayInputStream(rawContent);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    JsonFhirResource that = (JsonFhirResource) o;
    return Objects.deepEquals(rawContent, that.rawContent);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(rawContent);
  }

  @Override
  public @NonNull String toString() {
    return new String(rawContent, StandardCharsets.UTF_8);
  }

  public static FhirResource fromContent(@NonNull String jsonContent) {
    return new JsonFhirResource(
        Objects.requireNonNull(jsonContent, "The JSON Content must not e null")
            .getBytes(StandardCharsets.UTF_8));
  }

  public static FhirResource fromPath(@NonNull Path filePath) {
    try (var is =
        Files.newInputStream(
            Objects.requireNonNull(filePath, "The JSON file path must not e null"))) {
      return new JsonFhirResource(is.readAllBytes());
    } catch (IOException e) {
      throw new ParsingException("Could not find the file: " + e.getLocalizedMessage(), e);
    }
  }
}
