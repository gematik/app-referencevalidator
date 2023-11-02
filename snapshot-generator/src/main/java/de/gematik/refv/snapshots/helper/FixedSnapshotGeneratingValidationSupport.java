package de.gematik.refv.snapshots.helper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.hapi.converters.canonical.VersionCanonicalizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.validator.ProfileKnowledgeWorkerR5;
import org.hl7.fhir.common.hapi.validation.validator.VersionSpecificWorkerContextWrapper;
import org.hl7.fhir.exceptions.DefinitionException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.conformance.profile.ProfileKnowledgeProvider;
import org.hl7.fhir.r5.conformance.profile.ProfileUtilities;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.utilities.validation.ValidationMessage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Modified from <a href="https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/support/VersionIgnoringSnapshotGeneratingValidationSupport.java">https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/support/VersionIgnoringSnapshotGeneratingValidationSupport.java</a>
 * Copyright 2022 Deutscher Apothekerverband (DAV), Apache License, Version 2.0
 * Due to the issue in the base class an overwrite of the generating routine is needed, see <a href="https://github.com/hapifhir/hapi-fhir/issues/3942">https://github.com/hapifhir/hapi-fhir/issues/3942</a>
 */
@Slf4j
public class FixedSnapshotGeneratingValidationSupport extends SnapshotGeneratingValidationSupport {

    private final FhirContext myCtx;
    private final VersionCanonicalizer myVersionCanonicalizer;

    /**
     * Constructor
     * @param theCtx {@link FhirContext}
     */
    public FixedSnapshotGeneratingValidationSupport(FhirContext theCtx) {
        super(theCtx);
        myCtx = theCtx;
        myVersionCanonicalizer = new VersionCanonicalizer(theCtx);
    }

    @Nullable
    @Override
    /*
      The method has been cleaned up of unused functionality, such as support for FHIR R5 (reference validator supports R4 only)
     */
    public IBaseResource generateSnapshot(ValidationSupportContext theValidationSupportContext, IBaseResource theInput, String theUrl, String theWebUrl, String theProfileName) {
        FhirVersionEnum version = theInput.getStructureFhirVersionEnum();
        assert version == myCtx.getVersion().getVersion();

        Validate.notNull(myVersionCanonicalizer, "Can not generate snapshot for version: %s", version);

        org.hl7.fhir.r5.model.StructureDefinition inputCanonical = myVersionCanonicalizer.structureDefinitionToCanonical(theInput);
        final String inputUrl = inputCanonical.getUrl();

        if(!inputCanonical.getSnapshot().getElement().isEmpty()) {
            log.info("A snapshot already exists for: {}", inputUrl);
            return theInput;
        }

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

            org.hl7.fhir.r5.model.StructureDefinition baseCanonical = myVersionCanonicalizer.structureDefinitionToCanonical(base);

            if (baseCanonical.getSnapshot().getElement().isEmpty()) {
                // If the base definition also doesn't have a snapshot, generate that first
                theValidationSupportContext.getRootValidationSupport().generateSnapshot(theValidationSupportContext, base, null, null, null);
                baseCanonical = myVersionCanonicalizer.structureDefinitionToCanonical(base);
            }

            ArrayList<ValidationMessage> messages = new ArrayList<>();
            ProfileKnowledgeProvider profileKnowledgeProvider = new ProfileKnowledgeWorkerR5(myCtx);
            IWorkerContext context = new VersionSpecificWorkerContextWrapper(theValidationSupportContext, myVersionCanonicalizer);
            ProfileUtilities profileUtilities = new ProfileUtilities(context, messages, profileKnowledgeProvider);
            profileUtilities.setThrowException(true);
            profileUtilities.generateSnapshot(baseCanonical, inputCanonical, theUrl, theWebUrl, theProfileName);

            // Process snapshotGeneration messages (not in HAPI yet!!!)
            logValidationMessages(messages, inputUrl);
            var errors = messages.stream().filter(m -> m.getLevel().isError()).collect(Collectors.toList());
            if(!errors.isEmpty()) {
                throw new DefinitionException("Could not generate snapshot for " + inputUrl);
            }

            org.hl7.fhir.r4.model.StructureDefinition generatedR4 = (org.hl7.fhir.r4.model.StructureDefinition) myVersionCanonicalizer.structureDefinitionFromCanonical(inputCanonical);
            ((org.hl7.fhir.r4.model.StructureDefinition) theInput).getSnapshot().getElement().clear();
            ((org.hl7.fhir.r4.model.StructureDefinition) theInput).getSnapshot().getElement().addAll(generatedR4.getSnapshot().getElement());
        } finally {
            theValidationSupportContext.getCurrentlyGeneratingSnapshots().remove(inputUrl);
        }

        return theInput;


    }

    private void logValidationMessages(ArrayList<ValidationMessage> messages, String inputUrl) {
        for(ValidationMessage e : messages) {
            if(e.getLevel().isError())
                log.error("Error while snapshot generation for {} : {}", inputUrl, e);
            else if(e.getLevel() == ValidationMessage.IssueSeverity.WARNING)
                log.warn("Warning while snapshot generation for {} : {}", inputUrl, e);
            else
                log.info("Info while snapshot generation for {} : {}", inputUrl, e);
        }
    }
}

