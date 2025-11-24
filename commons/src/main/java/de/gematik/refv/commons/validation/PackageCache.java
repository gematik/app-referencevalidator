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
import ca.uhn.fhir.context.support.IValidationSupport;
import de.gematik.refv.commons.validation.support.CustomNpmPackageValidationSupport;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class PackageCache {
    private final FhirContext fhirContext;

    private final Map<String, IValidationSupport> packages = new ConcurrentHashMap<>();


    public PackageCache(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public IValidationSupport addOrGet(String packagePath, ValidationModuleResourceProvider validationModuleResourceProvider) {
        return packages.computeIfAbsent(packagePath, path -> createPrePopulatedSupportFromPackage(packagePath, validationModuleResourceProvider));
    }

    @SneakyThrows
    private IValidationSupport createPrePopulatedSupportFromPackage(String packagePath, ValidationModuleResourceProvider validationModuleResourceProvider) {
        return CustomNpmPackageValidationSupport.create(fhirContext, packagePath, validationModuleResourceProvider::getPackage);
    }
}
