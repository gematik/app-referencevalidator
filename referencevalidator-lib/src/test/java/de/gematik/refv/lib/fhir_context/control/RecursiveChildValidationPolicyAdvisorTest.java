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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration.RecursiveModePolicy;
import org.hl7.fhir.model.utilities.formats.FhirFormat;
import org.hl7.fhir.services.elementmodel.Element.SpecialElement;
import org.hl7.fhir.services.validation.IValidationPolicyAdvisor;
import org.hl7.fhir.services.validation.constants.ContainedReferenceValidationPolicy;
import org.hl7.fhir.services.validation.constants.ReferenceValidationPolicy;
import org.hl7.fhir.validation.ValidationEngine;
import org.hl7.fhir.validation.instance.InstanceValidator;
import org.hl7.fhir.validation.instance.advisor.BasePolicyAdvisorForFullValidation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;

class RecursiveChildValidationPolicyAdvisorTest {
  @ParameterizedTest
  @EnumSource(
      value = SpecialElement.class,
      names = {"BUNDLE_ENTRY", "PARAMETER"})
  void embeddedResourcesReceiveTypeChecksOnly(SpecialElement special) {
    var delegate = mock(IValidationPolicyAdvisor.class);
    var advisor = new RecursiveChildValidationPolicyAdvisor(delegate);
    clearInvocations(delegate);

    assertEquals(
        ContainedReferenceValidationPolicy.CHECK_TYPE,
        advisor.policyForContained(
            null, null, null, null, "Parameters", "id", special, "path", "url"));
    verifyNoInteractions(delegate);
  }

  @ParameterizedTest
  @EnumSource(
      value = SpecialElement.class,
      mode = EnumSource.Mode.EXCLUDE,
      names = {"BUNDLE_ENTRY", "PARAMETER"})
  @NullSource
  void otherResourcesRetainTheDelegatedPolicy(SpecialElement special) {
    var delegate = mock(IValidationPolicyAdvisor.class);
    when(delegate.policyForContained(
            null, null, null, null, "Patient", "id", special, "path", "url"))
        .thenReturn(ContainedReferenceValidationPolicy.CHECK_VALID);
    var advisor = new RecursiveChildValidationPolicyAdvisor(delegate);

    assertEquals(
        ContainedReferenceValidationPolicy.CHECK_VALID,
        advisor.policyForContained(
            null, null, null, null, "Patient", "id", special, "path", "url"));
    verify(delegate)
        .policyForContained(null, null, null, null, "Patient", "id", special, "path", "url");
  }

  @ParameterizedTest
  @EnumSource(RecursiveModePolicy.class)
  void factoryInstallsTheAdvisorOnlyForRecursiveMode(RecursiveModePolicy recursion)
      throws Exception {
    var engine = mock(ValidationEngine.class);
    var defaults = ValidationPolicyConfiguration.defaultConfiguration();
    var policy =
        new ValidationPolicyConfiguration(
            defaults.validationLevelPolicy(),
            defaults.unknownCodeSystemsPolicy(),
            defaults.exampleCodeSystemsUsagePolicy(),
            defaults.exampleUrlsUsagePolicy(),
            defaults.exampleRestReferencesPolicy(),
            recursion,
            defaults.anyExtensionPolicy());
    if (recursion == RecursiveModePolicy.ALLOWED) {
      var validator = mock(InstanceValidator.class);
      when(engine.getValidator(FhirFormat.JSON)).thenReturn(validator);
      when(validator.getPolicyAdvisor())
          .thenReturn(
              new BasePolicyAdvisorForFullValidation(ReferenceValidationPolicy.CHECK_VALID, null));
    }

    ValidationEngineFactory.configureRecursiveChildValidation(engine, policy);

    if (recursion == RecursiveModePolicy.DISALLOWED) {
      verifyNoInteractions(engine);
    } else {
      var captured = ArgumentCaptor.forClass(IValidationPolicyAdvisor.class);
      verify(engine).setPolicyAdvisor(captured.capture());
      assertEquals(
          ContainedReferenceValidationPolicy.CHECK_TYPE,
          captured
              .getValue()
              .policyForContained(
                  null,
                  null,
                  null,
                  null,
                  "Bundle",
                  "id",
                  SpecialElement.BUNDLE_ENTRY,
                  "path",
                  "url"));
      assertEquals(
          ContainedReferenceValidationPolicy.CHECK_VALID,
          captured
              .getValue()
              .policyForContained(
                  null,
                  null,
                  null,
                  null,
                  "Patient",
                  "id",
                  SpecialElement.CONTAINED,
                  "path",
                  "url"));
    }
  }
}
