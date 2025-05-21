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

import ca.uhn.fhir.context.support.IValidationSupport;
import de.gematik.refv.commons.Profile;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ValidationModuleConfiguration {

    private String id;
    private Map<String, DependencyList> dependencyLists = new HashMap<>();
    private Map<String, SupportedProfileVersions> supportedProfiles = new HashMap<>();

    private List<String> ignoredCodeSystems = new LinkedList<>();

    private List<String> ignoredValueSets = new LinkedList<>();
    private List<String> acceptedEncodings = new LinkedList<>();

    private boolean errorOnUnknownProfile;
    private boolean anyExtensionsAllowed;
    private String version;
    private IValidationSupport.IssueSeverity ucumValidationSeverityLevel = IValidationSupport.IssueSeverity.ERROR;

    public Optional<ProfileConfiguration> getSupportedProfileConfiguration(Profile profile) {
        String lookupVersion = profile.getVersion() == null ? "0.0.0" : profile.getVersion();
        String baseCanonical = profile.getBaseCanonical();

        if(supportedProfiles.containsKey(baseCanonical)) {
            var profileVersions = supportedProfiles.get(baseCanonical).getProfileVersions();
            if(profileVersions.containsKey(lookupVersion))
                return Optional.of(profileVersions.get(lookupVersion));
        }
        if(supportedProfiles.isEmpty()) {
            // There are no supportedProfiles defined for valmodule-core.
            // But if we don't return a dependencyList here,the validator will always return an Unsupported Profile Error
            var firstDependencyList = dependencyLists.keySet().stream().findFirst();
            if(firstDependencyList.isEmpty())
                throw new IllegalStateException("Supported profiles are not explicitly defined and no dependency lists found. Define at least one dependency list");

            return Optional.of(new ProfileConfiguration(List.of(firstDependencyList.get()), null));
        }
        return Optional.empty();
    }

    public DependencyListsWrapper getDependencyListsForProfile(ProfileConfiguration profileConfiguration) {
        var lists = profileConfiguration.getDependencyLists();
        boolean allKeysExist = lists.stream().allMatch(key -> dependencyLists.containsKey(key));
        if (!allKeysExist)
            throw new IllegalStateException(String.format("Some of the dependency lists are undefined: %s", String.join(",", profileConfiguration.getDependencyLists())));

        return new DependencyListsWrapper(lists.stream().map(key -> dependencyLists.get(key)).collect(Collectors.toList()));
    }

    public Profile findFirstSupportedProfileWithExistingConfiguration(List<String> profiles) {
        List<Profile> supportedProfilesFound = profiles.stream().map(Profile::parse).
                filter(
                profile -> getSupportedProfileConfiguration(profile).isPresent()
        ).collect(Collectors.toList());
        if(supportedProfilesFound.isEmpty())
            return null;
        if(supportedProfilesFound.size() > 1)
            log.warn("Multiple supported profiles found. Selecting the first one for further processing: {}", supportedProfilesFound.get(0));
        return supportedProfilesFound.get(0);
    }

}
