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
package de.gematik.refv.plugins.validation;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.gematik.refv.commons.Profile;
import de.gematik.refv.commons.configuration.DependencyList;
import de.gematik.refv.commons.configuration.ProfileConfiguration;
import de.gematik.refv.commons.configuration.SupportedProfileVersions;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.validation.BaseValidationResourceProvider;
import de.gematik.refv.commons.validation.support.CustomNpmPackageValidationSupport;
import de.gematik.refv.plugins.configuration.MalformedPackageDeclarationException;
import de.gematik.refv.plugins.configuration.PluginDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PluginValidationResourceProvider extends BaseValidationResourceProvider {

    private static final String CONFIG_FILE = "config.yaml";
    private final Function<String, InputStream> resourceLoaderFunction;
    private final FhirContext fhirContext;
    @java.lang.SuppressWarnings({"squid:S3077"}) // Reason for usage of volatile https://stackoverflow.com/a/7855774
    private volatile ValidationModuleConfiguration cachedConfiguration;
    private final Object lockObject = new Object();

    public PluginValidationResourceProvider(Function<String, InputStream> resourceLoaderFunction, FhirContext fhirContext) {
        super(resourceLoaderFunction);
        this.resourceLoaderFunction = resourceLoaderFunction;
        this.fhirContext = fhirContext;
    }

    @Override
    @SneakyThrows
    public ValidationModuleConfiguration getConfiguration() {
        if(cachedConfiguration == null) {
            // Double-checked locking pattern, cf. https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
            synchronized (lockObject) {
                if (cachedConfiguration == null) {
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    PluginDefinition pluginDefinition;
                    try (InputStream pluginDefinitionFile = getResource(CONFIG_FILE)) {
                        pluginDefinition = mapper.readValue(pluginDefinitionFile, PluginDefinition.class);
                    }
                    cachedConfiguration = createValidationModuleConfiguration(pluginDefinition);
                }
            }
        }
        return cachedConfiguration;
    }

    private ValidationModuleConfiguration createValidationModuleConfiguration(PluginDefinition pluginDefinition) throws MalformedPackageDeclarationException, IOException {
        ValidationModuleConfiguration validationModuleConfiguration = new ValidationModuleConfiguration();
        validationModuleConfiguration.setId(pluginDefinition.getId());
        validationModuleConfiguration.setAcceptedEncodings(pluginDefinition.getValidation().getAcceptedEncodings());
        validationModuleConfiguration.setAnyExtensionsAllowed(pluginDefinition.getValidation().isAnyExtensionsAllowed());
        validationModuleConfiguration.setErrorOnUnknownProfile(pluginDefinition.getValidation().isErrorOnUnknownProfile());
        validationModuleConfiguration.setVersion(pluginDefinition.getVersion());

        validationModuleConfiguration.setIgnoredCodeSystems(
                pluginDefinition.getValidation().getIgnoredCodeSystems() != null ? pluginDefinition.getValidation().getIgnoredCodeSystems() : new ArrayList<>()
        );
        validationModuleConfiguration.setIgnoredValueSets(
                pluginDefinition.getValidation().getIgnoredValueSets() != null ? pluginDefinition.getValidation().getIgnoredValueSets() : new ArrayList<>()
        );
        validationModuleConfiguration.setAcceptedEncodings(new ArrayList<>());

        List<String> dependencies = new ArrayList<>();
        dependencies.add(pluginDefinition.getValidationFhirPackageAsFilename());
        dependencies.addAll(pluginDefinition.getValidationDependenciesAsFilenames());
        List<String> distinctDependencies = dependencies.stream()
                .distinct()
                .collect(Collectors.toList());

        Map<String, DependencyList> dependencyLists = new HashMap<>();
        DependencyList dependencyList = new DependencyList("", "", distinctDependencies, pluginDefinition.getValidation().getValidationMessageTransformations());
        dependencyLists.put(pluginDefinition.getValidation().getFhirPackage(), dependencyList);
        validationModuleConfiguration.setDependencyLists(dependencyLists);

        validationModuleConfiguration.setSupportedProfiles(getSupportedProfiles(pluginDefinition.getValidation().getFhirPackage()));

        return validationModuleConfiguration;
    }

    private Map<String, SupportedProfileVersions> getSupportedProfiles(@NonNull String fhirPackageName) throws IOException {
        CustomNpmPackageValidationSupport customNpmPackageValidationSupport = CustomNpmPackageValidationSupport.create(fhirContext, fhirPackageName.replace("#", "-") + ".tgz", resourceLoaderFunction);
        var allProfiles = customNpmPackageValidationSupport.getAllProfiles();
        Map<String, SupportedProfileVersions> supportedProfiles = new HashMap<>();
        if(allProfiles != null) {
            for (Profile definition : allProfiles) {
                ProfileConfiguration profileConfiguration = new ProfileConfiguration(List.of(fhirPackageName), null);
                Map<String, ProfileConfiguration> profileVersions = new HashMap<>();
                profileVersions.put(definition.getVersion(), profileConfiguration);

                // Caution! If multiple profiles with the same base canonical are present, the last one will be used for validation of resources with meta.profile without patch number
                addVersionWithoutPatchnumber(definition.getVersion(), profileVersions, profileConfiguration);

                profileVersions.put("0.0.0", profileConfiguration);
                SupportedProfileVersions supportedProfileVersions = new SupportedProfileVersions(profileVersions);
                supportedProfiles.put(definition.getBaseCanonical(), supportedProfileVersions);
            }
        }
        return supportedProfiles;
    }

    /**
     * Adds profile with MAJOR.MINOR version to the map so that the profile can be also referenced without the patch number, e.g. KBV_PR_ERP_Bundle|1.2
     */
    private static void addVersionWithoutPatchnumber(String profileVersion, Map<String, ProfileConfiguration> profileVersions, ProfileConfiguration profileConfiguration) {
        if (profileVersion.matches("\\d+\\.\\d+\\.\\d+")) {
            String[] versionParts = profileVersion.split("\\.");
            String majorMinorVersion = versionParts[0] + "." + versionParts[1];
            profileVersions.put(majorMinorVersion, profileConfiguration);
        }
    }
}
