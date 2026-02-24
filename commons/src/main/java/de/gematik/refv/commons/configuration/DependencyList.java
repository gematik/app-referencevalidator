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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor
public class DependencyList {
  private String validFrom;
  private String validTill;
  private List<String> packages;
  private List<ValidationMessageTransformation> validationMessageTransformations;

  public List<String> getPackages() {
    return Collections.unmodifiableList(Objects.requireNonNullElseGet(packages, LinkedList::new));
  }

  public List<ValidationMessageTransformation> getValidationMessageTransformations() {
    return Collections.unmodifiableList(
        Objects.requireNonNullElseGet(validationMessageTransformations, LinkedList::new));
  }

  public boolean isValidFor(LocalDate resourceCreationDate) {
    var dateFrom = LocalDate.parse(getValidFrom());
    var dateTill = getValidTill() == null ? LocalDate.MAX : LocalDate.parse(getValidTill());
    return (getValidFrom() != null
            && (dateFrom.isBefore(resourceCreationDate) || dateFrom.isEqual(resourceCreationDate)))
        && (getValidTill() == null
            || dateTill.isAfter(resourceCreationDate)
            || dateTill.isEqual(resourceCreationDate));
  }

  public String toString()
    {
        return "DependencyList{"
            + "validFrom='"
            + validFrom
            + '\''
            + ", validTill='"
            + validTill
            + '\''
            + ", packages="
            + String.join(",",packages)
            + '}';
  }
}
