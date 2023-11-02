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

package de.gematik.refv.commons.validation.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import de.gematik.refv.commons.Profile;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.StructureDefinition;

public class PipedCanonicalCoreResourcesValidationSupport implements IValidationSupport {

    private final FhirContext ctx;

    public PipedCanonicalCoreResourcesValidationSupport(FhirContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public IBaseResource fetchStructureDefinition(String theUrl) {
        Profile p = Profile.parse(theUrl);
        if(p.getVersion() == null)
            return null;

        IValidationSupport support = ctx.getValidationSupport();
        IBaseResource resource = support.fetchStructureDefinition(p.getBaseCanonical());
        if(resource == null)
            return null;

        if(StringUtils.equals(((StructureDefinition) resource).getVersion(), p.getVersion()))
            return resource;

        return null;
    }

    @Override
    public FhirContext getFhirContext() {
        return ctx;
    }
}
