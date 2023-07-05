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
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.validation.support.IgnoreCodeSystemValidationSupport;
import de.gematik.refv.commons.validation.support.IgnoreValueSetValidationSupport;
import de.gematik.refv.commons.validation.support.PipedCanonicalCoreResourcesValidationSupport;
import lombok.SneakyThrows;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;


public class HapiFhirValidatorFactory {

    private FhirContext fhirContext;

    private PackageCache packageCache;
    static Logger logger = LoggerFactory.getLogger(HapiFhirValidatorFactory.class);

    public HapiFhirValidatorFactory(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
        packageCache = new PackageCache(fhirContext);
    }
    @SneakyThrows
    public FhirValidator createInstance(
            Collection<String> packageFilenames,
            Collection<String> patches,
            ValidationModuleConfiguration configuration) {
        Collection<String> codeSystemsToIgnore = configuration.getIgnoredCodeSystems();
        Collection<String> valueSetsToIgnore = configuration.getIgnoredValueSets();
        boolean errorOnUnknownProfile = configuration.isErrorOnUnknownProfile();
        boolean anyExtensionsAllowed = configuration.isAnyExtensionsAllowed();

        var patchesPrePopulatedValidationSupport = new PrePopulatedValidationSupport(fhirContext);
        for (String patch :
                patches) {
            logger.debug("Applying patch {}...",patch);
            InputStream is = ClasspathUtil.loadResourceAsStream("package/patches/" + patch);
            var reader = new InputStreamReader(is);
            var newResource = fhirContext.newJsonParser().parseResource(reader);
            reader.close();
            patchesPrePopulatedValidationSupport.addResource(newResource);
        }

        IValidationSupport validationSupport = fhirContext.getValidationSupport();

        var validationSupportChain = new ValidationSupportChain(
                new IgnoreCodeSystemValidationSupport(fhirContext, codeSystemsToIgnore),
                new IgnoreValueSetValidationSupport(fhirContext, valueSetsToIgnore),
                validationSupport,
                new PipedCanonicalCoreResourcesValidationSupport(fhirContext),
                patchesPrePopulatedValidationSupport
        );

        for (String packagePath : packageFilenames) {
            PrePopulatedValidationSupport prePopulatedValidationSupport = packageCache.addOrGet(packagePath);
            validationSupportChain.addValidationSupport(prePopulatedValidationSupport);
        }

        FhirInstanceValidator hapiValidatorModule = new FhirInstanceValidator(
                validationSupportChain);
        hapiValidatorModule.setErrorForUnknownProfiles(errorOnUnknownProfile);
        hapiValidatorModule.setNoExtensibleWarnings(true);
        hapiValidatorModule.setAnyExtensionsAllowed(anyExtensionsAllowed);
        FhirValidator fhirValidator = fhirContext.newValidator();
        fhirValidator.registerValidatorModule(hapiValidatorModule);
        return fhirValidator;
    }
}
