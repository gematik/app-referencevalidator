package de.gematik.refv.valmodule.eau;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.gematik.refv.commons.configuration.FhirPackageConfigurationLoader;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.commons.validation.ValidationResult;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class EauValidationModule implements ValidationModule{

    static Logger logger = LoggerFactory.getLogger(EauValidationModule.class);

    private static final String PACKAGES_YAML = "eau-packages.yaml";
    private static final String CODE = "eau";

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

    public EauValidationModule(FhirPackageConfigurationLoader configurationLoader, GenericValidator genericValidator) {
        this.configurationLoader = configurationLoader;
        this.genericValidator = genericValidator;
    }

    public void initialize() throws ValidationModuleInitializationException {
        try {
            configuration = configurationLoader.getConfiguration(PACKAGES_YAML);
        } catch (IOException e) {
            throw new ValidationModuleInitializationException("Could not load module configuration", e);
        }
    }

    /**
     * Validates the given File
     *
     * @param inputFile String path, not null
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public ValidationResult validateFile(@NonNull String inputFile) throws IllegalArgumentException, IOException {
        return validateFile(Path.of(inputFile));
    }

    /**
     * Validates the given String containing a FHIR resource
     *
     * @param fhirResourceAsString String, not null or empty
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public ValidationResult validateString(@NonNull String fhirResourceAsString) throws IllegalArgumentException {

        return genericValidator.validate(
                fhirResourceAsString,
                getConfiguration()
        );
    }

    /**
     * Validates the given File
     *
     * @param inputPath        String path, not null or empty
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public ValidationResult validateFile(@NonNull Path inputPath) throws IllegalArgumentException, IOException {
        logger.info("Reading input file {}...", inputPath);
        String body = Files.readString(inputPath, StandardCharsets.UTF_8);
        return validateString(body);
    }
}
