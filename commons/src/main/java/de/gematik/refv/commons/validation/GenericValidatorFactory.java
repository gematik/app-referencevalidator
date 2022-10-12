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

package de.gematik.refv.commons.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import de.gematik.refv.commons.validation.support.NpmPackageLoader;
import de.gematik.refv.commons.validation.support.FixedSnapshotGeneratingValidationSupport;
import de.gematik.refv.commons.validation.support.IgnoreMissingValueSetValidationSupport;
import lombok.SneakyThrows;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;

import java.util.Collection;

public class GenericValidatorFactory {

        @SneakyThrows
    public FhirValidator createInstance(
            FhirContext ctx,
            Collection<String> packageFilenames,
            Collection<String> codeSystemsToIgnore
            )  {
            var npmPackageSupport = new NpmPackageLoader(ctx).loadPackagesAndCreatePrePopulatedValidationSupport(packageFilenames);

            IValidationSupport validationSupport = ctx.getValidationSupport();

            var validationSupportChain = new ValidationSupportChain(
                    npmPackageSupport,
                    validationSupport,
                    new FixedSnapshotGeneratingValidationSupport(ctx),
                    new IgnoreMissingValueSetValidationSupport(ctx, codeSystemsToIgnore)
            );

            FhirInstanceValidator hapiValidatorModule = new FhirInstanceValidator(
                    validationSupportChain);
            hapiValidatorModule.setErrorForUnknownProfiles(true);
            hapiValidatorModule.setNoExtensibleWarnings(true);
            hapiValidatorModule.setAnyExtensionsAllowed(false);
            FhirValidator fhirValidator = ctx.newValidator();
            fhirValidator.registerValidatorModule(hapiValidatorModule);
            return fhirValidator;
        }
}
