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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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

                result = validateResource(resourceBody, resourceProvider, validationOptions, profileForValidation, configuration, allReferencedProfilesInResource);
            }
        }

        return outputFilter.apply(result, validationOptions.getValidationMessagesFilter());
    }

    private static boolean noneReferencedProfileMatches(List<String> allReferencedProfilesInResource, Pattern profileFilterRegex) {
        log.debug("Checking if any of the referenced profiles in the resource matches the profile filter: {}...", profileFilterRegex);
        return allReferencedProfilesInResource.stream().noneMatch(p -> profileFilterRegex.matcher(p).find());
    }

    private ValidationResult validateResource(String resourceBody, ValidationModuleResourceProvider resourceProvider, ValidationOptions validationOptions, Profile profileForValidation, ValidationModuleConfiguration configuration, List<String> allReferencedProfilesInResource) {
        log.info("Validating against {}...", profileForValidation);

        var profileConfigurationOptional = configuration.getSupportedProfileConfiguration(profileForValidation);
        if(profileConfigurationOptional.isEmpty())
            // Profile is listed among supported profiles, but there are no dependency lists defined --> corrupted module configuration
            throw new IllegalStateException(String.format("Could not retrieve profile configuration for %s", profileForValidation));

        ProfileConfiguration profileConfiguration = profileConfigurationOptional.get();

        String creationDateLocator = profileConfiguration.getCreationDateLocator();
        var dependencyLists = configuration.getDependencyListsForProfile(profileConfiguration);

        // Assumption: each profile has at least one dependency list configured
        if (StringUtils.isEmpty(creationDateLocator)) {
            return validateWithoutConfiguredLocator(resourceBody, resourceProvider, profileForValidation, dependencyLists, allReferencedProfilesInResource);
        }
        else {
            return validateUsingConfiguredLocator(resourceBody, resourceProvider, validationOptions, creationDateLocator, dependencyLists, profileForValidation, allReferencedProfilesInResource);
        }
    }

    private ValidationResult validateUsingConfiguredLocator(String resourceBody, ValidationModuleResourceProvider resourceProvider, ValidationOptions validationOptions, String creationDateLocator, DependencyListsWrapper dependencyLists, Profile profileForValidation, List<String> allReferencedProfilesInResource) {
        try {
            var resourceCreationDateOptional = new ResourceCreationDateLocator(fhirContext).findCreationDateIn(resourceBody, creationDateLocator);

            log.debug("Resource creation date: {}", resourceCreationDateOptional);

            if (resourceCreationDateOptional.isEmpty()) {
                // Use Case 2.1 Creation date could not be located in the resource --> error
                return ValidationResult.createInstance(ResultSeverityEnum.ERROR, ERR_REFV_NO_CREATION_DATE_IN_RESOURCE, String.format("Could not determine the creation date of the resource using the configured expression %s", creationDateLocator));
            }

            // Use Case 2.2 Creation date could be located in the resource
            var dependencyListOptional = dependencyLists.getDependencyListValidAt(resourceCreationDateOptional.get());
            if (dependencyListOptional.isPresent())
                // Use Case 2.2.1 Dependency list for the resource creation date is present
                return validateUsingDependencyList(resourceBody, resourceProvider, dependencyListOptional.get(), profileForValidation, allReferencedProfilesInResource);

            // Use Case 2.2.2 No Dependency list is found for the creation date
            if (isValidateProfileValidityPeriod(validationOptions)) {
                // Use Case 2.2.2.1 ValidityPeriodCheck is turned on and no dependency lists were found -> Profile is outside of validity period!
                String diagnostics = new DiagnosticsMessageBuilder().createValidityPeriodDiagnosticsString(dependencyLists, profileForValidation, resourceCreationDateOptional.get());
                return ValidationResult.createInstance(ResultSeverityEnum.ERROR, ERR_REFV_PROFILE_OUTSIDE_OF_VALIDITY_PERIOD, diagnostics);
            }

            // Use Case 2.2.2.2 ValidityPeriodChek is turned off -> use latest dependencies
            var result = validateUsingDependencyList(resourceBody, resourceProvider, dependencyLists.getLatestDependencyList(), profileForValidation, allReferencedProfilesInResource);
            addWarningAboutDeactivatedValidityPeriodCheckTo(result);
            return result;

        } catch (DataFormatException e) {
            log.debug("Parse error", e);
            return ValidationResult.createInstance(ResultSeverityEnum.ERROR, ERR_REFV_PARSE_ERROR, e.getMessage());
        }
    }

    private ValidationResult validateWithoutConfiguredLocator(String resourceBody, ValidationModuleResourceProvider resourceProvider, Profile profileForValidation, DependencyListsWrapper dependencyLists, List<String> allReferencedProfilesInResource) {
        ValidationResult result;
        log.debug("No creation date locator expression is configured for the profile {}. Using the latest package dependencies available...", profileForValidation);
        var dependencyList = dependencyLists.getLatestDependencyList();
        result = validateUsingDependencyList(resourceBody, resourceProvider, dependencyList, profileForValidation, allReferencedProfilesInResource);
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

    private ValidationResult validateUsingDependencyList(String resourceBody, ValidationModuleResourceProvider resourceProvider, DependencyList dependencyList, Profile profileForValidation, List<String> allReferencedProfilesInResource) {
        log.debug("Applying dependency list: {}", dependencyList);
        var fhirValidator = hapiFhirValidatorCache.computeIfAbsent(dependencyList, k ->
                hapiFhirValidatorFactory.createInstance(
                        dependencyList.getPackages(),
                        resourceProvider
                ));

        var options = new ca.uhn.fhir.validation.ValidationOptions();
        boolean isValidationProfileReferencedInTheResource = allReferencedProfilesInResource.contains(profileForValidation.getCanonical());
        if(!isValidationProfileReferencedInTheResource)
            options.addProfile(profileForValidation.getCanonical());
        var intermediateResult = fhirValidator.validateWithResult(resourceBody, options);
        
        log.debug("Pre-Transformation ValidationResult: Valid: {}, Messages: {}", intermediateResult.isSuccessful(), intermediateResult.getMessages());

        var filteredMessages = severityLevelTransformator.applyTransformations(intermediateResult.getMessages(), dependencyList.getValidationMessageTransformations());

        if(!isValidationProfileReferencedInTheResource) {
            SingleValidationMessage m = new SingleValidationMessage();
            m.setSeverity(ResultSeverityEnum.WARNING);
            m.setMessage(String.format("Resource meta.profile differs from the passed profile for validation: meta.profile=%s; passed=%s", allReferencedProfilesInResource, profileForValidation));
            m.setMessageId(WARN_REFV_PASSED_PROFILE_DIFFERS_FROM_META_PROFILE);
            filteredMessages.add(m);
        }

        return new ValidationResult(filteredMessages);
    }

    private boolean isValidateProfileValidityPeriod(de.gematik.refv.commons.validation.ValidationOptions validationOptions) {
        return validationOptions.getProfileValidityPeriodCheckStrategy() == ProfileValidityPeriodCheckStrategy.VALIDATE;
    }
}