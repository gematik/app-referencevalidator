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
package de.gematik.refv.cli.report.entity;

import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

public record ReportConfiguration(@NonNull Path filePath, @NonNull Format format) {
  public enum Format {
    HTML,
    JSON
  }

  public ReportConfiguration {
    Objects.requireNonNull(filePath, "The Report File Path must not be null");
    Objects.requireNonNull(format, "The Format must be defined");
  }

  public static @NonNull ReportConfiguration defaultConfiguration() {
    return new ReportConfiguration(Path.of("./report.json"), Format.JSON);
  }
}
