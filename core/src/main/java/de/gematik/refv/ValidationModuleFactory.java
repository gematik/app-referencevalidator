/*
 * Copyright (c) 2022 gematik GmbH
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
import de.gematik.refv.commons.validation.SeverityLevelTransformator;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.valmodule.erp.ErpValidationModule;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor
public class ValidationModuleFactory {
    static Logger logger = LoggerFactory.getLogger(ValidationModuleFactory.class);

    public ValidationModule createValidationModule(@NonNull SupportedValidationModule module) throws IllegalArgumentException, ValidationModuleInitializationException {
        if(!SupportedValidationModule.ERP.equals(module))
            throw new IllegalArgumentException("Unsupported validation module: " + module);

        GenericValidator engine = new GenericValidator(
                FhirContext.forR4(),
                new ReferencedProfileLocator(),
                new GenericValidatorFactory(),
                new SeverityLevelTransformator()
        );
        var erpValidationModule = new ErpValidationModule(
                new FhirPackageConfigurationLoader(),
                engine
        );
        erpValidationModule.initialize();
        return erpValidationModule;
    }
}
