package de.gematik.refv.valmodule.base;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationBasedValidationModule implements ValidationModule {

    static Logger logger = LoggerFactory.getLogger(ConfigurationBasedValidationModule.class);

    public String getId() {
        return code;
    }

    @Override
    public ValidationModuleConfiguration getConfiguration() {
        return configuration;
    }

    private final String code;
    private final FhirPackageConfigurationLoader configurationLoader;
    private ValidationModuleConfiguration configuration;
    private final GenericValidator genericValidator;

    public ConfigurationBasedValidationModule(String code, FhirPackageConfigurationLoader configurationLoader, GenericValidator genericValidator) {
        this.code = code;
        this.configurationLoader = configurationLoader;
        this.genericValidator = genericValidator;
    }

    public void initialize() throws ValidationModuleInitializationException {
        String configurationFile = String.format("%s-packages.yaml", code);
        try {
            configuration = configurationLoader.getConfiguration(configurationFile);
        } catch (IOException e) {
            throw new ValidationModuleInitializationException("Could not load module configuration", e);
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
                getConfiguration(),
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
