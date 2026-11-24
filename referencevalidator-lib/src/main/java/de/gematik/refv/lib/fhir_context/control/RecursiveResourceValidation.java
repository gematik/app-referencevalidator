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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.model.core.OperationOutcome;
import org.hl7.fhir.model.utilities.EOperationOutcome;
import org.hl7.fhir.model.utilities.formats.FhirFormat;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.validation.ValidationEngine;
import org.hl7.fhir.validation.ValidatorUtils;

/**
 * Helper that runs the {@link RecursiveInstanceValidator} and validates Resources with children
 * elements recursively.
 */
final class RecursiveResourceValidation extends ValidatorUtils {
  private RecursiveResourceValidation() {}

  /** Validates a Resource and its children elements recursively. */
  static OperationOutcome validate(
      ValidationEngine engine, FhirFormat format, InputStream input, List<String> profiles)
      throws IOException, EOperationOutcome {
    var configured = engine.getValidator(format);
    var validator =
        new RecursiveInstanceValidator(configured, engine.getDefaultInstanceValidatorParameters());
    var messages = new ArrayList<ValidationMessage>();
    validator.validate(null, messages, input, format, engine.asSdList(profiles));
    return messagesToOutcome(messages, engine.getContext(), validator.getFHIRPathEngine());
  }
}
