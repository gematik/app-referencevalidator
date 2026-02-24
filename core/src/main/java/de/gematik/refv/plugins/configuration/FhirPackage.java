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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
@Getter
@Setter(AccessLevel.PRIVATE)
public class FhirPackage {

    private static final String MALFORMED_PACKAGE_DECLARATION_TEXT = "Malformed package name '%s' (expected format: packageName#packageVersion)";
    private static final String PACKAGE_FILENAME_FORMATTER = "%s-%s.tgz";
    private static final String PACKAGE_NAMEVERSION_FORMATTER = "%s#%s";

    private final String name;
    private final String version;

    public FhirPackage(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public FhirPackage(String fhirPackageNameAndVersionWithRouteSeparator) {
        String[] validationFhirPackageNameAndVersion = fhirPackageNameAndVersionWithRouteSeparator.split("#");
        if(validationFhirPackageNameAndVersion.length < 2) {
            throw new MalformedPackageDeclarationException(String.format(MALFORMED_PACKAGE_DECLARATION_TEXT, fhirPackageNameAndVersionWithRouteSeparator));
        }
        name = validationFhirPackageNameAndVersion[0];
        version = validationFhirPackageNameAndVersion[1];
    }

    public String toNameAndVersion() {
        return String.format(PACKAGE_NAMEVERSION_FORMATTER, name, version);
    }

    public String toFilename() {
        return String.format(PACKAGE_FILENAME_FORMATTER, name, version);
    }

    public String toString() {
        return toNameAndVersion();
    }

}
