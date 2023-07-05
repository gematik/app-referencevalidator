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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PackageCache {
    private FhirContext fhirContext;

    private Map<String, PrePopulatedValidationSupport> packages = new ConcurrentHashMap<>();

    public PackageCache(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }


    public PrePopulatedValidationSupport addOrGet(String packagePath) {
        return packages.computeIfAbsent(packagePath, path -> createPrePopulatedSupportFromPackage(packagePath));
    }

    @SneakyThrows
    private PrePopulatedValidationSupport createPrePopulatedSupportFromPackage(String packagePath) {
        log.info("Loading package " + packagePath + "...");
        NpmPackageValidationSupport validationSupport = new NpmPackageValidationSupport(fhirContext);
        validationSupport.loadPackageFromClasspath("package/" + packagePath);
        return validationSupport;
    }
}
