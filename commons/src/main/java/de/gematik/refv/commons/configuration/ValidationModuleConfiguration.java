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

package de.gematik.refv.commons.configuration;

import de.gematik.refv.commons.Profile;
import de.gematik.refv.commons.exceptions.UnsupportedProfileException;
import lombok.Data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public class ValidationModuleConfiguration {

    private Map<String, DependencyList> dependencyLists = new HashMap<>();
    private Map<String, SupportedProfileVersions> supportedProfiles = new HashMap<>();

    private List<String> ignoredCodeSystems = new LinkedList<>();

    private List<String> ignoredValueSets = new LinkedList<>();
    private List<String> acceptedEncodings = new LinkedList<>();

    private boolean errorOnUnknownProfile;
    private boolean anyExtensionsAllowed;
    private String version;


    public ProfileConfiguration getSupportedProfileConfigurationOrThrow(Profile profile) {
        var result = getSupportedProfileConfigurationOptional(profile);
        if(result.isEmpty())
            throw new UnsupportedProfileException(profile);

        return result.get();
    }

    public Optional<ProfileConfiguration> getSupportedProfileConfigurationOptional(Profile profile) {
        String lookupVersion = profile.getVersion() == null ? "0.0.0" : profile.getVersion();
        String baseCanonical = profile.getBaseCanonical();

        if(supportedProfiles.containsKey(baseCanonical)) {
            var profileVersions = supportedProfiles.get(baseCanonical).getProfileVersions();
            if(profileVersions.containsKey(lookupVersion))
                return Optional.of(profileVersions.get(lookupVersion));
        }

        if(supportedProfiles.isEmpty()) {
            var allDependencyLists = dependencyLists.keySet().stream().findFirst();
            if(allDependencyLists.isEmpty())
                throw new IllegalStateException("Supported profiles are not explicitly defined and no dependency lists found. Define at least one dependency list");

            return Optional.of(new ProfileConfiguration(List.of(allDependencyLists.get()), null));
        }
        return Optional.empty();
    }

    public DependencyListsWrapper getDependencyListsForProfile(Profile profile) {
        var lists = getSupportedProfileConfigurationOrThrow(profile).getDependencyLists();
        boolean allKeysExist = lists.stream().allMatch(key -> dependencyLists.containsKey(key));
        if(!allKeysExist)
            throw new IllegalStateException(String.format("Some of the dependency lists of %s are undefined", profile));

        return new DependencyListsWrapper(lists.stream().map(key -> dependencyLists.get(key)).collect(Collectors.toList()));
    }
}
