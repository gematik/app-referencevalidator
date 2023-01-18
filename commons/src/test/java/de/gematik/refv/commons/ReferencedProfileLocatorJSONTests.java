package de.gematik.refv.commons;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

class ReferencedProfileLocatorJSONTests {
    ReferencedProfileLocator locator = new ReferencedProfileLocator();

    @ParameterizedTest
    @ValueSource(strings = {
            "https://bla.bla|1.0.2",
            "https://bla.bla"
    })
    void testProfileInCorrectResourceIsExtractedCorrectly(String canonical) {
        Profile profile = Profile.parse(canonical);
        String resource = String.format(
                "{\n"
                        + "\"resourceType\": \"Bundle\",\n"
                        + "\"id\": \"123456789\",\n"
                        + " \"meta\": {\n"
                        + "     \"profile\": [\n"
                        + "         \"%s\"\n"
                        + "    ],\n"
                        + "  }\n"
                        + "}", profile.getCanonical());
        Optional<Profile> profileOptional = locator.locate(resource);
        Assertions.assertTrue(profileOptional.isPresent());
        Assertions.assertEquals(profile, profileOptional.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Meta absent
            "{\n"
                    + "}",


            // Meta present, profile absent
            "{\n"
                    + " \"meta\": {\n"
                    + "     }\n"
                    + "}",


            // Profile present, value absent
            "{\n"
                    + " \"meta\": {\n"
                    + "     \"profile\": [\n"
                    + "    ]\n"
                    + "  }\n"
                    + "}",


            //Profile present, not a JsonArray, value empty
            "{\n"
                    + " \"meta\": {\n"
                    + "     \"profile\": \"\"\n"
                    + "  }\n"
                    + "}",


            // Profile present, value empty
            "{\n"
                    + " \"meta\": {\n"
                    + "     \"profile\": [\n"
                    + "         \"\"\n"
                    + "    ]\n"
                    + "  }\n"
                    + "}",
    })
    void testNoProfile(String resource) {
        Optional<Profile> profileOptional = locator.locate(resource);
        Assertions.assertTrue(profileOptional.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            //Meta field corrupted `}` missing
            "{\n"
                    + " \"meta\": {\n"
                    + "}",


            // Profile field corrupted `]` missing
            "{\n"
                    + " \"meta\": {\n"
                    + "     \"profile\": [\n"
                    + "  }\n"
                    + "}",
    })
    void corruptedJsonResource(String resource) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> locator.locate(resource));
    }

    @Test
    void testNotJsonResource() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> locator.locate("not a json resource"));
    }
}
