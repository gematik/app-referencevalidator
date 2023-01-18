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

package de.gematik.refv.commons.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import de.gematik.refv.commons.Profile;
import de.gematik.refv.commons.ReferencedProfileLocator;
import de.gematik.refv.commons.configuration.PackageDefinition;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.exceptions.UnsupportedProfileException;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class GenericValidator {

    static Logger logger = LoggerFactory.getLogger(GenericValidator.class);
    private final FhirContext fhirContext;

    private final Map<Profile, FhirValidator> hapiValidatorsCache = new HashMap<>();

    private final ReferencedProfileLocator referencedProfileLocator;

    private final GenericValidatorFactory genericValidatorFactory;

    private final SeverityLevelTransformer severityLevelTransformator;

    private final ProfileCacheStrategy cacheStrategy;

    public ValidationResult validate(
            @NonNull
            String resourceBody,
            @NonNull
            ValidationModuleConfiguration configuration) throws IllegalArgumentException {

        Profile profileInResource = getProfileInResource(resourceBody);

        logger.info("Validating against {}...", profileInResource);

        var packageDefinition = getPackageDefinitionForProfile(profileInResource, configuration);

        logger.debug("PackageDefinition: {}", packageDefinition);

        FhirValidator fhirValidator = getOrCreateCachedFhirValidatorFor(profileInResource, configuration, packageDefinition);

        var intermediateResult = fhirValidator.validateWithResult(resourceBody);

        logger.debug("Pre-Transformation ValidationResult: Valid: {}, Messages: {}", intermediateResult.isSuccessful(), intermediateResult.getMessages());

        var filteredMessages = severityLevelTransformator.applyTransformations(intermediateResult.getMessages(), packageDefinition.getValidationMessageTransformations());
        var result = new ValidationResult(filteredMessages);

        logger.info("Final ValidationResult: {}", result);

        return result;
    }

    private FhirValidator getOrCreateCachedFhirValidatorFor(Profile profileInResource, ValidationModuleConfiguration configuration, PackageDefinition packageDefinition) {
        // The Cache helps to reuse initialized HapiValidator instances if the method is invoked on FHIR instances with the same Profile/Version.
        // For single usages (such as in CLI) there is no performance gain
        if(cacheStrategy == ProfileCacheStrategy.CACHE_PROFILES) {
            hapiValidatorsCache.computeIfAbsent(profileInResource, p -> genericValidatorFactory.createInstance(
                    fhirContext,
                    configuration.listPackageNamesToLoad(packageDefinition),
                    configuration.getPatchesForPackageAndItsDependencies(packageDefinition),
                    configuration.getIgnoredCodeSystems()
            ));
            return hapiValidatorsCache.get(profileInResource);
        }

        return genericValidatorFactory.createInstance(
                fhirContext,
                configuration.listPackageNamesToLoad(packageDefinition),
                configuration.getPatchesForPackageAndItsDependencies(packageDefinition),
                configuration.getIgnoredCodeSystems()
        );
    }

    private PackageDefinition getPackageDefinitionForProfile(Profile profileInResource, ValidationModuleConfiguration configuration) {
        var packageDefinition = configuration.getPackageDefinitionForProfile(profileInResource);
        if(packageDefinition.isEmpty())
            throw new UnsupportedProfileException(profileInResource);
        return packageDefinition.get();
    }

    private Profile getProfileInResource(String resourceBody) {
        // Use custom performance-optimized profile extraction due to issues with HAPI XML Parser
        // Parsed XML files are transformed to JSON internally by HAPI and some elements such as XML comments are processed wrongly
        Optional<Profile> profileOrEmpty = referencedProfileLocator.locate(resourceBody);
        if (profileOrEmpty.isEmpty())
            throw new IllegalArgumentException("FHIR resources without a referenced profile are currently unsupported");
        return profileOrEmpty.get();
    }

}
