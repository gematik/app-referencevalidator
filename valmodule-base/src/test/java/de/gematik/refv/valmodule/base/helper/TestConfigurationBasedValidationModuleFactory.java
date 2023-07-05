package de.gematik.refv.valmodule.base.helper;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.configuration.FhirPackageConfigurationLoader;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.valmodule.base.ConfigurationBasedValidationModule;
import lombok.SneakyThrows;


public class TestConfigurationBasedValidationModuleFactory {

    @SneakyThrows
    public static ConfigurationBasedValidationModule createInstance(String module) {
        GenericValidator engine = new GenericValidator(
                FhirContext.forR4()
        );
        var validationModule = new ConfigurationBasedValidationModule(
                module,
                new FhirPackageConfigurationLoader(),
                engine
        );
        validationModule.initialize();
        return validationModule;
    }
}
