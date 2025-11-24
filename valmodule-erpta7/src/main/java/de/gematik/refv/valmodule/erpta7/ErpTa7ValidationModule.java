/*-
 * #%L
 * valmodule-erpta7
 * %%
 * Copyright (C) 2025 gematik GmbH
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
package de.gematik.refv.valmodule.erpta7;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.validation.ReferencedProfileLocator;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.commons.validation.ValidationModuleResourceProvider;
import de.gematik.refv.commons.validation.ValidationOptions;
import de.gematik.refv.commons.validation.ValidationResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ErpTa7ValidationModule implements ValidationModule {

    public static final String RECHNUNG_BUNDLE = "https://fhir.gkvsv.de/StructureDefinition/GKVSV_PR_TA7_Rechnung_Bundle";
    private final ValidationModuleResourceProvider resourceProvider;
    private final ErpTa7RechnungBundleValidator erpTa7RechnungBundleValidator = new ErpTa7RechnungBundleValidator();

    public String getId() {
        return code;
    }

    @Override
    public ValidationModuleConfiguration getConfiguration() {
        return configuration;
    }

    private final ReferencedProfileLocator referencedProfileLocator = new ReferencedProfileLocator();

    private String code;
    private ValidationModuleConfiguration configuration;


    public ErpTa7ValidationModule(@NonNull ValidationModuleResourceProvider resourceProvider) {
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
        List<String> allProfiles = referencedProfileLocator.getAllReferencedProfilesInResource(fhirResourceAsString);
        if (allProfiles.isEmpty()) {
            throw new IllegalArgumentException("FHIR resources without a referenced profile are currently unsupported");
        }
        var profileInResource = configuration.findFirstSupportedProfileWithExistingConfiguration(allProfiles);
        if(profileInResource == null) {
            throw new IllegalArgumentException(String.format("The erpta7 validation module should only be used to validate %s resources", RECHNUNG_BUNDLE));
        }

        var result = erpTa7RechnungBundleValidator.validateBundleConcurrently(fhirResourceAsString, validationOptions);

        log.debug("ValidationResult: {}", result);

        return result;
    }

    @Override
    public ValidationResult validateFile(Path inputPath, ValidationOptions validationOptions) throws IllegalArgumentException, IOException {
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
