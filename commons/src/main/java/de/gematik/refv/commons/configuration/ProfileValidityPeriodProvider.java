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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.commons.configuration;

import java.time.LocalDate;
import java.util.Optional;

public class ProfileValidityPeriodProvider {

    public Optional<ProfileValidityPeriod> calculateFrom(DependencyListsWrapper dependencyLists) {
        if(dependencyLists.areNoValidityPeriodsDefined())
            return Optional.empty();

        var validityFrom = findMinimalValidityFrom(dependencyLists);
        var validityTill = findMaximalValidityTill(dependencyLists);

        return Optional.of(new ProfileValidityPeriod(validityFrom, validityTill));
    }

    private static Optional<LocalDate> findMaximalValidityTill(DependencyListsWrapper dependencyLists) {
        if(dependencyLists.isAnyDependencyListWithOpenEndExists())
            return Optional.empty();
        else
            return dependencyLists.getLatestValidTill();
    }

    private static Optional<LocalDate> findMinimalValidityFrom(DependencyListsWrapper dependencyLists) {
        if(dependencyLists.isAnyDependencyListWithOpenStartExists())
            throw new IllegalStateException("Unexpected empty validFrom in one of dependency lists");

        return dependencyLists.getEarliestValidFrom();
    }
}
