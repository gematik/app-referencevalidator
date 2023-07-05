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

package de.gematik.refv.commons.helper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.gematik.refv.commons.configuration.FhirPackageConfigurationLoader;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.commons.validation.ValidationOptions;
import de.gematik.refv.commons.validation.ValidationResult;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class SimpleValidationModule implements ValidationModule {

    private static final String CONFIGURATION_FILE = "simple-packages.yaml";
    private static final String CODE = "simple";

    private final String configurationFile;

    public String getId() {
        return CODE;
    }

    @Override
    public ValidationModuleConfiguration getConfiguration() {
        return configuration;
    }

    private final FhirPackageConfigurationLoader configurationLoader;

    private ValidationModuleConfiguration configuration;

    private final GenericValidator genericValidator;

    @SneakyThrows
    public static SimpleValidationModule createInstance(String configFile) {
        FhirContext fhirContext = FhirContext.forR4();
        GenericValidator engine = new GenericValidator(
                fhirContext
        );
        var validationModule = new SimpleValidationModule(
                new FhirPackageConfigurationLoader(),
                engine,
                configFile
        );
        validationModule.initialize();
        return validationModule;
    }

    private SimpleValidationModule(FhirPackageConfigurationLoader configurationLoader, GenericValidator genericValidator, String configurationFile) {
        this.configurationLoader = configurationLoader;
        this.genericValidator = genericValidator;
        this.configurationFile = configurationFile;
    }

    public void initialize() throws ValidationModuleInitializationException {
        try {
            configuration = configurationLoader.getConfiguration(configurationFile);
        } catch (IOException e) {
            throw new ValidationModuleInitializationException("Could not load module configuration", e);
        }
    }

    @Override
    public ValidationResult validateFile(@NonNull String inputPath, @NonNull ValidationOptions validationOptions) throws IllegalArgumentException {
        return validateFile(Paths.get(inputPath), validationOptions);
    }

    @Override
    public ValidationResult validateString(String fhirResourceAsString, @NonNull ValidationOptions validationOptions) {
        var result = genericValidator.validate(
                fhirResourceAsString,
                getConfiguration(),
                validationOptions);
        log.info("Validation result: {}", result);
        return result;
    }

    @Override
    @SneakyThrows
    public ValidationResult validateFile(Path inputPath, @NonNull ValidationOptions validationOptions) {
        log.info("Reading input file {}...", inputPath);
        String body = Files.readString(inputPath, StandardCharsets.UTF_8);
        return validateString(body, validationOptions);
    }

    /**
     * Validates the given File
     *
     * @param inputFile String path, not null
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public de.gematik.refv.commons.validation.ValidationResult validateFile(@NonNull String inputFile) throws IllegalArgumentException, IOException {
        return validateFile(Path.of(inputFile));
    }

    /**
     * Validates the given String containing a FHIR resource
     *
     * @param fhirResourceAsString String, not null or empty
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public de.gematik.refv.commons.validation.ValidationResult validateString(@NonNull String fhirResourceAsString) throws IllegalArgumentException {

        return validateString(fhirResourceAsString, ValidationOptions.getDefaults());
    }

    /**
     * Validates the given File
     *
     * @param inputPath String path, not null or empty
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public de.gematik.refv.commons.validation.ValidationResult validateFile(@NonNull Path inputPath) throws IllegalArgumentException, IOException {
        return validateFile(inputPath, ValidationOptions.getDefaults());
    }
}
