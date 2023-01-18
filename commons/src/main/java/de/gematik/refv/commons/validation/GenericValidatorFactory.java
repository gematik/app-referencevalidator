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

package de.gematik.refv.commons.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.util.ClasspathUtil;
import ca.uhn.fhir.validation.FhirValidator;
import de.gematik.refv.commons.validation.support.IgnoreMissingValueSetValidationSupport;
import de.gematik.refv.commons.validation.support.PipedCanonicalCoreResourcesValidationSupport;
import lombok.SneakyThrows;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.StructureDefinition;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

public class GenericValidatorFactory {

    @SneakyThrows
    public FhirValidator createInstance(
            FhirContext ctx,
            Collection<String> packageFilenames,
            Collection<String> patches,
            Collection<String> codeSystemsToIgnore) {

        var npmPackageSupport = new NpmPackageValidationSupport(ctx);
        for (String packagePath : packageFilenames) {
            npmPackageSupport.loadPackageFromClasspath("package/" + packagePath);
        }

        for (String patch :
                patches) {
            InputStream is = ClasspathUtil.loadResourceAsStream("package/patches/" + patch);
            var reader = new InputStreamReader(is);
            var newResource = ctx.newJsonParser().parseResource(StructureDefinition.class, reader);
            reader.close();
            npmPackageSupport.addResource(newResource);
        }

        IValidationSupport validationSupport = ctx.getValidationSupport();

            var validationSupportChain = new ValidationSupportChain(
                    npmPackageSupport,
                    validationSupport,
                    new IgnoreMissingValueSetValidationSupport(ctx, codeSystemsToIgnore),
                    new PipedCanonicalCoreResourcesValidationSupport(ctx)
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
