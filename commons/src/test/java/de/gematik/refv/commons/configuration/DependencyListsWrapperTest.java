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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DependencyListsWrapperTest {

    static String dl1Start = "2020-01-01";
    static String dl1End = "2020-01-31";
    static String dl2Start = "2020-02-01";
    static String dl2End = "2020-02-28";
    static DependencyList dl1 = new DependencyList(dl1Start, dl1End, List.of("simplevalidationmodule.test-1.0.0.tgz"), new LinkedList<>());
    static DependencyList dl2 = new DependencyList(dl2Start, dl2End, List.of("simplevalidationmodule.test-1.0.0.tgz"), new LinkedList<>());

    static DependencyListsWrapper wrapper;

    @BeforeAll
    static void beforeAll() {
       wrapper = new DependencyListsWrapper(dl1, dl2);
    }

    @Test
    void testDependencyListValidAtReturnsCorrectResults() {
        Assertions.assertEquals(Optional.of(dl1), wrapper.getDependencyListValidAt(LocalDate.parse(dl1Start)));
        Assertions.assertEquals(Optional.of(dl1), wrapper.getDependencyListValidAt(LocalDate.parse(dl1End)));
        Assertions.assertEquals(Optional.of(dl1), wrapper.getDependencyListValidAt(LocalDate.parse(dl1Start).plusDays(1)));

        Assertions.assertEquals(Optional.of(dl2), wrapper.getDependencyListValidAt(LocalDate.parse(dl2Start)));
        Assertions.assertEquals(Optional.of(dl2), wrapper.getDependencyListValidAt(LocalDate.parse(dl2End)));
        Assertions.assertEquals(Optional.of(dl2), wrapper.getDependencyListValidAt(LocalDate.parse(dl2Start).plusDays(2)));

        Assertions.assertFalse(wrapper.getDependencyListValidAt(LocalDate.parse(dl1Start).minusDays(1)).isPresent());
        Assertions.assertFalse(wrapper.getDependencyListValidAt(LocalDate.parse(dl2End).plusDays(1)).isPresent());
    }

    @Test
    void testDependencyListValidAtReturnsCorrectResultsForOpenEndedLists() {
        DependencyList dl3 = new DependencyList(dl2End, null, List.of("simplevalidationmodule.test-1.0.0.tgz"), new LinkedList<>());
        DependencyListsWrapper wrapper2 = new DependencyListsWrapper(dl1, dl2, dl3);
        Assertions.assertEquals(Optional.of(dl3), wrapper2.getDependencyListValidAt(LocalDate.parse(dl2End).plusDays(1)));
    }

    @Test
    void testDependencyListValidAtThrowsExceptionOnTwoOpenEndedLists() {
        DependencyList dl3 = new DependencyList(dl2End, null, List.of("simplevalidationmodule.test-1.0.0.tgz"), new LinkedList<>());
        DependencyList dl4 = new DependencyList(dl2End, null, List.of("simplevalidationmodule.test-1.0.0.tgz"), new LinkedList<>());
        DependencyListsWrapper wrapper2 = new DependencyListsWrapper(dl1, dl2, dl3, dl4);
        assertThatThrownBy(() -> wrapper2.getDependencyListValidAt(LocalDate.parse(dl2End).plusDays(1))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testGetLatestDependencyListThrowsExceptionOnEmptyDependencyLists() {
        DependencyListsWrapper wrapper2 = new DependencyListsWrapper(new LinkedList<>());
        assertThatThrownBy(() -> wrapper2.getLatestDependencyList()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getLatestDependencyList() {
        Assertions.assertEquals(dl2, wrapper.getLatestDependencyList());
    }

    @Test
    void areNoValidityPeriodsDefined() {
        Assertions.assertFalse(wrapper.areNoValidityPeriodsDefined());

        DependencyList dl1Empty = new DependencyList(null, null, List.of("simplevalidationmodule.test-1.0.0.tgz"), new LinkedList<>());
        DependencyList dl2Empty = new DependencyList(null, null, List.of("simplevalidationmodule.test-1.0.0.tgz"), new LinkedList<>());

        DependencyListsWrapper wrapper2 = new DependencyListsWrapper(dl1Empty, dl2Empty);
        Assertions.assertTrue(wrapper2.areNoValidityPeriodsDefined());
    }

    @Test
    void isAnyDependencyListWithOpenEndExists() {
        Assertions.assertFalse(wrapper.isAnyDependencyListWithOpenEndExists());

        DependencyList dl3 = new DependencyList(dl2End, null, List.of("simplevalidationmodule.test-1.0.0.tgz"), new LinkedList<>());
        DependencyListsWrapper wrapper2 = new DependencyListsWrapper(dl1, dl2, dl3);
        Assertions.assertTrue(wrapper2.isAnyDependencyListWithOpenEndExists());
    }

    @Test
    void isAnyDependencyListWithOpenStartExists() {
        Assertions.assertFalse(wrapper.isAnyDependencyListWithOpenStartExists());

        DependencyList dl3 = new DependencyList(null, dl1Start, List.of("simplevalidationmodule.test-1.0.0.tgz"), new LinkedList<>());
        DependencyListsWrapper wrapper2 = new DependencyListsWrapper(dl3, dl1, dl2);
        Assertions.assertTrue(wrapper2.isAnyDependencyListWithOpenStartExists());
    }

    @Test
    void getLatestValidTill() {
        Assertions.assertEquals(dl2End, wrapper.getLatestValidTill().get().toString());
    }

    @Test
    void getEarliestValidFrom() {
        Assertions.assertEquals(dl1Start, wrapper.getEarliestValidFrom().get().toString());
    }
}