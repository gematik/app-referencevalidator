/*-
 * #%L
 * commons
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
package de.gematik.refv.commons.validation;

import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import java.io.IOException;
import java.nio.file.Path;

public interface ValidationModule {
    String getId();
    ValidationModuleConfiguration getConfiguration();
    ValidationResult validateFile(String inputPath) throws IllegalArgumentException, IOException;
    ValidationResult validateString(String fhirResourceAsString) throws IllegalArgumentException;
    ValidationResult validateFile(Path inputPath) throws IllegalArgumentException, IOException;
    void initialize() throws ValidationModuleInitializationException;

    ValidationResult validateFile(String inputPath, ValidationOptions validationOptions) throws IllegalArgumentException, IOException;
    ValidationResult validateString(String fhirResourceAsString, ValidationOptions validationOptions) throws IllegalArgumentException;
    ValidationResult validateFile(Path inputPath, ValidationOptions validationOptions) throws IllegalArgumentException, IOException;
}
