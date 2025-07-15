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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter(AccessLevel.PRIVATE)
@AllArgsConstructor
@Slf4j
@Builder
public class ValidationModuleConfiguration {

    private String id;
    private Map<String, DependencyList> dependencyLists;
    private Map<String, SupportedProfileVersions> supportedProfiles;
    private List<String> ignoredCodeSystems;
    private List<String> ignoredValueSets;
    private List<String> acceptedEncodings;
    private boolean errorOnUnknownProfile;
    private boolean anyExtensionsAllowed;
    private String version;
    @Builder.Default
    private IValidationSupport.IssueSeverity ucumValidationSeverityLevel = IValidationSupport.IssueSeverity.ERROR;

    public Map<String, DependencyList> getDependencyLists() {
        return Collections.unmodifiableMap(Objects.requireNonNullElse(dependencyLists, new HashMap<>()));
    }

    public Map<String, SupportedProfileVersions> getSupportedProfiles() {
        return Collections.unmodifiableMap(Objects.requireNonNullElse(supportedProfiles, new HashMap<>()));
    }

    public List<String> getIgnoredCodeSystems() {
        return Collections.unmodifiableList(Objects.requireNonNullElse(ignoredCodeSystems, new LinkedList<>()));
    }

    public List<String> getIgnoredValueSets() {
        return Collections.unmodifiableList(Objects.requireNonNullElse(ignoredValueSets, new LinkedList<>()));
    }

    public List<String> getAcceptedEncodings() {
        return Collections.unmodifiableList(Objects.requireNonNullElse(acceptedEncodings, new LinkedList<>()));
    }

    public Optional<ProfileConfiguration> getSupportedProfileConfiguration(Profile profile) {
        String lookupVersion = profile.getVersion() == null ? "0.0.0" : profile.getVersion();
        String baseCanonical = profile.getBaseCanonical();

        if(getSupportedProfiles().containsKey(baseCanonical)) {
            var profileVersions = getSupportedProfiles().get(baseCanonical).getProfileVersions();
            if(profileVersions.containsKey(lookupVersion))
                return Optional.of(profileVersions.get(lookupVersion));
        }
        if(getSupportedProfiles().isEmpty()) {
            // There are no supportedProfiles defined for valmodule-core.
            // But if we don't return a dependencyList here,the validator will always return an Unsupported Profile Error
            var firstDependencyList = getDependencyLists().keySet().stream().findFirst();
            if(firstDependencyList.isEmpty())
                throw new IllegalStateException("Supported profiles are not explicitly defined and no dependency lists found. Define at least one dependency list");

            return Optional.of(new ProfileConfiguration(List.of(firstDependencyList.get()), null));
        }
        return Optional.empty();
    }

    public DependencyListsWrapper getDependencyListsForProfile(ProfileConfiguration profileConfiguration) {
        var lists = profileConfiguration.getDependencyLists();
        boolean allKeysExist = lists.stream().allMatch(key -> getDependencyLists().containsKey(key));
        if (!allKeysExist)
            throw new IllegalStateException(String.format("Some of the dependency lists are undefined: %s", String.join(",", profileConfiguration.getDependencyLists())));

        return new DependencyListsWrapper(lists.stream().map(key -> getDependencyLists().get(key)).collect(Collectors.toList()));
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
