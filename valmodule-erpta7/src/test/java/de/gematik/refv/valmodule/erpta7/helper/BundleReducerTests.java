/*-
 * #%L
 * valmodule-erpta7
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
package de.gematik.refv.valmodule.erpta7.helper;

import static de.gematik.refv.valmodule.erpta7.helper.ResourceLoader.getTestResourceBody;
import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.XmlParser;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BundleReducerTests {

    private BundleReducer bundleReducer;

    private static final Pattern LINE_SEPARATOR_PATTERN = Pattern.compile("\\R");

    private final XmlParser xmlParser = (XmlParser) FhirContext.forR4().newXmlParser();

    // Workaround for different line separators when running tests locally vs running tests in jenkins
    private String normalizeLineSeparators(String input) {
        return LINE_SEPARATOR_PATTERN.matcher(input).replaceAll("\n");
    }

    @BeforeEach
    @SneakyThrows
    void setUp() {
        String testResourceBody = getTestResourceBody("bundle-reducer/example-bundle.xml");
        bundleReducer = new BundleReducer(testResourceBody);
    }



    @Test
    @SneakyThrows
    void testGetAllRezeptBundlesAsString() {
        List<String> allRezeptBundlesAsString = bundleReducer.getAllRezeptBundlesAsString();
        List<String> normalizedRezeptBundles = allRezeptBundlesAsString.stream()
                .map(this::normalizeLineSeparators)
                .collect(Collectors.toList());

        assertThat(normalizedRezeptBundles)
                .hasSize(3)
                .contains(
                        "<Bundle xmlns=\"http://hl7.org/fhir\">\n" +
                                "                Rezept-Bundle 2\n" +
                                "            </Bundle>");
    }

    @SneakyThrows
    @ParameterizedTest
    @CsvSource({
            "bundle-reducer/example-bundle.xml, bundle-reducer/reduced-bundle.xml",
            "bundle-reducer/bundle-with-first-entry-not-composition.xml, bundle-reducer/reduced-bundle-with-first-entry-not-composition.xml"
    })
    void testGetReducedResourceBody(String inputResource, String expectedResource) {
        String testResourceBody = getTestResourceBody(inputResource);
        var bundleReducer = new BundleReducer(testResourceBody);

        xmlParser.setPrettyPrint(true);
        String reducedResourceBody = xmlParser.encodeResourceToString(xmlParser.parseResource(bundleReducer.getReducedResourceBody()));
        String expected = xmlParser.encodeResourceToString(xmlParser.parseResource(getTestResourceBody(expectedResource)));
        assertThat(reducedResourceBody).as("Body has not been reduced correctly").isEqualTo(expected);
    }
}
