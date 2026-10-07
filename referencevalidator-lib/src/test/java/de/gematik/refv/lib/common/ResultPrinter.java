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
package de.gematik.refv.lib.common;

import de.gematik.refv.lib.fhir_context.entity.ResultMessage;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationResult;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper to print result messages */
public final class ResultPrinter {
  private static final Logger log = LoggerFactory.getLogger(ResultPrinter.class);

  private ResultPrinter() {}

  public static void printMessages(@NonNull ValidationResult result) {
    result.messages().forEach(ResultPrinter::showMessage);
  }

  public static void printMessages(@NonNull SnapshotGenerationResult result) {
    result.messages().forEach(ResultPrinter::showMessage);
  }

  private static void showMessage(ResultMessage resultMessage) {
    log.info(
        "Result: {} - {}:  {}",
        resultMessage.severity(),
        resultMessage.messageId(),
        resultMessage.messageContent());
  }
}
