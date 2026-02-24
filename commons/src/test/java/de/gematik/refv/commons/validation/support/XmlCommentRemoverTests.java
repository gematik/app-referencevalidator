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
package de.gematik.refv.commons.validation.support;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class XmlCommentRemoverTests {

    @ParameterizedTest
    @ValueSource( strings = {
            "<root><!-- Comment abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZäöüÄÖÜß1234567890§$%&/}{[]()?!#'``+-:;,.-<>@€|²³{[]}µ^^^°~´´ --><element>Value</element><!-- Comment 2 --></root>",
            "<root><!-- Comment with\nline break --><element>Value</element></root>",
            "<root><element>Value</element></root>"
    })
    @Execution(ExecutionMode.CONCURRENT)
    void testRemoveXmlCommentsFrom(String inputXml) {
        String expectedOutput = "<root><element>Value</element></root>";
        String cleanedXml = XmlCommentRemover.removeXmlCommentsFrom(inputXml);
        assertEquals(expectedOutput, cleanedXml);
    }

    @Test
    void testRemoveXmlCommentsFrom_EmptyString() {
        String inputXml = "";
        String cleanedXml = XmlCommentRemover.removeXmlCommentsFrom(inputXml);
        assertEquals(inputXml, cleanedXml);
    }
}
