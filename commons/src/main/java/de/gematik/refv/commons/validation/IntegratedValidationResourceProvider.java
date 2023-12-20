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

package de.gematik.refv.commons.validation;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.util.ClasspathUtil;

/**
 * Load resources (NPM package or patch) using a classpath specification, e.g. <code>/path/to/resource/my_package.tgz</code>. The
 * classpath spec can optionally be prefixed with the string <code>classpath:</code>
 *
 * @throws InternalErrorException If the classpath file can't be found
 */
public class IntegratedValidationResourceProvider extends BaseValidationResourceProvider {

    public IntegratedValidationResourceProvider(String moduleId) {
        super(path -> ClasspathUtil.loadResourceAsStream(moduleId + "/" + path));
    }
}
