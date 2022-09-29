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

package de.gematik.refv.commons.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.util.ClasspathUtil;
import de.gematik.refv.commons.validation.support.FixedSnapshotGeneratingValidationSupport;
import lombok.SneakyThrows;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Original file: <a href="https://github.com/hapifhir/hapi-fhir/blob/master/hapi-fhir-validation/src/test/java/org/hl7/fhir/r4/validation/SnapshotGeneratorR4Test.java">https://github.com/hapifhir/hapi-fhir/blob/master/hapi-fhir-validation/src/test/java/org/hl7/fhir/r4/validation/SnapshotGeneratorR4Test.java</a>
 * Copied as regression test for FixedSnapshotGeneratingValidationSupport, which fixes SnapshotGeneratingValidationSupport
 */
class FixedSnapshotGeneratingValidationSupportTests {

    private static final FhirContext fhirContext = FhirContext.forR4();

    private FixedSnapshotGeneratingValidationSupport snapshotGeneratingValidationSupport;

    private ValidationSupportChain chain;

    @BeforeEach
    public void beforeTest() {
        snapshotGeneratingValidationSupport = new FixedSnapshotGeneratingValidationSupport(fhirContext);
        chain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext),
                snapshotGeneratingValidationSupport
        );
    }

    @Test
    @SneakyThrows
    void testGenerateSnapshotWithDerivationDepthOf1() {
        StructureDefinition differential = loadResourceFromClasspath("profile-differential-patient-r4.json");

        // Generate the snapshot
        StructureDefinition snapshot = (StructureDefinition) snapshotGeneratingValidationSupport.generateSnapshot(new ValidationSupportContext(chain), differential, "http://foo", null, "THE BEST PROFILE");

        /*
        FileWriter writer = new FileWriter(Paths.get("src/test/resources/snapshot.json").toFile());
        fhirContext.newJsonParser().encodeResourceToWriter(snapshot, writer);
        writer.close();
        */
        Assertions.assertNotNull(snapshot);
        Assertions.assertEquals(54, snapshot.getSnapshot().getElement().size());
    }

    @Test
    @SneakyThrows
    void testGenerateSnapshotWithDerivationDepthOf2() {
        StructureDefinition differential = loadResourceFromClasspath("profile-differential-patient-r4-derived.json");

        PrePopulatedValidationSupport support = new PrePopulatedValidationSupport(fhirContext);
        support.addResource(loadResourceFromClasspath("profile-differential-patient-r4.json"));
        chain.addValidationSupport(support);
        chain.addValidationSupport(snapshotGeneratingValidationSupport);

        // Generate the snapshot
        StructureDefinition snapshot = (StructureDefinition) snapshotGeneratingValidationSupport.generateSnapshot(new ValidationSupportContext(chain), differential, "http://foo-derived", null, "THE BEST PROFILE DERIVED");

        Assertions.assertNotNull(snapshot);
        Assertions.assertEquals(55, snapshot.getSnapshot().getElement().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "profile-differential-patient-r4-with-unknown-base.json",
            "profile-differential-patient-r4-with-no-base.json"
    })
    @SneakyThrows
    void testThrowsExceptionWithUnknownBase(String filename) {
        StructureDefinition differential = loadResourceFromClasspath(filename);

        ValidationSupportContext theValidationSupportContext = new ValidationSupportContext(chain);
        Assertions.assertThrows(PreconditionFailedException.class, () -> snapshotGeneratingValidationSupport.generateSnapshot(theValidationSupportContext, differential, "http://foo", null, "THE BEST PROFILE"));
    }

    protected StructureDefinition loadResourceFromClasspath(String filename) {
        IParser newJsonParser = fhirContext.newJsonParser();
        return newJsonParser.parseResource(StructureDefinition.class, ClasspathUtil.loadResourceAsStream("classpath:" + filename));
    }
}
