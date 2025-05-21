/*-
 * #%L
 * commons
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.commons.configuration;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * Modified from <a href="https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/configuration/FhirPackageInfo.java">https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/configuration/FhirPackageInfo.java</a>
 * Copyright 2022 Deutscher Apothekerverband (DAV), Apache License, Version 2.0
 *
 */
@Data
@AllArgsConstructor
public class SupportedProfileVersions {
    // Key: Version of Profile
    private Map<String, ProfileConfiguration> profileVersions;

}
