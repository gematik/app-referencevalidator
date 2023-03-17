package de.gematik.refv.valmodule.base;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.ReferencedProfileLocator;
import de.gematik.refv.commons.configuration.FhirPackageConfigurationLoader;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.commons.validation.GenericValidatorFactory;
import de.gematik.refv.commons.validation.ProfileCacheStrategy;
import de.gematik.refv.commons.validation.SeverityLevelTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class ConfigurationBasedValidationModuleTests {

    private static GenericValidator engine;

    @BeforeAll
    static void beforeAll() {
        engine = new GenericValidator(
                FhirContext.forR4(),
                new ReferencedProfileLocator(),
                new GenericValidatorFactory(),
                new SeverityLevelTransformer(),
                ProfileCacheStrategy.CACHE_PROFILES
        );
    }

    @Test
    void testValidateFileValid() throws IOException, ValidationModuleInitializationException {
        ConfigurationBasedValidationModule validationModule = new ConfigurationBasedValidationModule(
                "minimal",
                new FhirPackageConfigurationLoader(),
                engine
        );
        validationModule.initialize();
        var result = validationModule.validateFile("src/test/resources/test-minimal-example-valid.xml");
        Assertions.assertTrue(result.isValid(), result.toString());
    }

    @Test
    void testValidateStringValid() throws IOException, ValidationModuleInitializationException {
        ConfigurationBasedValidationModule validationModule = new ConfigurationBasedValidationModule(
                "minimal",
                new FhirPackageConfigurationLoader(),
                engine
        );
        validationModule.initialize();
        String fhirResourceAsString = Files.readString(Path.of("src/test/resources/test-minimal-example-valid.xml"), StandardCharsets.UTF_8);
        var result = validationModule.validateString(fhirResourceAsString);
        Assertions.assertTrue(result.isValid(), result.toString());
    }

    @Test
    void testValidateFileWithPathValid() throws IOException, ValidationModuleInitializationException {
        ConfigurationBasedValidationModule validationModule = new ConfigurationBasedValidationModule(
                "minimal",
                new FhirPackageConfigurationLoader(),
                engine
        );
        validationModule.initialize();
        var result = validationModule.validateFile(Path.of("src/test/resources/test-minimal-example-valid.xml"));
        Assertions.assertTrue(result.isValid(), result.toString());
    }

    @Test
    void testValidateFileInvalid() throws IOException, ValidationModuleInitializationException {
        ConfigurationBasedValidationModule validationModule = new ConfigurationBasedValidationModule(
                "minimal",
                new FhirPackageConfigurationLoader(),
                engine
        );
        validationModule.initialize();
        var result = validationModule.validateFile("src/test/resources/test-minimal-example-invalid.xml");
        Assertions.assertFalse(result.isValid(), result.toString());
    }

    @Test
    void testInitialize() {
        ConfigurationBasedValidationModule validationModule = new ConfigurationBasedValidationModule(
                "minimal",
                new FhirPackageConfigurationLoader(),
                engine
        );
        Assertions.assertDoesNotThrow(validationModule::initialize);
    }

    @Test
    void testCreateInstanceWithNonExistingModule() {
        FhirPackageConfigurationLoader fhirPackageConfigurationLoader = new FhirPackageConfigurationLoader();
        ConfigurationBasedValidationModule validationModule = new ConfigurationBasedValidationModule(
                "non-existing-module",
                fhirPackageConfigurationLoader,
                engine
        );
        Assertions.assertThrows(ValidationModuleInitializationException.class, validationModule::initialize);
    }
}
