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
import de.gematik.refv.commons.exceptions.UnsupportedPackageException;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Modified from <a href="https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/ValidatorFactory.java">https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/ValidatorFactory.java</a> and <a href="https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/configuration/FhirPackageProperties.java">https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/configuration/FhirPackageProperties.java</a>
 * Copyright 2022 Deutscher Apothekerverband (DAV), Apache License, Version 2.0
 */
@Data
public class ValidationModuleConfiguration {

    static Logger logger = LoggerFactory.getLogger(ValidationModuleConfiguration.class);
    private Map<String, PackageDefinitionByVersion> fhirPackages = new HashMap<>();
    private Map<String, SupportedProfileVersions> supportedProfiles = new HashMap<>();

    private List<String> ignoredCodeSystems = new LinkedList<>();

    private List<String> ignoredValueSets = new LinkedList<>();

    private boolean errorOnUnknownProfile;
    private boolean anyExtensionsAllowed;

    public Collection<String> getPatchesForPackageAndItsDependencies(PackageDefinition packageDefinition) {
        var result = new HashSet<String>();
        result.addAll(packageDefinition.getPatches().stream().map(p -> getPathToPatch(packageDefinition.getFilename(), p)).collect(Collectors.toList()));
        result.addAll(getDistinctPatchFilenamesFromPackageDependencies(packageDefinition.getDependencies()));
        return result;
    }

    @SneakyThrows
    private Collection<String> getDistinctPatchFilenamesFromPackageDependencies(List<PackageReference> dependencies) {
        HashSet<String> packageFilenameList = new HashSet<>();

        if (dependencies == null || dependencies.isEmpty()) {
            return  packageFilenameList;
        }

        for(PackageReference dependency : dependencies) {
            String requiredPackageName = dependency.getPackageName();
            String requiredPackageVersion = dependency.getPackageVersion();

            PackageDefinitionByVersion dependencyVersions = fhirPackages.get(requiredPackageName);
            if (dependencyVersions == null)
                throw new UnsupportedPackageException(dependency);

            PackageDefinition dependencyPackageDefinition = dependencyVersions.getPackageVersions().get(requiredPackageVersion);
            if (dependencyPackageDefinition == null)
                throw new UnsupportedPackageException(dependency);

            List<String> patches = dependencyPackageDefinition.getPatches().stream().map(p -> getPathToPatch(dependencyPackageDefinition.getFilename(), p)).collect(Collectors.toList());
            packageFilenameList.addAll(patches);
            List<PackageReference> furtherDependencies = fhirPackages.get(dependency.getPackageName()).getPackageVersions().get(dependency.getPackageVersion()).getDependencies();
            if (furtherDependencies != null && !furtherDependencies.isEmpty()) {
                packageFilenameList.addAll(getDistinctPatchFilenamesFromPackageDependencies(furtherDependencies));
            }
        }
        return packageFilenameList;


    }

    private String getPathToPatch(String packageFileName, String patchFileName) {
        String fileNameWithoutExtension = packageFileName.replaceFirst("[.][^.]+$", "");
        return String.format("%s/%s",fileNameWithoutExtension,patchFileName);
    }

    public Optional<PackageDefinition> getPackageDefinitionForProfile(Profile profile) {
        logger.debug("Looking for package definition of profile {}", profile);

        String lookupVersion = profile.getVersion() == null ? "0.0.0" : profile.getVersion();
        var packageReference = getPackageReferenceForProfile(profile.getBaseCanonical(), lookupVersion);
        if(packageReference.isEmpty()) {
            logger.warn("Package reference for {} not found", profile);
            return Optional.empty();
        }

        String packageName = packageReference.get().getPackageName();

        if(!fhirPackages.containsKey(packageName)) {
            logger.warn("Package {} is unknown", packageName);
            return Optional.empty();
        }

        var packageVersions = fhirPackages.get(packageName).getPackageVersions();
        String packageVersion = packageReference.get().getPackageVersion();

        if(!packageVersions.containsKey(packageVersion)) {
            logger.warn("Version {} of package {} is unknown", packageVersion, packageName);
            return Optional.empty();
        }

        PackageDefinition result = packageVersions.get(packageVersion);
        logger.debug("Package definition of profile {}: {}", profile, result);
        return Optional.of(result);
    }

    @SneakyThrows
    // The method has not yet been tested on recursive dependency definitions
    public Collection<String> getDistinctFilenamesFromPackageDependencies(Collection<PackageReference> dependencies) {
        HashSet<String> packageFilenameList = new HashSet<>();

        if (dependencies == null || dependencies.isEmpty()) {
            return  packageFilenameList;
        }

        for(PackageReference dependency : dependencies) {
            String requiredPackageName = dependency.getPackageName();
            String requiredPackageVersion = dependency.getPackageVersion();

            PackageDefinitionByVersion requiredSupportedPackage = fhirPackages.get(requiredPackageName);
            if (requiredSupportedPackage == null)
                throw new UnsupportedPackageException(dependency);

            PackageDefinition requiredPackageVersionFilenameDependencies = requiredSupportedPackage.getPackageVersions().get(requiredPackageVersion);
            if (requiredPackageVersionFilenameDependencies == null)
                throw new UnsupportedPackageException(dependency);

            packageFilenameList.add(requiredPackageVersionFilenameDependencies.getFilename());
            List<PackageReference> furtherDependencies = fhirPackages.get(dependency.getPackageName()).getPackageVersions().get(dependency.getPackageVersion()).getDependencies();
            if (furtherDependencies != null && !furtherDependencies.isEmpty()) {
                packageFilenameList.addAll(getDistinctFilenamesFromPackageDependencies(furtherDependencies));
            }
        }
        return packageFilenameList;
    }

    private Optional<PackageReference> getPackageReferenceForProfile(@NonNull String canonical,@NonNull String version) {
        if(supportedProfiles.containsKey(canonical)) {
            var profileVersions = supportedProfiles.get(canonical).getProfileVersions();
            if(profileVersions.containsKey(version)) {
                PackageReference requiredPackage = profileVersions.get(version).getRequiredPackage();
                if(requiredPackage != null)
                    return Optional.of(requiredPackage);

                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public List<String> listPackageNamesToLoad(PackageDefinition packageDefinition) {
        var packageFilenames = new LinkedList<String>();
        packageFilenames.add(packageDefinition.getFilename());
        packageFilenames.addAll(getDistinctFilenamesFromPackageDependencies(packageDefinition.getDependencies()));
        return packageFilenames;
    }
}
