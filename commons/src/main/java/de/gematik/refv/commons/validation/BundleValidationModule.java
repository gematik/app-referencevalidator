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

import ca.uhn.fhir.validation.IValidationContext;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.utilities.Utilities;

/**
 * The module is a backport of fullUrl and ID match validation introduced by a <a href="https://github.com/hapifhir/org.hl7.fhir.core/commit/4c6a318749d164466d2bf31918a3969f239c4ca8">version of Java Core Validator</a>, which is later than the one used at the moment.
 * However, the original implementation behaves incorrectly for fullUrls being URNs (cf. BundleValidationModuleTests.validateResourceWithValidUrnFullUrlAndId).
 * Once the original implementation is fixed, this module can be completely removed.
 */
class BundleValidationModule implements IValidatorModule {

    private static final String FULLURL_AND_ID_MISMATCH_CODE = "BUNDLE_ENTRY_URL_MATCHES_TYPE_ID";
    private static final String MESSAGE_TEMPLATE = "The fullUrl ''{0}'' looks like a RESTful server URL, so it must end with the correct type and id (/{1}/{2})";

    @Override
    public void validateResource(IValidationContext<IBaseResource> iValidationContext) {
        var resource = iValidationContext.getResource();
        if(!(resource instanceof Bundle))
            return;

        var bundle = (Bundle)resource;
        var i = 0;
        for(var entry: bundle.getEntry()) {
            String fullUrl = entry.getFullUrl();
            String resourceType = entry.getResource().fhirType();
            String id = entry.getResource().getIdPart();

            if(StringUtils.isBlank(fullUrl) || StringUtils.isBlank(id) || !Utilities.isURL(fullUrl))
                continue;

            if(!fullUrl.endsWith("/"+ resourceType + "/" + id)) {
                SingleValidationMessage message = new SingleValidationMessage();
                message.setSeverity(ResultSeverityEnum.WARNING);
                message.setMessage(MessageFormat.format(MESSAGE_TEMPLATE, fullUrl, resourceType, id));
                message.setMessageId(FULLURL_AND_ID_MISMATCH_CODE);
                message.setLocationString(MessageFormat.format("Bundle.entry[{0}]", i));
                iValidationContext.addValidationMessage(message);
            }

            i++;
        }
    }
}
