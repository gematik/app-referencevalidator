package de.gematik.refv.valmodule.eau.helper;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.ReferencedProfileLocator;
import de.gematik.refv.commons.configuration.FhirPackageConfigurationLoader;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.commons.validation.GenericValidatorFactory;
import de.gematik.refv.commons.validation.ProfileCacheStrategy;
import de.gematik.refv.commons.validation.SeverityLevelTransformer;
import de.gematik.refv.valmodule.eau.EauValidationModule;
import lombok.SneakyThrows;

public class TestEauValidationModuleFactory {

    @SneakyThrows
    public static EauValidationModule createInstance() {
        return createInstanceWithCachingStrategy(ProfileCacheStrategy.CACHE_PROFILES);
    }

    private static EauValidationModule createInstanceWithCachingStrategy(ProfileCacheStrategy cacheProfiles) throws ValidationModuleInitializationException {
        GenericValidator engine = new GenericValidator(
                FhirContext.forR4(),
                new ReferencedProfileLocator(),
                new GenericValidatorFactory(),
                new SeverityLevelTransformer(),
                cacheProfiles
        );
        var validationModule = new EauValidationModule(
                new FhirPackageConfigurationLoader(),
                engine
        );
        validationModule.initialize();
        return validationModule;
    }

    @SneakyThrows
    public static EauValidationModule createNonCachingInstance() {
        return createInstanceWithCachingStrategy(ProfileCacheStrategy.NO_CACHE);
    }
}
