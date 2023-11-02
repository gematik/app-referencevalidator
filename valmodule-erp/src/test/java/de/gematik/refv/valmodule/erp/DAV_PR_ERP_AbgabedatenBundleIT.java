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

package de.gematik.refv.valmodule.erp;

import de.gematik.refv.valmodule.base.helper.BaseProfileIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import java.util.stream.Stream;


class DAV_PR_ERP_AbgabedatenBundleIT extends BaseProfileIntegrationTest {
    private static final String DIR = "DAV-PR-ERP-AbgabedatenBundle";

    @BeforeAll
    void beforeAll() {
        super.createValidationModule("erp");
    }

    @TestFactory
    Stream<DynamicTest> testValidation() {
        return super.testValidationBase(DIR);
    }
}
