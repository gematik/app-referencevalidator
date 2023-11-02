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

package de.gematik.refv.commons.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.Optional;

class ProfileValidityPeriodProviderTests {

    ProfileValidityPeriodProvider validityPeriodProvider = new ProfileValidityPeriodProvider();
    @Test
    void testDependencyListsWithNonEmptyValidFromAndValidTillAreProcessedCorrectly() {
        DependencyList dl1 = new DependencyList("2020-01-01", "2020-02-28", new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
        DependencyList dl2 = new DependencyList("2020-03-01", "2020-04-30", new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
        DependencyList dl3 = new DependencyList("2020-04-30", "2020-05-31", new LinkedList<>(), new LinkedList<>(), new LinkedList<>());

        var result = validityPeriodProvider.calculateFrom(new DependencyListsWrapper(dl1, dl2, dl3));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Optional.of(LocalDate.parse("2020-01-01")), result.get().validFrom);
        Assertions.assertEquals(Optional.of(LocalDate.parse("2020-05-31")), result.get().validTill);
    }

    @Test
    void testOneDependencyListWithNonEmptyValidFromAndValidTillAreProcessedCorrectly() {
        DependencyList dl1 = new DependencyList("2020-01-01", "2020-02-28", new LinkedList<>(), new LinkedList<>(), new LinkedList<>());

        var result = validityPeriodProvider.calculateFrom(new DependencyListsWrapper(dl1));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Optional.of(LocalDate.parse("2020-01-01")), result.get().validFrom);
        Assertions.assertEquals(Optional.of(LocalDate.parse("2020-02-28")), result.get().validTill);
    }

    @Test
    void testDependencyListsWithEmptyValidTillAreProcessedCorrectly() {
        DependencyList dl1 = new DependencyList("2020-01-01", "2020-02-28", new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
        DependencyList dl2 = new DependencyList("2020-03-01", "2020-04-30", new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
        DependencyList dl3 = new DependencyList("2020-04-30", null, new LinkedList<>(), new LinkedList<>(), new LinkedList<>());

        var result = validityPeriodProvider.calculateFrom(new DependencyListsWrapper(dl1, dl2, dl3));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Optional.of(LocalDate.parse("2020-01-01")), result.get().validFrom);
        Assertions.assertEquals(Optional.empty(), result.get().validTill);
    }

    @Test
    void testDependencyListsWithoutValidityDatesAreProcessedCorrectly() {
        DependencyList dl1 = new DependencyList(null, null, new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
        DependencyList dl2 = new DependencyList(null, null, new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
        DependencyList dl3 = new DependencyList(null, null, new LinkedList<>(), new LinkedList<>(), new LinkedList<>());

        var result = validityPeriodProvider.calculateFrom(new DependencyListsWrapper(dl1, dl2, dl3));

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    void testDependencyListsWithMissingValueFromThrowException() {
        DependencyList dl1 = new DependencyList(null, "2020-01-31", new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
        DependencyList dl2 = new DependencyList("2020-02-01", "2020-02-28", new LinkedList<>(), new LinkedList<>(), new LinkedList<>());

        var dependencyLists = new DependencyListsWrapper(dl1, dl2);
        Assertions.assertThrows(IllegalStateException.class, () -> validityPeriodProvider.calculateFrom(dependencyLists));
    }
}
