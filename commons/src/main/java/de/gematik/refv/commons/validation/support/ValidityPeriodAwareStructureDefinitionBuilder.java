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

import de.gematik.refv.commons.configuration.ProfileValidityPeriod;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.StructureDefinition;

import java.time.LocalDate;

@Slf4j
public class ValidityPeriodAwareStructureDefinitionBuilder {

    public static final String CONSTRAINT_KEY_REFVAL_PROFILE_DATE_VALIDITY_CHECK = "refval-profile-date-validity-check";

    void putValidityPeriodCosntraintsOn(StructureDefinition structureDefinition, String creationDateLocator, ProfileValidityPeriod profileValidityPeriod) {
        String sdType = structureDefinition.getType();
        var firstElementOfSnapshot = structureDefinition.getSnapshot().getElement().stream().filter(e -> e.getId().equals(sdType)).findFirst();
        if(firstElementOfSnapshot.isEmpty())
            throw new IllegalArgumentException("Could not find snapshot element to add a constraint to");

        var firstElement = firstElementOfSnapshot.get();

        var validFromOptional = profileValidityPeriod.getValidFrom();
        if(validFromOptional.isEmpty())
            throw new IllegalArgumentException("Profile validity periods without start date are unsupported");

        LocalDate validFrom = validFromOptional.get();
        LocalDate validTill = profileValidityPeriod.getValidTill().orElse(LocalDate.of(3000,1,1));

        String expressionForDate = String.format("%s.toString() >= '%s' and %s.toString() <= '%s'",
                creationDateLocator, validFrom, creationDateLocator, validTill);

        log.debug("Adding constraint '{}' to profile {}|{}}...", expressionForDate, structureDefinition.getUrl(), structureDefinition.getVersion());
        var constraint = new ElementDefinition.ElementDefinitionConstraintComponent();
        constraint.
                setKey(CONSTRAINT_KEY_REFVAL_PROFILE_DATE_VALIDITY_CHECK).
                setExpression(expressionForDate).
                setSeverity(ElementDefinition.ConstraintSeverity.ERROR).
                setHuman(String.format("Profile %s|%s is invalid for the resource creation time. Expression: %s", structureDefinition.getUrl(), structureDefinition.getVersion(), expressionForDate));
        firstElement.addConstraint(constraint);
        log.debug("Adding constraint '{}' to profile {}|{}}...", expressionForDate, structureDefinition.getUrl(), structureDefinition.getVersion());
    }

}
