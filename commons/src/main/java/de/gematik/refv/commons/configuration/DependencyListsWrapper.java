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
package de.gematik.refv.commons.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class DependencyListsWrapper {

    @Getter
    private List<DependencyList> dependencyLists;

    public DependencyListsWrapper(DependencyList...  lists) {
        dependencyLists = List.of(lists);
    }

    public List<DependencyList> getDependencyListsValidAt(LocalDate resourceCreationDate) {
        return dependencyLists.stream().filter(dl -> dl.isValidFor(resourceCreationDate)).collect(Collectors.toList());
    }

    public DependencyList getLatestDependencyList() {
        var openEndDependencyList = dependencyLists.stream().filter(dl -> dl.getValidTill() == null).findFirst();
        if(openEndDependencyList.isPresent())
            return openEndDependencyList.get();

        var firstSortedOptional = dependencyLists.stream().min((o1, o2) -> {
            LocalDate validTill1 = LocalDate.parse(o1.getValidTill());
            LocalDate validTill2 = LocalDate.parse(o2.getValidTill());
            if (validTill2.isAfter(validTill1))
                return 1;
            else if (validTill2.isBefore(validTill1))
                return -1;
            else
                return 0;
        });

        if(firstSortedOptional.isPresent())
            return firstSortedOptional.get();

        throw new IllegalStateException("Could not determine latest dependency list");
    }

    public boolean areNoValidityPeriodsDefined() {
        return dependencyLists.stream().allMatch(dl -> dl.getValidFrom() == null && dl.getValidTill() == null);
    }

    public boolean isAnyDependencyListWithOpenEndExists() {
        return dependencyLists.stream().anyMatch(dl -> dl.getValidTill() == null);
    }

    public boolean isAnyDependencyListWithOpenStartExists() {
        return dependencyLists.stream().anyMatch(dl -> dl.getValidFrom() == null);
    }

    public Optional<LocalDate> getLatestValidTill() {
        var dependencyListOptional = dependencyLists.stream().filter(dl -> dl.getValidTill() != null).min((o1, o2) -> {
            LocalDate d1 = LocalDate.parse(o1.getValidTill());
            LocalDate d2 = LocalDate.parse(o2.getValidTill());
            return d2.compareTo(d1);
        });
        return dependencyListOptional.map(dependencyList -> LocalDate.parse(dependencyList.getValidTill()));

    }

    public Optional<LocalDate> getEarliestValidFrom() {
        var dependencyListsSortedOptional = dependencyLists.stream().filter(dl -> dl.getValidFrom() != null).min((o1, o2) -> {
            LocalDate d1 = LocalDate.parse(o1.getValidFrom());
            LocalDate d2 = LocalDate.parse(o2.getValidFrom());
            return d1.compareTo(d2);
        });
        return dependencyListsSortedOptional.map(dependencyList -> LocalDate.parse(dependencyList.getValidFrom()));

    }
}
