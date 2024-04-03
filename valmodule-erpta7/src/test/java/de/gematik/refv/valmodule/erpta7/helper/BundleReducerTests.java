/*
Copyright (c) 2022-2024 gematik GmbH

Licensed under the Apache License, Version 2.0 (the License);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package de.gematik.refv.valmodule.erpta7.helper;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BundleReducerTests {

    private BundleReducer bundleReducer;

    private static final Pattern LINE_SEPARATOR_PATTERN = Pattern.compile("\\R");

    // Workaround for different line separators when running tests locally vs running tests in jenkins
    private String normalizeLineSeparators(String input) {
        return LINE_SEPARATOR_PATTERN.matcher(input).replaceAll("\n");
    }

    @BeforeEach
    void setUp() {
        String testResourceBody =
                "<Bundle>\n" +
                        "    <timestamp value=\"2024-01-01T00:00:00Z\"/>\n" +
                        "    <entry>\n" +
                        "        <resource>\n" +
                        "            <Composition>\n" +
                        "            </Composition>\n" +
                        "        </resource>\n" +
                        "    </entry>\n" +
                        "    <entry>\n" +
                        "        <resource>\n" +
                        "            <List>\n" +
                        "            </List>\n" +
                        "        </resource>\n" +
                        "    </entry>\n" +
                        "    <entry>\n" +
                        "        <resource>\n" +
                        "            <Bundle>\n" +
                        "                Rezept-Bundle 1\n" +
                        "            </Bundle>\n" +
                        "        </resource>\n" +
                        "    </entry>\n" +
                        "    <entry>\n" +
                        "        <resource>\n" +
                        "            <Bundle>\n" +
                        "                Rezept-Bundle 2\n" +
                        "            </Bundle>\n" +
                        "        </resource>\n" +
                        "    </entry>\n" +
                        "    <entry>\n" +
                        "        <resource>\n" +
                        "            <Bundle>\n" +
                        "                Rezept-Bundle 3\n" +
                        "            </Bundle>\n" +
                        "        </resource>\n" +
                        "    </entry>\n" +
                        "</Bundle>";
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

    @Test
    @SneakyThrows
    void testGetReducedResourceBody() {
        String reducedResourceBody = bundleReducer.getReducedResourceBody();
        reducedResourceBody = normalizeLineSeparators(reducedResourceBody);
        assertThat(reducedResourceBody).isEqualTo(
                "<Bundle xmlns=\"http://hl7.org/fhir\"><timestamp value=\"2024-01-01T00:00:00Z\"/><entry>\n" +
                        "        <resource>\n" +
                        "            <Composition>\n" +
                        "            </Composition>\n" +
                        "        </resource>\n" +
                        "    </entry><entry>\n" +
                        "        <resource>\n" +
                        "            <List>\n" +
                        "            </List>\n" +
                        "        </resource>\n" +
                        "    </entry><entry>\n" +
                        "        <resource>\n" +
                        "            <Bundle>\n" +
                        "                Rezept-Bundle 1\n" +
                        "            </Bundle>\n" +
                        "        </resource>\n" +
                        "    </entry></Bundle>");
    }
}
