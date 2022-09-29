/*
 * Copyright (c) 2022 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.refv.commons.validation;

import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Path;

/**
 * <p>ValidationModule is currently NOT threadsafe, but it can be reused for validating
 * further FHIR resources in the same or another thread.</p>
 */
public interface ValidationModule {
    String getId();
    ValidationModuleConfiguration getConfiguration();
    ValidationResult validateFile(@NonNull String inputPath) throws IllegalArgumentException, IOException;
    ValidationResult validateString(@NonNull String fhirResourceAsString) throws IllegalArgumentException;
    ValidationResult validateFile(Path inputPath) throws IllegalArgumentException, IOException;

}
