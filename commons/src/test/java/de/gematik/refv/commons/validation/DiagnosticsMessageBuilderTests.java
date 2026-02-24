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
import de.gematik.refv.commons.configuration.DependencyList;
import de.gematik.refv.commons.configuration.DependencyListsWrapper;
import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiagnosticsMessageBuilderTests {

    private Profile profileInResource;
    private LocalDate localDate;
    private DiagnosticsMessageBuilder diagnosticsMessageBuilder;

    @BeforeEach
    void setUp() {
        profileInResource = new Profile("http://profile/in/resource", "http://profile/", "1.0.0");
        localDate = LocalDate.of(1999, 1, 1);
        diagnosticsMessageBuilder = new DiagnosticsMessageBuilder();
    }

    @Test
    void testCreateValidityPeriodDiagnosticsString() {
        DependencyList dependencyList = new DependencyList("1999-01-01", "2000-01-01", new ArrayList<>(), new ArrayList<>());
        DependencyListsWrapper dependencyListsWrapper = new DependencyListsWrapper(dependencyList);
        String diagnostics = diagnosticsMessageBuilder.
                createValidityPeriodDiagnosticsString(dependencyListsWrapper, profileInResource, localDate);
        Assertions.assertEquals("Profile http://profile/in/resource is invalid for the creation date of the resource (1999-01-01); only valid from 1999-01-01 till 2000-01-01", diagnostics);
    }

    @Test
    void testCreateValidityPeriodDiagnosticsStringValidityPeriodOptionalIsEmpty() {
        DependencyListsWrapper dependencyListsWrapper = new DependencyListsWrapper(new ArrayList<>());
        String diagnostics = diagnosticsMessageBuilder.
                createValidityPeriodDiagnosticsString(dependencyListsWrapper, profileInResource, localDate);
        Assertions.assertEquals("Profile http://profile/in/resource is invalid for the creation date of the resource (1999-01-01)", diagnostics);
    }

    @Test
    void testCreateValidityPeriodDiagnosticsStringValidFromIsEmptyThrowsIllegalStateException() {
        DependencyList dependencyList = new DependencyList(null, "2000-01-01", new ArrayList<>(), new ArrayList<>());
        DependencyListsWrapper dependencyListsWrapper = new DependencyListsWrapper(dependencyList);
        Assertions.assertThrows(IllegalStateException.class, () -> diagnosticsMessageBuilder
                .createValidityPeriodDiagnosticsString(dependencyListsWrapper, profileInResource, localDate));
    }

    @Test
    void testCreateValidityPeriodDiagnosticsStringValidTillIsEmpty() {
        DependencyList dependencyList = new DependencyList("1999-01-01", null, new ArrayList<>(), new ArrayList<>());
        DependencyListsWrapper dependencyListsWrapper = new DependencyListsWrapper(dependencyList);
        String diagnostics = diagnosticsMessageBuilder.
                createValidityPeriodDiagnosticsString(dependencyListsWrapper, profileInResource, localDate);
        Assertions.assertEquals("Profile http://profile/in/resource is invalid for the creation date of the resource (1999-01-01); only valid from 1999-01-01", diagnostics);
    }
}
