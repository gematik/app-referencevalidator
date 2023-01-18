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

package de.gematik.refv.valmodule.erp.helper;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.ReferencedProfileLocator;
import de.gematik.refv.commons.configuration.FhirPackageConfigurationLoader;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.validation.GenericValidatorFactory;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.commons.validation.ProfileCacheStrategy;
import de.gematik.refv.commons.validation.SeverityLevelTransformer;
import de.gematik.refv.valmodule.erp.ErpValidationModule;
import lombok.SneakyThrows;

public class TestErpValidationModuleFactory {

    @SneakyThrows
    public static ErpValidationModule createInstance() {
        return createInstanceWithCachingStrategy(ProfileCacheStrategy.CACHE_PROFILES);
    }

    private static ErpValidationModule createInstanceWithCachingStrategy(ProfileCacheStrategy cacheProfiles) throws ValidationModuleInitializationException {
        GenericValidator engine = new GenericValidator(
                FhirContext.forR4(),
                new ReferencedProfileLocator(),
                new GenericValidatorFactory(),
                new SeverityLevelTransformer(),
                cacheProfiles
        );
        var validationModule = new ErpValidationModule(
                new FhirPackageConfigurationLoader(),
                engine
        );
        validationModule.initialize();
        return validationModule;
    }

    @SneakyThrows
    public static ErpValidationModule createNonCachingInstance() {
        return createInstanceWithCachingStrategy(ProfileCacheStrategy.NO_CACHE);
    }
}
