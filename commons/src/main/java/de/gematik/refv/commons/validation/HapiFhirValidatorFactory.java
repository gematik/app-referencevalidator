/*-
 * #%L
 * commons
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * *******
 * 
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.commons.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.validation.support.IgnoreCodeSystemValidationSupport;
import de.gematik.refv.commons.validation.support.IgnoreValueSetValidationSupport;
import de.gematik.refv.commons.validation.support.PipedCanonicalCoreResourcesValidationSupport;
import de.gematik.refv.commons.validation.support.UcumValidationSupport;
import java.util.Collection;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;

class HapiFhirValidatorFactory {

    private final FhirContext fhirContext;

    private final PackageCache packageCache;


    public HapiFhirValidatorFactory(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
        packageCache = new PackageCache(fhirContext);
    }

    @SneakyThrows
    public FhirValidator createInstance(
            @NonNull Collection<String> packageFilenames,
            @NonNull ValidationModuleResourceProvider validationModuleResourceProvider) {

        var configuration = validationModuleResourceProvider.getConfiguration();
        var validationSupportChain = createValidationSupportChain(configuration);
        addNpmPackagesToValidationSupportChain(packageFilenames, validationSupportChain, validationModuleResourceProvider);
        return createHapiFhirValidator(configuration, validationSupportChain);
    }

    private FhirValidator createHapiFhirValidator(ValidationModuleConfiguration configuration, ValidationSupportChain validationSupportChain) {
        FhirInstanceValidator hapiValidatorModule = new FhirInstanceValidator(
                validationSupportChain);
        hapiValidatorModule.setErrorForUnknownProfiles(configuration.isErrorOnUnknownProfile());
        hapiValidatorModule.setNoExtensibleWarnings(true);
        hapiValidatorModule.setAnyExtensionsAllowed(configuration.isAnyExtensionsAllowed());
        FhirValidator fhirValidator = fhirContext.newValidator();
        fhirValidator.registerValidatorModule(hapiValidatorModule);
        fhirValidator.registerValidatorModule(new BundleValidationModule());
        return fhirValidator;
    }

    private void addNpmPackagesToValidationSupportChain(Collection<String> packageFilenames, ValidationSupportChain validationSupportChain, ValidationModuleResourceProvider validationModuleResourceProvider) {
        for (String packagePath : packageFilenames) {
            var prePopulatedValidationSupport = packageCache.addOrGet(packagePath, validationModuleResourceProvider);
            validationSupportChain.addValidationSupport(prePopulatedValidationSupport);
        }
    }

    private ValidationSupportChain createValidationSupportChain(ValidationModuleConfiguration configuration) {
        var validationSupport = fhirContext.getValidationSupport();
        return new ValidationSupportChain(
                new UcumValidationSupport(fhirContext, configuration.getUcumValidationSeverityLevel()),
                new SnapshotGeneratingValidationSupport(fhirContext),
                new CachingValidationSupport(new IgnoreCodeSystemValidationSupport(fhirContext, configuration.getIgnoredCodeSystems())),
                new CachingValidationSupport(new IgnoreValueSetValidationSupport(fhirContext, configuration.getIgnoredValueSets())),
                new CachingValidationSupport(validationSupport),
                new PipedCanonicalCoreResourcesValidationSupport(fhirContext)
        );
    }
}
