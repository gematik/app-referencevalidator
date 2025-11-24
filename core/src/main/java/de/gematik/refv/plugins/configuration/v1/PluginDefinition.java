/*-
 * #%L
 * referencevalidator-lib
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.plugins.configuration.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.gematik.refv.plugins.configuration.FhirPackage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Getter
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginDefinition {
    @NonNull
    private String configSpecVersion;
    @NonNull
    private String id;
    @NonNull
    private String version;
    @NonNull
    private String author;
    @NonNull
    private String description;
    private String specUrl;
    @NonNull
    private ValidationSection validation;
    private List<FhirPackage> snapshotDependencies;

    // Used by PluginBuilder only. Can be removed in the future.
    public List<FhirPackage> getSnapshotDependencies() {
        return Collections.unmodifiableList(Objects.requireNonNullElseGet(snapshotDependencies, LinkedList::new));
    }
}

