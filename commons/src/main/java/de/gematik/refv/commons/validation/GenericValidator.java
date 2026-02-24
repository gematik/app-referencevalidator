/*-
 * #%L
 * commons
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.commons.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.gematik.refv.commons.Profile;
import de.gematik.refv.commons.configuration.DependencyList;
import de.gematik.refv.commons.configuration.DependencyListsWrapper;
import de.gematik.refv.commons.configuration.ProfileConfiguration;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.exceptions.UnsupportedProfileException;
import de.gematik.refv.commons.validation.support.XmlCommentRemover;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class GenericValidator {
    public static final String ERR_REFV_PROFILE_OUTSIDE_OF_VALIDITY_PERIOD = "REFV_PROFILE_OUTSIDE_OF_VALIDITY_PERIOD";
    public static final String ERR_REFV_NO_CREATION_DATE_IN_RESOURCE = "REFV_NO_CREATION_DATE_IN_RESOURCE";
    public static final String ERR_REFV_PARSE_ERROR = "REFV_PARSE_ERROR";
    public static final String WARN_REFV_VALIDITY_PERIOD_CHECK_DISABLED = "REFV_VALIDITY_PERIOD_CHECK_DISABLED";
    public static final String WARN_REFV_PASSED_PROFILE_DIFFERS_FROM_META_PROFILE = "REFV_WARN_PASSED_PROFILE_DIFFERS_FROM_META_PROFILE";
    public static final String ERR_REFV_WRONG_ENCODING = "REFV_WRONG_ENCODING";
    public static final String ERR_REFV_PROFILE_FILTER_MISMATCH = "REFV_PROFILE_FILTER_MISMATCH";
    private final FhirContext fhirContext;

    private final ReferencedProfileLocator referencedProfileLocator = new ReferencedProfileLocator();

    private final HapiFhirValidatorFactory hapiFhirValidatorFactory;

    private final SeverityLevelTransformer severityLevelTransformator = new SeverityLevelTransformer();
    private final ConcurrentHashMap<DependencyList, FhirValidator> hapiFhirValidatorCache = new ConcurrentHashMap<>();

    private final ValidationResultOutputFilter outputFilter = new ValidationResultOutputFilter();

    public GenericValidator(FhirContext context) {
        this.fhirContext = context;
        this.hapiFhirValidatorFactory = new HapiFhirValidatorFactory(fhirContext);

        // Do not evict internal cache of FhirInstanceValidator as the object itself is kept in memory and its cache is permanently required
        // Mediates the memory leak issue https://github.com/hapifhir/org.hl7.fhir.core/issues/1412 (occurs in HAPI 6.6.2)
        System.setProperty("TEST_SYSTEM_PROP_VALIDATION_RESOURCE_CACHES_MS", String.valueOf(Long.MAX_VALUE));
    }

    public ValidationResult validate(
            @NonNull
            String resourceBody,
            @NonNull
            ValidationModuleResourceProvider resourceProvider) throws IllegalArgumentException {
        return validate(resourceBody, resourceProvider, ValidationOptions.getDefaults());
    }

    public ValidationResult validate(
            @NonNull
            String resourceBody,
            @NonNull
            ValidationModuleResourceProvider resourceProvider,
            @NonNull ValidationOptions validationOptions) throws IllegalArgumentException {

        resourceBody = XmlCommentRemover.removeXmlCommentsFrom(resourceBody);
        ValidationResult result;
        var configuration = resourceProvider.getConfiguration();

        if(!validateEncoding(resourceBody, configuration, validationOptions))
            result = ValidationResult.createInstance(ResultSeverityEnum.ERROR, ERR_REFV_WRONG_ENCODING, String.format("Wrong instance encoding. Allowed encodings: %s", String.join(",", getAcceptedEncodings(configuration, validationOptions))));
        else {

            List<String> allReferencedProfilesInResource = referencedProfileLocator.getAllReferencedProfilesInResource(resourceBody);
            if(validationOptions.getProfileFilterRegex() != null && noneReferencedProfileMatches(allReferencedProfilesInResource, validationOptions.getProfileFilterRegex())) {
                result = ValidationResult.createInstance(ResultSeverityEnum.ERROR, ERR_REFV_PROFILE_FILTER_MISMATCH, String.format("None of the referenced profiles in the resource matches the profile filter: %s. Referenced profiles: %s", validationOptions.getProfileFilterRegex(), allReferencedProfilesInResource));
            }
            else {
                Profile profileForValidation;
                if (!validationOptions.getProfiles().isEmpty()) {
                    var userDefinedProfile = validationOptions.getProfiles().get(0); // Only one user defined profile is supported at the moment
                    log.warn("Profile for validation has been passed by user: " + userDefinedProfile);
                    profileForValidation = configuration.findFirstSupportedProfileWithExistingConfiguration(List.of(userDefinedProfile));
                } else if (!allReferencedProfilesInResource.isEmpty())
                    profileForValidation = configuration.findFirstSupportedProfileWithExistingConfiguration(allReferencedProfilesInResource);
                else
                    throw new IllegalArgumentException("FHIR resources without a referenced profile are currently unsupported. Please provide a profile parameter or a profile in the resource meta.profile element.");

                if (profileForValidation == null)
                    throw new UnsupportedProfileException(allReferencedProfilesInResource);

                result = validateResource(
                        new ResourceToValidate(resourceBody, allReferencedProfilesInResource),
                        resourceProvider,
                        validationOptions.getProfileValidityPeriodCheckStrategy(),
                        profileForValidation);
            }
        }

        return outputFilter.apply(result, validationOptions.getValidationMessagesFilter());
    }

    private static boolean noneReferencedProfileMatches(List<String> allReferencedProfilesInResource, Pattern profileFilterRegex) {
        log.debug("Checking if any of the referenced profiles in the resource matches the profile filter: {}...", profileFilterRegex);
        return allReferencedProfilesInResource.stream().noneMatch(p -> profileFilterRegex.matcher(p).find());
    }

    private ValidationResult validateResource(ResourceToValidate resource, ValidationModuleResourceProvider resourceProvider, ProfileValidityPeriodCheckStrategy profileValidityPeriodCheckStrategy, Profile profileForValidation) {
        var configuration = resourceProvider.getConfiguration();

        log.info("Validating against {}...", profileForValidation);

        var profileConfigurationOptional = configuration.getSupportedProfileConfiguration(profileForValidation);
        if(profileConfigurationOptional.isEmpty())
            // Profile is listed among supported profiles, but there are no dependency lists defined --> corrupted module configuration
            throw new IllegalStateException(String.format("Could not retrieve profile configuration for %s", profileForValidation));

        ProfileConfiguration profileConfiguration = profileConfigurationOptional.get();

        String creationDateLocator = profileConfiguration.getCreationDateLocator();
        var dependencyListsWrapper = configuration.getDependencyListsForProfile(profileConfiguration);

        // Assumption: each profile has at least one dependency list configured
        if (StringUtils.isEmpty(creationDateLocator)) {
            log.debug("No creation date locator expression is configured for the profile {}. Validating against all configured dependency lists...", profileForValidation);
            return validateUsingMultipleDependencyLists(resource, resourceProvider, profileForValidation, dependencyListsWrapper.getDependencyLists());
        }
        else {
            return validateUsingConfiguredLocator(resource, resourceProvider, profileValidityPeriodCheckStrategy, creationDateLocator, dependencyListsWrapper, profileForValidation);
        }
    }

    private ValidationResult validateUsingConfiguredLocator(ResourceToValidate resource, ValidationModuleResourceProvider resourceProvider, ProfileValidityPeriodCheckStrategy profileValidityPeriodCheckStrategy, String creationDateLocator, DependencyListsWrapper dependencyListsWrapper, Profile profileForValidation) {
        try {
            var resourceCreationDateOptional = new ResourceCreationDateLocator(fhirContext).findCreationDateIn(resource.getBody(), creationDateLocator);

            log.debug("Resource creation date: {}", resourceCreationDateOptional);

            if (resourceCreationDateOptional.isEmpty()) {
                // Use Case 2.1 Creation date could not be located in the resource --> error
                return ValidationResult.createInstance(ResultSeverityEnum.ERROR, ERR_REFV_NO_CREATION_DATE_IN_RESOURCE, String.format("Could not determine the creation date of the resource using the configured expression %s", creationDateLocator));
            }

            // Use Case 2.2 Creation date could be located in the resource
            List<DependencyList> dependencyLists = dependencyListsWrapper.getDependencyListsValidAt(resourceCreationDateOptional.get());
            if (!dependencyLists.isEmpty()) {
                // Use Case 2.2.1 At least one dependency list for the resource creation date is present
                return validateUsingMultipleDependencyLists(resource, resourceProvider, profileForValidation, dependencyLists);
            }

            // Use Case 2.2.2 No Dependency list is found for the creation date
            if (profileValidityPeriodCheckStrategy == ProfileValidityPeriodCheckStrategy.VALIDATE) {
                // Use Case 2.2.2.1 ValidityPeriodCheck is turned on and no dependency lists were found -> Profile is outside of validity period!
                String diagnostics = new DiagnosticsMessageBuilder().createValidityPeriodDiagnosticsString(dependencyListsWrapper, profileForValidation, resourceCreationDateOptional.get());
                return ValidationResult.createInstance(ResultSeverityEnum.ERROR, ERR_REFV_PROFILE_OUTSIDE_OF_VALIDITY_PERIOD, diagnostics);
            }

            // Use Case 2.2.2.2 ValidityPeriodChek is turned off -> use latest dependencies
            var result = validateUsingDependencyList(resource, resourceProvider, dependencyListsWrapper.getLatestDependencyList(), profileForValidation);
            addWarningAboutDeactivatedValidityPeriodCheckTo(result);
            return result;

        } catch (DataFormatException e) {
            log.debug("Parse error", e);
            return ValidationResult.createInstance(ResultSeverityEnum.ERROR, ERR_REFV_PARSE_ERROR, e.getMessage());
        }
    }

    private ValidationResult validateUsingMultipleDependencyLists(ResourceToValidate resource, ValidationModuleResourceProvider resourceProvider, Profile profileForValidation, List<DependencyList> dependencyLists) {
        ValidationResult result = null;
        for(var dependencyList : dependencyLists) {
            result = validateUsingDependencyList(resource, resourceProvider, dependencyList, profileForValidation);
            if(result.isValid())
                break;
        }
        return result;
    }

    private boolean validateEncoding(String resourceBody, ValidationModuleConfiguration configuration, de.gematik.refv.commons.validation.ValidationOptions validationOptions) {
        List<String> acceptedEncodings = getAcceptedEncodings(configuration, validationOptions);

        if(acceptedEncodings.isEmpty())
            return true;

        EncodingEnum encoding = EncodingEnum.detectEncodingNoDefault(resourceBody);

        if(acceptedEncodings.contains(Constants.FORMAT_XML) && encoding == EncodingEnum.XML)
            return true;

        if(acceptedEncodings.contains(Constants.FORMAT_JSON) && encoding == EncodingEnum.JSON)
            return true;

        log.warn("Unknown resource encoding: {}", encoding);
        return false;
    }

    private static List<String> getAcceptedEncodings(ValidationModuleConfiguration configuration, ValidationOptions validationOptions) {
        var encodings = !validationOptions.getAcceptedEncodings().isEmpty() ? validationOptions.getAcceptedEncodings() : configuration.getAcceptedEncodings();
        return encodings.stream().map(String::toLowerCase).collect(Collectors.toList());
    }

    private void addWarningAboutDeactivatedValidityPeriodCheckTo(ValidationResult result) {
        var m = new SingleValidationMessage();
        m.setSeverity(ResultSeverityEnum.WARNING);
        m.setMessageId(WARN_REFV_VALIDITY_PERIOD_CHECK_DISABLED);
        m.setMessage("Validity period check has been disabled by user");
        result.getValidationMessages().add(m);
    }

    private ValidationResult validateUsingDependencyList(ResourceToValidate resource, ValidationModuleResourceProvider resourceProvider, DependencyList dependencyList, Profile profileForValidation) {
        var ignoredValueSets = resourceProvider.getConfiguration().getIgnoredValueSets();

        log.debug("Applying dependency list: {}", dependencyList);
        var fhirValidator = hapiFhirValidatorCache.computeIfAbsent(dependencyList, k ->
                hapiFhirValidatorFactory.createInstance(
                        dependencyList.getPackages(),
                        resourceProvider
                ));

        var options = new ca.uhn.fhir.validation.ValidationOptions();
        boolean isValidationProfileReferencedInTheResource = resource.getProfiles().contains(profileForValidation.getCanonical());
        if(!isValidationProfileReferencedInTheResource)
            options.addProfile(profileForValidation.getCanonical());
        var intermediateResult = fhirValidator.validateWithResult(resource.getBody(), options);
        
        log.debug("Pre-Transformation ValidationResult: Valid: {}, Messages: {}", intermediateResult.isSuccessful(), intermediateResult.getMessages());

        var filteredMessages = severityLevelTransformator.applyTransformations(intermediateResult.getMessages(), dependencyList.getValidationMessageTransformations(), ignoredValueSets);

        if(!isValidationProfileReferencedInTheResource) {
            SingleValidationMessage m = new SingleValidationMessage();
            m.setSeverity(ResultSeverityEnum.WARNING);
            m.setMessage(String.format("Resource meta.profile differs from the passed profile for validation: meta.profile=%s; passed=%s", resource.getProfiles(), profileForValidation));
            m.setMessageId(WARN_REFV_PASSED_PROFILE_DIFFERS_FROM_META_PROFILE);
            filteredMessages.add(m);
        }

        return new ValidationResult(filteredMessages);
    }

    @AllArgsConstructor
    @Getter
    private static class ResourceToValidate {
        private String body;
        private Collection<String> profiles;
    }
}
