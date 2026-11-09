/*-
 * #%L
 * Reference Validator Library
 * %%
 * Copyright (C) 2024 - 2026 gematik GmbH
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
package de.gematik.refv.lib.fhir_context.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration.AnyExtensionPolicy;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration.ExampleCodeSystemsUsagePolicy;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration.ExampleUrlsUsagePolicy;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration.ValidationLevelPolicy;
import java.util.stream.Stream;
import org.hl7.fhir.services.elementmodel.Element.SpecialElement;
import org.hl7.fhir.services.validation.constants.ContainedReferenceValidationPolicy;
import org.hl7.fhir.services.validation.constants.ReferenceValidationPolicy;
import org.hl7.fhir.validation.ValidationEngine;
import org.hl7.fhir.validation.instance.advisor.BasePolicyAdvisorForFullValidation;
import org.hl7.fhir.validation.service.utils.ValidationLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class ValidationEngineFactoryTest {

  @ParameterizedTest
  @MethodSource("validationLevelCases")
  void validationLevelPolicySelectsHL7Level(
      ValidationLevelPolicy policy, ValidationLevel expectedLevel) {
    var parameters =
        ValidationEngineFactory.buildInstanceValidationParameters(
            configuration(
                policy,
                AnyExtensionPolicy.DISALLOWED,
                ExampleCodeSystemsUsagePolicy.DISALLOWED,
                ExampleUrlsUsagePolicy.ALLOWED));

    assertEquals(expectedLevel, parameters.getLevel());
  }

  static Stream<Arguments> validationLevelCases() {
    return Stream.of(
        Arguments.of(ValidationLevelPolicy.SHOW_ALL, ValidationLevel.HINTS),
        Arguments.of(ValidationLevelPolicy.SHOW_WARNINGS_AND_ERRORS, ValidationLevel.WARNINGS),
        Arguments.of(ValidationLevelPolicy.SHOW_ONLY_ERRORS, ValidationLevel.ERRORS));
  }

  @Test
  void anyExtensionPolicyIsPassedToHL7ValidatorParameters() {
    var allowed =
        ValidationEngineFactory.buildInstanceValidationParameters(
            configuration(
                ValidationLevelPolicy.SHOW_WARNINGS_AND_ERRORS,
                AnyExtensionPolicy.ALLOWED,
                ExampleCodeSystemsUsagePolicy.DISALLOWED,
                ExampleUrlsUsagePolicy.ALLOWED));
    var disallowed =
        ValidationEngineFactory.buildInstanceValidationParameters(
            configuration(
                ValidationLevelPolicy.SHOW_WARNINGS_AND_ERRORS,
                AnyExtensionPolicy.DISALLOWED,
                ExampleCodeSystemsUsagePolicy.DISALLOWED,
                ExampleUrlsUsagePolicy.ALLOWED));

    assertTrue(allowed.getExtensions().contains("any"));
    assertTrue(disallowed.getExtensions().isEmpty());
  }

  @ParameterizedTest
  @MethodSource("examplePolicyCases")
  void exampleUrlsAreAllowedOnlyWhenBothExamplePoliciesAllowThem(
      ExampleCodeSystemsUsagePolicy codeSystemsPolicy,
      ExampleUrlsUsagePolicy urlsPolicy,
      boolean expectedAllowed) {
    var parameters =
        ValidationEngineFactory.buildInstanceValidationParameters(
            configuration(
                ValidationLevelPolicy.SHOW_WARNINGS_AND_ERRORS,
                AnyExtensionPolicy.DISALLOWED,
                codeSystemsPolicy,
                urlsPolicy));

    assertEquals(expectedAllowed, parameters.isAllowExampleUrls());
  }

  static Stream<Arguments> examplePolicyCases() {
    return Stream.of(
        Arguments.of(ExampleCodeSystemsUsagePolicy.ALLOWED, ExampleUrlsUsagePolicy.ALLOWED, true),
        Arguments.of(
            ExampleCodeSystemsUsagePolicy.DISALLOWED, ExampleUrlsUsagePolicy.ALLOWED, false),
        Arguments.of(
            ExampleCodeSystemsUsagePolicy.ALLOWED, ExampleUrlsUsagePolicy.DISALLOWED, false),
        Arguments.of(
            ExampleCodeSystemsUsagePolicy.DISALLOWED, ExampleUrlsUsagePolicy.DISALLOWED, false));
  }

  @Test
  void referencePolicyMatchesCoreCliDefaultAndStillValidatesEmbeddedResources() {
    var engine = mock(ValidationEngine.class);

    ValidationEngineFactory.configureDefaultReferencePolicy(engine);

    var advisorCaptor = ArgumentCaptor.forClass(BasePolicyAdvisorForFullValidation.class);
    verify(engine).setPolicyAdvisor(advisorCaptor.capture());
    var advisor = advisorCaptor.getValue();
    assertEquals(
        ReferenceValidationPolicy.IGNORE,
        advisor.policyForReference(null, null, "path", "Medication/1", null));
    assertEquals(
        ContainedReferenceValidationPolicy.CHECK_VALID,
        advisor.policyForContained(
            null, null, null, null, "Parameters", "id", SpecialElement.PARAMETER, "path", "url"));
  }

  private static ContextConfiguration configuration(
      ValidationLevelPolicy validationLevelPolicy,
      AnyExtensionPolicy anyExtensionPolicy,
      ExampleCodeSystemsUsagePolicy exampleCodeSystemsUsagePolicy,
      ExampleUrlsUsagePolicy exampleUrlsUsagePolicy) {
    var defaults = ContextConfiguration.defaultConfiguration();
    var defaultPolicy = defaults.validationPolicy();
    return new ContextConfiguration(
        defaults.fhirRelease(),
        defaults.locale(),
        defaults.displayBehavior(),
        defaults.packageLoading(),
        defaults.terminology(),
        new ValidationPolicyConfiguration(
            validationLevelPolicy,
            defaultPolicy.unknownCodeSystemsPolicy(),
            exampleCodeSystemsUsagePolicy,
            exampleUrlsUsagePolicy,
            defaultPolicy.exampleRestReferencesPolicy(),
            defaultPolicy.recursiveModePolicy(),
            anyExtensionPolicy));
  }
}
