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
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.validation.ValidationContext;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.BaseDateTimeType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@AllArgsConstructor
public class ResourceCreationDateLocator {

    private FhirContext fhirContext;

    public Optional<LocalDate> findCreationDateIn(String resourceBody, String creationDateLocator) {
        var context = ValidationContext.forText(fhirContext, resourceBody, null);

        var fhirPathEngine = fhirContext.newFhirPath();
        var creationDateList = fhirPathEngine.evaluate(context.getResource(), creationDateLocator, BaseDateTimeType.class);
        if(creationDateList == null || creationDateList.isEmpty())
            return Optional.empty();

        if(creationDateList.size() > 1)
            throw new IllegalArgumentException(String.format("Multiple values for creationDate found using expression: '%s'", creationDateLocator));

        var creationDate = creationDateList.get(0).getValue();
        if(creationDate == null) // Date could not be parsed
            throw new DataFormatException(String.format("Could not parse date %s", creationDateList.get(0).toString()));

        return Optional.of(LocalDate.ofInstant(creationDate.toInstant(), ZoneId.systemDefault()));
    }

}
