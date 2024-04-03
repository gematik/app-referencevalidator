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
package de.gematik.refv.commons.configuration;

import de.gematik.refv.commons.Profile;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationModuleConfigurationTests {

    @Test
    void testFindFirstSupportedProfile() {
        var configuration = new ValidationModuleConfiguration();
        configuration.getSupportedProfiles().put("https://supported-profile1",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));
        configuration.getSupportedProfiles().put("https://supported-profile2",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));

        List<String> referencedProfiles = List.of("http://unknown-profile1", "http://unknown-profile2", "https://supported-profile1|1.0.0", "https://supported-profile2|1.0.0");
        Profile firstSupportedProfile = configuration.findFirstSupportedProfileWithExistingConfiguration(referencedProfiles);
        assertThat(firstSupportedProfile)
                .isNotNull()
                .isEqualTo(Profile.parse("https://supported-profile1|1.0.0"));
    }

    @Test
    void testFindFirstSupportedProfileReturnsNullIfProfileVersionIsNotSupported() {
        var configuration = new ValidationModuleConfiguration();
        configuration.getSupportedProfiles().put("https://supported-profile1",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));

        List<String> referencedProfiles = List.of("https://supported-profile1");
        Profile firstSupportedProfile = configuration.findFirstSupportedProfileWithExistingConfiguration(referencedProfiles);
        assertThat(firstSupportedProfile)
                .isNull();
    }

    @Test
    void testFindFirstSupportedProfileReturnsNotNullIfNoProfileVersionIsGivenAndTheProfileIsSupported() {
        var configuration = new ValidationModuleConfiguration();
        configuration.getSupportedProfiles().put("https://supported-profile1",new SupportedProfileVersions(new HashMap<>() {{
            put("0.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));

        List<String> referencedProfiles = List.of("https://supported-profile1");
        Profile firstSupportedProfile = configuration.findFirstSupportedProfileWithExistingConfiguration(referencedProfiles);
        assertThat(firstSupportedProfile)
                .isNotNull()
                .isEqualTo(Profile.parse("https://supported-profile1"));
    }

    @Test
    void testFindFirstSupportedProfileWhenMultipleSupportedProfilesFound() {
        var configuration = new ValidationModuleConfiguration();
        configuration.getSupportedProfiles().put("https://supported-profile1",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));
        configuration.getSupportedProfiles().put("https://supported-profile2",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));

        List<String> referencedProfiles = List.of("http://unknown-profile1", "http://unknown-profile2", "https://supported-profile1|1.0.0", "https://supported-profile2|1.0.0");
        Profile firstSupportedProfile = configuration.findFirstSupportedProfileWithExistingConfiguration(referencedProfiles);
        assertThat(firstSupportedProfile)
                .isNotNull()
                .isEqualTo(Profile.parse("https://supported-profile1|1.0.0"));
    }

    @Test
    void testFindFirstSupportedProfileWhenNoSupportedProfilesPresent() {
        var configuration = new ValidationModuleConfiguration();
        configuration.getSupportedProfiles().put("https://supported-profile",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));

        List<String> referencedProfiles = List.of("http://unknown-profile1", "http://unknown-profile2", "http://unknown-profile3");
        Profile firstSupportedProfile = configuration.findFirstSupportedProfileWithExistingConfiguration(referencedProfiles);
        assertThat(firstSupportedProfile).isNull();
    }
}
