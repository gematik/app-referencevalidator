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

package de.gematik.refv;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.refv.commons.ReferencedProfileLocator;
import de.gematik.refv.commons.configuration.FhirPackageConfigurationLoader;
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.commons.validation.GenericValidatorFactory;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.commons.validation.ProfileCacheStrategy;
import de.gematik.refv.commons.validation.SeverityLevelTransformer;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.valmodule.base.ConfigurationBasedValidationModule;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.IOException;

@NoArgsConstructor
public class ValidationModuleFactory {

    public ValidationModule createValidationModule(@NonNull SupportedValidationModule module) throws IllegalArgumentException, IOException, ValidationModuleInitializationException {
        GenericValidator engine = new GenericValidator(
                FhirContext.forR4(),
                new ReferencedProfileLocator(),
                new GenericValidatorFactory(),
                new SeverityLevelTransformer(),
                ProfileCacheStrategy.CACHE_PROFILES
        );

        ValidationModule result = new ConfigurationBasedValidationModule(
                module.toString(),
                new FhirPackageConfigurationLoader(),
                engine
        );
        result.initialize();
        return result;
    }
}
