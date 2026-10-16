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

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.model.Base;
import org.hl7.fhir.model.ValidationInformation.ValidationMode;
import org.hl7.fhir.model.core.ElementDefinition;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.model.core.VersionResolutionRules;
import org.hl7.fhir.services.elementmodel.Element;
import org.hl7.fhir.services.validation.ValidatorSession;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.validation.instance.InstanceValidator;
import org.hl7.fhir.validation.instance.ResourcePercentageLogger;
import org.hl7.fhir.validation.instance.utils.ElementInfo;
import org.hl7.fhir.validation.instance.utils.NodeStack;
import org.hl7.fhir.validation.instance.utils.ValidationContext;
import org.hl7.fhir.validation.service.model.InstanceValidatorParameters;

/**
 * Custom HL7 InstanceValidator which allows the multiple-profile branch to bypass the
 * 'policyForContained' option.
 */
@SuppressWarnings("java:S2637") // hostServices can be null
final class RecursiveInstanceValidator extends InstanceValidator {
  @SuppressWarnings("ConstantConditions") // HL7's own engine supplies null host/xver services here.
  RecursiveInstanceValidator(InstanceValidator configured, InstanceValidatorParameters parameters) {
    super(configured.getContext(), null, null, new ValidatorSession(), configured.getSettings());
    initializeFromParameters(parameters);
    setValidationLanguage(configured.getValidationLanguage());
    setNoExtensibleWarnings(configured.isNoExtensibleWarnings());
    setFetcher(configured.getFetcher());
    getImplementationGuides().addAll(configured.getImplementationGuides());
    getValidationControl().putAll(configured.getValidationControl());
    setAIService(configured.getAIService());
    setCacheFolder(configured.getCacheFolder());
    setLogProgress(configured.isLogProgress());
    setPolicyAdvisor(configured.getPolicyAdvisor());
  }

  @Override
  public boolean checkChildByDefinition(
      ValidationContext valContext,
      List<ValidationMessage> errors,
      StructureDefinition profile,
      ElementDefinition definition,
      Element resource,
      Element element,
      String actualType,
      NodeStack stack,
      boolean inCodeableConcept,
      boolean checkDisplayInContext,
      ElementInfo ei,
      String extensionUrl,
      ElementDefinition checkDefn,
      boolean isSlice,
      ResourcePercentageLogger pct,
      ValidationMode mode) {
    var special = ei.getElement().getSpecial();
    if ((special == Element.SpecialElement.BUNDLE_ENTRY
            || special == Element.SpecialElement.PARAMETER)
        && checkDefn.getTypeList().stream().anyMatch(type -> type.getProfileList().size() > 1)) {
      var replacement = typeOnlyDefinition(checkDefn);
      if (replacement != checkDefn) {
        var index = profile.getSnapshot().getElementList().indexOf(checkDefn);
        if (index >= 0) {
          // Never alter loaded snapshots: they can be shared by concurrent context clones.
          var copy = profile.copy(Base.COPY_NOTHING);
          copy.getSnapshot().getElementList().set(index, replacement);
          profile = copy;
          checkDefn = replacement;
        }
      }
    }
    return super.checkChildByDefinition(
        valContext,
        errors,
        profile,
        definition,
        resource,
        element,
        actualType,
        stack,
        inCodeableConcept,
        checkDisplayInContext,
        ei,
        extensionUrl,
        checkDefn,
        isSlice,
        pct,
        mode);
  }

  ElementDefinition typeOnlyDefinition(ElementDefinition definition) {
    var types = new ArrayList<ElementDefinition.TypeRefComponent>();
    for (var type : definition.getTypeList()) {
      if (!type.hasProfile()) {
        types.add(type.copy(Base.COPY_NOTHING));
        continue;
      }
      for (var canonical : type.getProfileList()) {
        var candidate =
            getContext()
                .fetchResource(
                    StructureDefinition.class,
                    canonical.getValue(),
                    VersionResolutionRules.defaultRule());
        // Preserve HL7's normal diagnostics if a permitted profile cannot be resolved.
        if (candidate == null || !candidate.hasType()) {
          return definition;
        }
        var permitted = type.copy(Base.COPY_NOTHING);
        permitted.setCode(candidate.getType());
        permitted.getProfileList().clear();
        if (types.stream().noneMatch(existing -> existing.getCode().equals(permitted.getCode()))) {
          types.add(permitted);
        }
      }
    }
    var copy = definition.copy(Base.COPY_NOTHING);
    copy.getTypeList().clear();
    copy.getTypeList().addAll(types);
    return copy;
  }
}
