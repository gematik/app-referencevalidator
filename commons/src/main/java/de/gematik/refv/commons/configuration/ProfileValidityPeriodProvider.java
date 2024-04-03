/*
Copyright (c) 2022-2024 gematik GmbH

Licensed under the Apache License, Version 2.0 (the License);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package de.gematik.refv.commons.configuration;

import lombok.Data;

import java.util.Optional;

@Data
public class ProfileValidityPeriodProvider {

    public Optional<ProfileValidityPeriod> calculateFrom(DependencyListsWrapper dependencyLists) {
        ProfileValidityPeriod validityPeriod = new ProfileValidityPeriod();

        if(dependencyLists.areNoValidityPeriodsDefined())
            return Optional.empty();

        findMinimalValidityFrom(dependencyLists, validityPeriod);
        findMaximalValidityTill(dependencyLists, validityPeriod);

        return Optional.of(validityPeriod);
    }

    private static void findMaximalValidityTill(DependencyListsWrapper dependencyLists, ProfileValidityPeriod validityPeriod) {
        if(dependencyLists.isAnyDependencyListWithOpenEndExists())
            validityPeriod.setValidTill(Optional.empty());
        else
            validityPeriod.setValidTill(dependencyLists.getLatestValidTill());
    }

    private static void findMinimalValidityFrom(DependencyListsWrapper dependencyLists, ProfileValidityPeriod validityPeriod) {
        if(dependencyLists.isAnyDependencyListWithOpenStartExists())
            throw new IllegalStateException("Unexpected empty validFrom in one of dependency lists");

        validityPeriod.setValidFrom(dependencyLists.getEarliestValidFrom());
    }
}
