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
package de.gematik.refv.plugins.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.gematik.refv.commons.configuration.ValidationMessageTransformation;
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
public class DependencyList {
    @NonNull
    private FhirPackage fhirPackage;
    private List<FhirPackage> dependencies;
    private List<ValidationMessageTransformation> validationMessageTransformations;
    private String validFrom;
    private String validTill;
    private List<CreationDateLocator> creationDateLocators;

    public List<FhirPackage> getDependencies() {
        return Collections.unmodifiableList(Objects.requireNonNullElseGet(dependencies, LinkedList::new));
    }

    public List<ValidationMessageTransformation> getValidationMessageTransformations() {
        return Collections.unmodifiableList(Objects.requireNonNullElseGet(validationMessageTransformations, LinkedList::new));
    }

    public List<CreationDateLocator> getCreationDateLocators() {
        return Collections.unmodifiableList(Objects.requireNonNullElseGet(creationDateLocators, LinkedList::new));
    }
}
