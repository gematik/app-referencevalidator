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

import java.util.List;
import java.util.Objects;
import org.hl7.fhir.model.Base;
import org.hl7.fhir.model.ValidationInformation.ValidationMode;
import org.hl7.fhir.model.core.ElementDefinition;
import org.hl7.fhir.model.core.StructureDefinition;
import org.hl7.fhir.services.elementmodel.Element;
import org.hl7.fhir.services.validation.ValidatorSession;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.validation.instance.InstanceValidator;
import org.hl7.fhir.validation.instance.ResourcePercentageLogger;
import org.hl7.fhir.validation.instance.utils.ElementInfo;
import org.hl7.fhir.validation.instance.utils.NodeStack;
import org.hl7.fhir.validation.instance.utils.ValidationContext;
import org.hl7.fhir.validation.service.model.InstanceValidatorParameters;

/** Configured HL7 validator that preserves embedded-resource paths in diagnostics. */
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
    int firstChildMessage = errors.size();
    boolean valid =
        super.checkChildByDefinition(
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
            checkDefinitionForChild(checkDefn, ei.getElement()),
            isSlice,
            pct,
            mode);
    if (isEmbeddedResource(ei.getElement().getSpecial()) && ei.getElement().isResource()) {
      prefixChildMessages(errors, firstChildMessage, ei.getPath());
    }
    return valid;
  }

  private static ElementDefinition checkDefinitionForChild(
      ElementDefinition definition, Element child) {
    return isEmbeddedResource(child.getSpecial()) && child.isResource()
        ? selectChildProfileAlternative(definition, child)
        : definition;
  }

  static ElementDefinition selectChildProfileAlternative(
      ElementDefinition definition, Element child) {
    if (definition.getTypeList().size() != 1) {
      return definition;
    }
    var type = definition.getTypeFirstRep();
    if (type.getProfileList().size() < 2) {
      return definition;
    }
    var meta = child.getNamedChild("meta", false);
    if (meta == null) {
      return definition;
    }
    var declaredProfileUrls =
        meta.getChildrenByName("profile").stream()
            .map(Element::getValue)
            .filter(Objects::nonNull)
            .map(RecursiveInstanceValidator::canonicalUrl)
            .distinct()
            .toList();
    var allowedProfileUrls =
        type.getProfileList().stream()
            .map(profile -> canonicalUrl(profile.getValue()))
            .distinct()
            .toList();
    var selectedProfileUrls =
        allowedProfileUrls.stream().filter(declaredProfileUrls::contains).toList();
    if (selectedProfileUrls.isEmpty() || selectedProfileUrls.size() == allowedProfileUrls.size()) {
      return definition;
    }

    var narrowedDefinition = definition.copy(Base.COPY_ALL);
    narrowedDefinition
        .getTypeFirstRep()
        .getProfileList()
        .removeIf(profile -> !selectedProfileUrls.contains(canonicalUrl(profile.getValue())));
    return narrowedDefinition;
  }

  private static String canonicalUrl(String canonical) {
    if (canonical == null) {
      return "";
    }
    int versionSeparator = canonical.indexOf('|');
    return versionSeparator < 0 ? canonical : canonical.substring(0, versionSeparator);
  }

  private static void prefixChildMessages(
      List<ValidationMessage> messages, int firstChildMessage, String childPath) {
    for (int messageIndex = firstChildMessage; messageIndex < messages.size(); messageIndex++) {
      var message = messages.get(messageIndex);
      var content = message.getMessage();
      if (content != null && !hasEmbeddedResourcePath(content)) {
        message.setMessage(stripPathComments(childPath) + ": " + content);
      }
    }
  }

  private static boolean hasEmbeddedResourcePath(String message) {
    return message.startsWith("Bundle.entry[") || message.startsWith("Parameters.parameter[");
  }

  private static String stripPathComments(String path) {
    return path.replaceAll("/\\*[^*]*\\*/", "");
  }

  private static boolean isEmbeddedResource(Element.SpecialElement special) {
    return special == Element.SpecialElement.BUNDLE_ENTRY
        || special == Element.SpecialElement.PARAMETER;
  }
}
