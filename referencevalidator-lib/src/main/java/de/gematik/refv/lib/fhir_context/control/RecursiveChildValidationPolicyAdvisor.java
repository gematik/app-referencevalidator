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

import org.hl7.fhir.model.core.ElementDefinition;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.services.elementmodel.Element;
import org.hl7.fhir.services.validation.IResourceValidator;
import org.hl7.fhir.services.validation.IValidationPolicyAdvisor;
import org.hl7.fhir.services.validation.constants.ContainedReferenceValidationPolicy;
import org.hl7.fhir.validation.instance.advisor.RulesDrivenPolicyAdvisor;

/** Customizes the Rules for checking recursively the children elements of a FHIR Resource. */
final class RecursiveChildValidationPolicyAdvisor extends RulesDrivenPolicyAdvisor {
  RecursiveChildValidationPolicyAdvisor(IValidationPolicyAdvisor delegate) {
    super(delegate);
  }

  @Override
  public ContainedReferenceValidationPolicy policyForContained(
      IResourceValidator validator,
      Object appContext,
      StructureDefinition structure,
      ElementDefinition element,
      String containerType,
      String containerId,
      Element.SpecialElement containingResourceType,
      String path,
      String url) {
    if (containingResourceType == Element.SpecialElement.BUNDLE_ENTRY
        || containingResourceType == Element.SpecialElement.PARAMETER) {
      // Full validation is performed separately using the child's own meta.profile declarations.
      return ContainedReferenceValidationPolicy.CHECK_TYPE;
    }
    return super.policyForContained(
        validator,
        appContext,
        structure,
        element,
        containerType,
        containerId,
        containingResourceType,
        path,
        url);
  }
}
