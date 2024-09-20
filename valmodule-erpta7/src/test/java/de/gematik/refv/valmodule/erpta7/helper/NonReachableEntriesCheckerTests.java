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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import static de.gematik.refv.valmodule.erpta7.helper.ResourceLoader.getTestResourceBody;

class NonReachableEntriesCheckerTests {

    @Test
    @SneakyThrows
    void testIfNonReachableEntriesAreFoundForDocuments() {
        var testBundle = getTestResourceBody("non-reachable-entries-checker/document-bundle-with-non-reachable-entries.xml");
        var documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        var xmlDoc = documentBuilder.parse(new InputSource(new StringReader(testBundle)));
        var messages = new NonReachableEntriesChecker().checkForNonReachableEntries(xmlDoc);
        Assertions.assertThat(messages).hasSize(1);
        Assertions.assertThat(messages.get(0).getMessage()).contains("Entry 'E' isn't reachable by traversing links (forward or backward) from the Composition");
    }

    @Test
    @SneakyThrows
    void testIfNonReachableEntriesAreIgnoredForCollections() {
        var testBundle = getTestResourceBody("non-reachable-entries-checker/collection-bundle-with-non-reachable-entries.xml");
        var documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        var xmlDoc = documentBuilder.parse(new InputSource(new StringReader(testBundle)));
        var messages = new NonReachableEntriesChecker().checkForNonReachableEntries(xmlDoc);
        Assertions.assertThat(messages).isEmpty();
    }
}
