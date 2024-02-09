/*
 * Copyright (c) 2024 gematik GmbH
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

package de.gematik.refv.commons;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Modified from <a href="https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/util/Profile.java">https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/util/Profile.java</a>
 * Copyright 2022 Deutscher Apothekerverband (DAV), Apache License, Version 2.0
 */
@Data
@AllArgsConstructor
public class Profile {
    private String canonical;
    private String baseCanonical;
    private String version;

    public static Profile parse(String canonical) {
        String[] splittedString = canonical.split("\\|");
        if (splittedString.length < 2)
        {
            return new Profile(canonical, splittedString[0], null);
        }
        else
        {
            return new Profile(canonical, splittedString[0], splittedString[1]);
        }
    }

    @Override
    public String toString() {
        return canonical;
    }
}
