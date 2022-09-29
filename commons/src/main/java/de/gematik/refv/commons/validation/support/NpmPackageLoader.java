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

package de.gematik.refv.commons.validation.support;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NpmPackageLoader {

    public PrePopulatedValidationSupport loadPackagesAndCreatePrePopulatedValidationSupport(FhirContext ctx,
                                                                                            Collection<String> packageFilesToLoad) throws IOException {
        NpmPackageValidationSupportCache npmCache = new NpmPackageValidationSupportCache(ctx);

        List<String> correctedURIs = new ArrayList<>(packageFilesToLoad.size());

        for(String p : packageFilesToLoad) {
            correctedURIs.add("classpath:package/" + p);
        }
        return npmCache.createPrePopulatedValidationSupport(correctedURIs);
    }



}
