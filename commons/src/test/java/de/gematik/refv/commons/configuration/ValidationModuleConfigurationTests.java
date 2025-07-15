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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.refv.commons.Profile;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationModuleConfigurationTests {

    @Test
    void testFindFirstSupportedProfile() {
        var supportedProfiles = new HashMap<String, SupportedProfileVersions>();
        supportedProfiles.put("https://supported-profile1",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));
        supportedProfiles.put("https://supported-profile2",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));
        var configuration = ValidationModuleConfiguration.builder().supportedProfiles(supportedProfiles).build();

        List<String> referencedProfiles = List.of("http://unknown-profile1", "http://unknown-profile2", "https://supported-profile1|1.0.0", "https://supported-profile2|1.0.0");
        Profile firstSupportedProfile = configuration.findFirstSupportedProfileWithExistingConfiguration(referencedProfiles);
        assertThat(firstSupportedProfile)
                .isNotNull()
                .isEqualTo(Profile.parse("https://supported-profile1|1.0.0"));
    }

    @Test
    void testFindFirstSupportedProfileReturnsNullIfProfileVersionIsNotSupported() {
        var supportedProfiles = new HashMap<String, SupportedProfileVersions>();
        supportedProfiles.put("https://supported-profile1",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));
        var configuration = ValidationModuleConfiguration.builder().supportedProfiles(supportedProfiles).build();

        List<String> referencedProfiles = List.of("https://supported-profile1");
        Profile firstSupportedProfile = configuration.findFirstSupportedProfileWithExistingConfiguration(referencedProfiles);
        assertThat(firstSupportedProfile)
                .isNull();
    }

    @Test
    void testFindFirstSupportedProfileReturnsNotNullIfNoProfileVersionIsGivenAndTheProfileIsSupported() {
        var supportedProfiles = new HashMap<String, SupportedProfileVersions>();
        supportedProfiles.put("https://supported-profile1",new SupportedProfileVersions(new HashMap<>() {{
            put("0.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));
        var configuration = ValidationModuleConfiguration.builder().supportedProfiles(supportedProfiles).build();

        List<String> referencedProfiles = List.of("https://supported-profile1");
        Profile firstSupportedProfile = configuration.findFirstSupportedProfileWithExistingConfiguration(referencedProfiles);
        assertThat(firstSupportedProfile)
                .isNotNull()
                .isEqualTo(Profile.parse("https://supported-profile1"));
    }

    @Test
    void testFindFirstSupportedProfileWhenMultipleSupportedProfilesFound() {
        var supportedProfiles = new HashMap<String, SupportedProfileVersions>();
        supportedProfiles.put("https://supported-profile1",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));
        supportedProfiles.put("https://supported-profile2",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));
        var configuration = ValidationModuleConfiguration.builder().supportedProfiles(supportedProfiles).build();

        List<String> referencedProfiles = List.of("http://unknown-profile1", "http://unknown-profile2", "https://supported-profile1|1.0.0", "https://supported-profile2|1.0.0");
        Profile firstSupportedProfile = configuration.findFirstSupportedProfileWithExistingConfiguration(referencedProfiles);
        assertThat(firstSupportedProfile)
                .isNotNull()
                .isEqualTo(Profile.parse("https://supported-profile1|1.0.0"));
    }

    @Test
    void testFindFirstSupportedProfileWhenNoSupportedProfilesPresent() {
        var supportedProfiles = new HashMap<String, SupportedProfileVersions>();
        supportedProfiles.put("https://supported-profile",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.0", new ProfileConfiguration(List.of("dependency-list"), null));
        }}));
        var configuration = ValidationModuleConfiguration.builder().supportedProfiles(supportedProfiles).build();

        List<String> referencedProfiles = List.of("http://unknown-profile1", "http://unknown-profile2", "http://unknown-profile3");
        Profile firstSupportedProfile = configuration.findFirstSupportedProfileWithExistingConfiguration(referencedProfiles);
        assertThat(firstSupportedProfile).isNull();
    }
}
