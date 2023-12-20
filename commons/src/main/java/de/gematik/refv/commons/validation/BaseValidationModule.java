/*
 * Copyright (c) 2023 gematik GmbH
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

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class BaseValidationModule implements ValidationModule {

    static Logger logger = LoggerFactory.getLogger(BaseValidationModule.class);
    private final ValidationModuleResourceProvider resourceProvider;

    public String getId() {
        return code;
    }

    @Override
    public ValidationModuleConfiguration getConfiguration() {
        return configuration;
    }

    private String code;
    private ValidationModuleConfiguration configuration;
    private final GenericValidator genericValidator;

    public BaseValidationModule(@NonNull ValidationModuleResourceProvider resourceProvider, GenericValidator genericValidator) {
        this.genericValidator = genericValidator;
        this.resourceProvider = resourceProvider;
    }

    public void initialize() throws ValidationModuleInitializationException {
        try {
            this.configuration = this.resourceProvider.getConfiguration();
            this.code = this.configuration.getId();
        } catch (Exception e) {
            throw new ValidationModuleInitializationException("Initialization failed", e);
        }
    }

    @Override
    public ValidationResult validateFile(@NonNull String inputFile, ValidationOptions validationOptions) throws IllegalArgumentException, IOException {
        return validateFile(Path.of(inputFile), validationOptions);
    }

    @Override
    public ValidationResult validateString(String fhirResourceAsString, ValidationOptions validationOptions) {
        ValidationResult result = genericValidator.validate(
                fhirResourceAsString,
                resourceProvider,
                validationOptions
        );

        logger.debug("ValidationResult: {}", result);

        return result;
    }

    @Override
    public ValidationResult validateFile(Path inputPath, ValidationOptions validationOptions) throws IllegalArgumentException, IOException {
        logger.info("Reading input file {}...", inputPath);
        String body = Files.readString(inputPath, StandardCharsets.UTF_8);
        return validateString(body, validationOptions);
    }

    /**
     * Validates the given File
     *
     * @param inputFile String path, not null
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public ValidationResult validateFile(@NonNull String inputFile) throws IllegalArgumentException, IOException {
        return validateFile(inputFile, ValidationOptions.getDefaults());
    }

    /**
     * Validates the given String containing a FHIR resource
     *
     * @param fhirResourceAsString String, not null or empty
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public ValidationResult validateString(@NonNull String fhirResourceAsString) throws IllegalArgumentException {
        return validateString(fhirResourceAsString, ValidationOptions.getDefaults());
    }

    /**
     * Validates the given File
     *
     * @param inputPath        String path, not null or empty
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public ValidationResult validateFile(@NonNull Path inputPath) throws IllegalArgumentException, IOException {
        return validateFile(inputPath, ValidationOptions.getDefaults());
    }

}
