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
import de.gematik.refv.commons.ReferencedProfileLocator;
import de.gematik.refv.commons.configuration.FhirPackageConfigurationLoader;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.commons.validation.GenericValidatorFactory;
import de.gematik.refv.commons.validation.ProfileCacheStrategy;
import de.gematik.refv.commons.validation.SeverityLevelTransformer;
import de.gematik.refv.commons.validation.ValidationModule;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimpleValidationModule  implements ValidationModule {

    static Logger logger = LoggerFactory.getLogger(SimpleValidationModule.class);

    private static final String CODE = "simple";

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
    public static SimpleValidationModule createInstance() {
        GenericValidator engine = new GenericValidator(
                FhirContext.forR4(),
                new ReferencedProfileLocator(),
                new GenericValidatorFactory(),
                new SeverityLevelTransformer(),
                ProfileCacheStrategy.CACHE_PROFILES
        );
        var validationModule = new SimpleValidationModule(
                new FhirPackageConfigurationLoader(),
                engine
        );
        validationModule.initialize();
        return validationModule;
    }

    private SimpleValidationModule(FhirPackageConfigurationLoader configurationLoader, GenericValidator genericValidator) {
        this.configurationLoader = configurationLoader;
        this.genericValidator = genericValidator;
    }

    public void initialize() throws ValidationModuleInitializationException {
        try {
            configuration = configurationLoader.getConfiguration();
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

        return genericValidator.validate(
                fhirResourceAsString,
                getConfiguration()
        );
    }

    /**
     * Validates the given File
     *
     * @param inputPath String path, not null or empty
     * @return Map of {@link ResultSeverityEnum} as key and a List of {@link SingleValidationMessage} as key
     */
    public de.gematik.refv.commons.validation.ValidationResult validateFile(@NonNull Path inputPath) throws IllegalArgumentException, IOException {
        logger.info("Reading input file {}...", inputPath);
        String body = Files.readString(inputPath, StandardCharsets.UTF_8);
        return validateString(body);
    }
}
