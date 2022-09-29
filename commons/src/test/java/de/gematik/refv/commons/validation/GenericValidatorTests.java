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
import ca.uhn.fhir.util.ClasspathUtil;
import de.gematik.refv.commons.ReferencedProfileLocator;
import de.gematik.refv.commons.configuration.PackageDefinition;
import de.gematik.refv.commons.configuration.PackageDefinitionByVersion;
import de.gematik.refv.commons.configuration.PackageReference;
import de.gematik.refv.commons.configuration.PackageReferenceForAProfileVersion;
import de.gematik.refv.commons.configuration.SupportedProfileVersions;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import de.gematik.refv.commons.exceptions.UnsupportedProfileException;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;

class GenericValidatorTests {

    private GenericValidator genericValidator;

    @BeforeEach
    public void beforeEach() {
        genericValidator = new GenericValidator(
                FhirContext.forR4(),
                new ReferencedProfileLocator(),
                new GenericValidatorFactory(),
                new SeverityLevelTransformator()
        );
    }

    @Test
    @SneakyThrows
    void testUnknownProfileThrowsException() {
        var configuration = new ValidationModuleConfiguration();
        var input = "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                + "    <meta>\n"
                + "        <profile value=\"https://bla.bla|1.0.2\"/>\n"
                + "        \n"
                + "    </meta>\n"
                + "</Bundle>";
        Assertions.assertThrows(UnsupportedProfileException.class, () -> genericValidator.validate(input, configuration));
    }

    @Test
    @SneakyThrows
    void testUnknownPackageThrowsException() {
        var configuration = new ValidationModuleConfiguration();
        configuration.getSupportedProfiles().put("https://bla.bla",new SupportedProfileVersions(new HashMap<>() {{
            put("1.0.2", new PackageReferenceForAProfileVersion(new PackageReference("unknownpackage", "1.0.0")));
        }}));
        var input = "<Bundle xmlns=\"http://hl7.org/fhir\">\n"
                + "    <id value=\"fb16b9fb-eca9-4a64-b257-083ac87c9c9c\"/>\n"
                + "    <meta>\n"
                + "        <profile value=\"https://bla.bla|1.0.2\"/>\n"
                + "        \n"
                + "    </meta>\n"
                + "</Bundle>";
        Assertions.assertThrows(UnsupportedProfileException.class, () -> genericValidator.validate(input, configuration));
    }

    @Test
    @SneakyThrows
    void testValidate() {
        var configuration = new ValidationModuleConfiguration();
        var input = ClasspathUtil.loadResourceAsStream("classpath:" + "Organization-TIOrganizationExample001.xml");
        configuration.getSupportedProfiles().put("https://gematik.de/fhir/directory/StructureDefinition/TIOrganization",new SupportedProfileVersions(new HashMap<>() {{
            put("0.0.0", new PackageReferenceForAProfileVersion(new PackageReference("de.gematik.fhir.directory", "0.2.0")));
        }}));
        configuration.getFhirPackages().put("de.gematik.fhir.directory", new PackageDefinitionByVersion(new HashMap<>() {{
            put("0.2.0", new PackageDefinition("de.gematik.fhir.directory-0.2.0.tgz", new LinkedList<>(),new LinkedList<>()));
        }}));

        genericValidator.validate(IOUtils.toString(input, StandardCharsets.UTF_8), configuration);
    }
}
