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
package de.gematik.refv.commons.validation;

import de.gematik.refv.commons.helper.ValidationModuleFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class BundlesIT {

    private static ValidationModule validationModule;

    @BeforeAll
    @SneakyThrows
    static void beforeAll() {
        validationModule = ValidationModuleFactory.createInstance("simple");
        validationModule.initialize();
    }

    @SneakyThrows
    @Test
    void completelyOrphanedEntriesAreInvalid() {
        assertInvalidAndThatMessageIdIsReturned("src/test/resources/bundles/invalid/unreferenced-entries.xml", "Bundle_BUNDLE_Entry_Orphan_DOCUMENT");
    }

    private static void assertInvalidAndThatMessageIdIsReturned(String path, String messageId) throws IOException {
        var result = validationModule.validateFile(path);
        Assertions.assertFalse(result.isValid(), result.toString());
        Assertions.assertTrue(result.getValidationMessages().stream().anyMatch(m -> m.getMessageId().equals(messageId)), "Expected error not found");
    }

    private static void assertValidAndThatMessageIdIsReturned(String path, String messageId) throws IOException {
        var options = ValidationOptions.getDefaults();
        options.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
        var result = validationModule.validateFile(path, options);
        Assertions.assertTrue(result.isValid(), result.toString());
        Assertions.assertTrue(result.getValidationMessages().stream().anyMatch(m -> m.getMessageId().equals(messageId)), "Expected warning not found");
    }

    @SneakyThrows
    @Test
    void referencesWhichAreNotURIsAreInvalid() {
        String path = "src/test/resources/bundles/invalid/references-not-uris.xml";
        assertInvalidAndThatMessageIdIsReturned(path, "Reference_REF_Format2");
    }

    @Test
    @SneakyThrows
    void validForPartiallyOrphanedEntries() {
        assertValidAndThatMessageIdIsReturned("src/test/resources/bundles/valid/unreferenced-entry-but-with-outgoing-references-to-other-entries.xml", "BUNDLE_BUNDLE_ENTRY_REVERSE_R4");
    }

    @Test
    @SneakyThrows
    void validForMismatchingRelativeAndAbsoluteUrlsEvenIfTargetProfileDoesntMatch() {
        assertValidAndThatMessageIdIsReturned("src/test/resources/bundles/valid/Target_Profiles_Are_Not_Validated_If_Local_References_Do_Not_Match_Full_URLs.xml", "BUNDLE_BUNDLE_POSSIBLE_MATCH_WRONG_FU");
    }

    @Test
    @SneakyThrows
    void validForMixedURNsAndURLs() {
        assertValidAndThatMessageIdIsReturned(
                "src/test/resources/bundles/valid/Mix_Of_URNs_And_URLs_In_Full_URLs.xml", "BUNDLE_BUNDLE_POSSIBLE_MATCH_WRONG_FU");
    }
}
