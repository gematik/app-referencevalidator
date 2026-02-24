/*-
 * #%L
 * referencevalidator-lib
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
package de.gematik.refv.plugins.configuration.v2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.gematik.refv.plugins.configuration.DependencyList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationSection {
    @NonNull
    private List<DependencyList> dependencyLists;
    private boolean errorOnUnknownProfile;
    private boolean anyExtensionsAllowed;
    private List<String> acceptedEncodings;
    private List<String> ignoredCodeSystems;
    private List<String> ignoredValueSets;

    public List<DependencyList> getDependencyLists() {
        return Collections.unmodifiableList(Objects.requireNonNullElseGet(dependencyLists, LinkedList::new));
    }

    public List<String> getAcceptedEncodings() {
        return Collections.unmodifiableList(Objects.requireNonNullElseGet(acceptedEncodings, LinkedList::new));
    }

    public List<String> getIgnoredCodeSystems() {
        return Collections.unmodifiableList(Objects.requireNonNullElseGet(ignoredCodeSystems, LinkedList::new));
    }

    public List<String> getIgnoredValueSets() {
        return Collections.unmodifiableList(Objects.requireNonNullElseGet(ignoredValueSets, LinkedList::new));
    }
}

