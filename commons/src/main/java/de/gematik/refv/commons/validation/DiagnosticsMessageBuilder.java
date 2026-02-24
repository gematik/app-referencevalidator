/*-
 * #%L
 * commons
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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

import de.gematik.refv.commons.Profile;
import de.gematik.refv.commons.configuration.DependencyListsWrapper;
import de.gematik.refv.commons.configuration.ProfileValidityPeriodProvider;
import java.time.LocalDate;

class DiagnosticsMessageBuilder {

    public String createValidityPeriodDiagnosticsString(DependencyListsWrapper dependencyLists, Profile profileInResource, LocalDate resourceCreationDate) {
        ProfileValidityPeriodProvider validityPeriodProvider = new ProfileValidityPeriodProvider();
        var validityPeriodOptional = validityPeriodProvider.calculateFrom(dependencyLists);
        StringBuilder pattern = new StringBuilder("Profile %s is invalid for the creation date of the resource (%s)");

        if(validityPeriodOptional.isEmpty())
            return String.format(pattern.toString(), profileInResource, resourceCreationDate);

        var validityPeriod = validityPeriodOptional.get();

        LocalDate validFrom = validityPeriod.getValidFrom().orElseThrow(() -> new IllegalStateException("Unexpected validity period list without start date detected for profile " + profileInResource));

        pattern.append("; only valid from %s");

        var validTillOptional = validityPeriod.getValidTill();
        if(validTillOptional.isEmpty())
            return String.format(pattern.toString(), profileInResource, resourceCreationDate, validFrom);

        pattern.append(" till %s");

        LocalDate validTill = validTillOptional.get();

        return String.format(pattern.toString(),
                profileInResource, resourceCreationDate,
                validFrom, validTill);
    }

}
