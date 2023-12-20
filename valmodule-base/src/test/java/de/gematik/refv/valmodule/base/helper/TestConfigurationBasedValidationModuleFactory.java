package de.gematik.refv.valmodule.base.helper;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.commons.validation.IntegratedValidationModule;
import lombok.SneakyThrows;


public class TestConfigurationBasedValidationModuleFactory {

    @SneakyThrows
    public static IntegratedValidationModule createInstance(String module) {
        GenericValidator engine = new GenericValidator(
                FhirContext.forR4()
        );
        var validationModule = new IntegratedValidationModule(
                module,
                engine
        );
        validationModule.initialize();
        return validationModule;
    }
}
