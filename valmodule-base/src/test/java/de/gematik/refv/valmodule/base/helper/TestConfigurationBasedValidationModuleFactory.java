package de.gematik.refv.valmodule.base.helper;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.ReferencedProfileLocator;
import de.gematik.refv.commons.configuration.FhirPackageConfigurationLoader;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.commons.validation.GenericValidatorFactory;
import de.gematik.refv.commons.validation.ProfileCacheStrategy;
import de.gematik.refv.commons.validation.SeverityLevelTransformer;
import de.gematik.refv.valmodule.base.ConfigurationBasedValidationModule;
import lombok.SneakyThrows;


public class TestConfigurationBasedValidationModuleFactory {

    @SneakyThrows
    public static ConfigurationBasedValidationModule createInstance(String module) {
        return createInstanceWithCachingStrategy(ProfileCacheStrategy.CACHE_PROFILES, module);
    }

    private static ConfigurationBasedValidationModule createInstanceWithCachingStrategy(ProfileCacheStrategy cacheProfiles, String module) throws ValidationModuleInitializationException {
        GenericValidator engine = new GenericValidator(
                FhirContext.forR4(),
                new ReferencedProfileLocator(),
                new GenericValidatorFactory(),
                new SeverityLevelTransformer(),
                cacheProfiles
        );
        var validationModule = new ConfigurationBasedValidationModule(
                module,
                new FhirPackageConfigurationLoader(),
                engine
        );
        validationModule.initialize();
        return validationModule;
    }

    @SneakyThrows
    public static ConfigurationBasedValidationModule createNonCachingInstance(String module) {
        return createInstanceWithCachingStrategy(ProfileCacheStrategy.NO_CACHE, module);
    }
}
