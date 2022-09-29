/*
 * Copyright (c) 2022 gematik GmbH
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

package de.gematik.refv.commons.validation.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.validator.ProfileKnowledgeWorkerR5;
import org.hl7.fhir.common.hapi.validation.validator.VersionSpecificWorkerContextWrapper;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.conformance.ProfileUtilities;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Modified from <a href="https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/support/VersionIgnoringSnapshotGeneratingValidationSupport.java">https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/support/VersionIgnoringSnapshotGeneratingValidationSupport.java</a>
 * Copyright 2022 Deutscher Apothekerverband (DAV), Apache License, Version 2.0
 *
 * Due to the issue in the base class an overwrite of the generating routine is needed, see <a href="https://github.com/hapifhir/hapi-fhir/issues/3942">https://github.com/hapifhir/hapi-fhir/issues/3942</a>
 */
public class FixedSnapshotGeneratingValidationSupport extends SnapshotGeneratingValidationSupport {

    private static final Logger log = LoggerFactory.getLogger(FixedSnapshotGeneratingValidationSupport.class);
    private final FhirContext myCtx;

    /**
     * Constructor
     * @param theCtx {@link FhirContext}
     */
    public FixedSnapshotGeneratingValidationSupport(FhirContext theCtx) {
        super(theCtx);
        myCtx = theCtx;
    }

    @Nullable
    @Override
    /*
      The method has been cleaned up of unused functionality, such as support for FHIR R5 (reference validator supports R4 only)
     */
    public IBaseResource generateSnapshot(ValidationSupportContext theValidationSupportContext, IBaseResource theInput, String theUrl, String theWebUrl, String theProfileName) {
            FhirVersionEnum version = theInput.getStructureFhirVersionEnum();
            assert version == myCtx.getVersion().getVersion();

            VersionSpecificWorkerContextWrapper.IVersionTypeConverter converter = newVersionTypeConverter(version);
            Validate.notNull(converter, "Can not generate snapshot for version: %s", version);

            org.hl7.fhir.r5.model.StructureDefinition inputCanonical = (org.hl7.fhir.r5.model.StructureDefinition) converter.toCanonical(theInput);

            final String inputUrl = inputCanonical.getUrl();
            
            if (theValidationSupportContext.getCurrentlyGeneratingSnapshots().contains(inputUrl)) {
                log.debug("Detected circular dependency, already generating snapshot for: {}", inputUrl);
                return theInput;
            }
            
            theValidationSupportContext.getCurrentlyGeneratingSnapshots().add(inputUrl);
            
            try {
                // This is the fix. The try-finally block is moved down to avoid a profile with snapshot being generated be removed from list of currently generated snapshots
                String baseDefinition = inputCanonical.getBaseDefinition();
                if (isBlank(baseDefinition)) {
                    throw new PreconditionFailedException("StructureDefinition[id=" + inputCanonical.getIdElement().getId() + ", url=" + inputCanonical.getUrl() + "] has no base");
                }

                IBaseResource base = theValidationSupportContext.getRootValidationSupport().fetchStructureDefinition(baseDefinition);
                if (base == null) {
                    throw new PreconditionFailedException("Unknown base definition: " + baseDefinition);
                }

                org.hl7.fhir.r5.model.StructureDefinition baseCanonical = (org.hl7.fhir.r5.model.StructureDefinition) converter.toCanonical(base);

                if (baseCanonical.getSnapshot().getElement().isEmpty()) {
                    // If the base definition also doesn't have a snapshot, generate that first
                    theValidationSupportContext.getRootValidationSupport().generateSnapshot(theValidationSupportContext, base, null, null, null);
                    baseCanonical = (org.hl7.fhir.r5.model.StructureDefinition) converter.toCanonical(base);
                }

                ArrayList<ValidationMessage> messages = new ArrayList<>();
                ProfileUtilities.ProfileKnowledgeProvider profileKnowledgeProvider = new ProfileKnowledgeWorkerR5(myCtx);
                IWorkerContext context = new VersionSpecificWorkerContextWrapper(theValidationSupportContext, converter);
                ProfileUtilities profileUtilities = new ProfileUtilities(context, messages, profileKnowledgeProvider);
                profileUtilities.generateSnapshot(baseCanonical, inputCanonical, theUrl, theWebUrl, theProfileName);

                org.hl7.fhir.r4.model.StructureDefinition generatedR4 = (org.hl7.fhir.r4.model.StructureDefinition) converter.fromCanonical(inputCanonical);
                ((org.hl7.fhir.r4.model.StructureDefinition) theInput).getSnapshot().getElement().clear();
                ((org.hl7.fhir.r4.model.StructureDefinition) theInput).getSnapshot().getElement().addAll(generatedR4.getSnapshot().getElement());
            } finally {
               theValidationSupportContext.getCurrentlyGeneratingSnapshots().remove(inputUrl);
           }

            return theInput;


    }
}
