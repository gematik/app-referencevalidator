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
package de.gematik.refv.cli;

import de.gematik.refv.cli.support.TestAppender;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@Slf4j
class ReferenceValidatorIT {


    TestAppender appender;

    @BeforeEach
    void beforeEach() {
        appender = new TestAppender();
        Logger.getRootLogger().addAppender(appender);
    }

    @AfterEach
    void afterEach() {
        Logger.getRootLogger().removeAppender(appender);
    }

    @Test
    void testVerboseMode() {
        String input = "erp src/test/resources/erp-test.xml -v";
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        boolean isValid = appender.getLogs().stream().anyMatch(e -> e.getLevel().equals(Level.DEBUG));
        Assertions.assertTrue(isValid,"No debug messages in verbose mode found");
    }

    @Test
    void testValidationAgainstProfile() {
        String profileToValidate = "http://fhir.org/StructureDefinition/Bundle";
        String input = "core src/test/resources/core-test.xml -p " + profileToValidate + " -v";
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        boolean profileTakenIntoAccount = appender.getLogs().stream().anyMatch(e -> e.getMessage().toString().contains(profileToValidate));
        Assertions.assertTrue(profileTakenIntoAccount,"Passed profile has not been taken into account while validation");

        boolean warningIssued = appender.getLogs().stream().anyMatch(l -> l.getMessage().toString().contains("WARNING")) && appender.getLogs().stream().anyMatch(l -> l.getMessage().toString().contains("REFV_WARN_PASSED_PROFILE_DIFFERS_FROM_META_PROFILE"));
        Assertions.assertTrue(warningIssued,"No warning issued about profile differences");
    }

    @Test
    void testOnlyErrorsInOutput() {
        String input = "erp src/test/resources/erp-test-error.xml";
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        boolean isValid = appender.getLogs().stream().noneMatch(l -> l.getMessage().toString().contains("INFORMATION")) && appender.getLogs().stream().anyMatch(l -> l.getMessage().toString().contains("ERROR"));
        Assertions.assertTrue(isValid,"Information message found in output - only errors are allowed by default");
    }

    @Test
    void testAcceptedEncodingsLeeadsToInvalidInstancesIfViolated() {
        String input = "core src/test/resources/core-test.json --accepted-encodings xml";
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        boolean isValid = appender.getLogs().stream().anyMatch(l -> l.getMessage().toString().contains("ERROR"));
        Assertions.assertTrue(isValid,"Errors have not been found in output but expected");
    }

    @Test
    void testAcceptedEncodingsListIsParsedCorrectly() {
        String input = "core src/test/resources/core-test.json --accepted-encodings xml,json";
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        boolean isValid = appender.getLogs().stream().noneMatch(l -> l.getMessage().toString().contains("ERROR"));
        Assertions.assertTrue(isValid,"Errors have been found in output but none expected");
    }



    @Test
    void testNoValidityPeriodCheck() {
        String input = "erp src/test/resources/DAV-PR-ERP-Abgabeinformationen-validity-period-not-started.xml -nvp";
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        boolean isValid = appender.getLogs().stream().noneMatch(l -> l.getMessage().toString().contains("ERROR"));
        Assertions.assertTrue(isValid,"No errors has been found");
    }

    @Test
    void testModuleConfiguration() {
        String input = "erp --module-info";
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        boolean isValid = appender.getLogs().stream().anyMatch(l -> l.getMessage().toString().contains("Supported profiles:") && l.getMessage().toString().contains("FHIR-Package"));
        Assertions.assertTrue(isValid, "No information on supported profiles and FHIR-Packages found");
    }

    @Test
    void testInfoOnEmptyInput() {

        ReferenceValidator.main(new String[]{});

        boolean isValid = appender.getLogs().stream().anyMatch(l -> l.getMessage().toString().contains("Usage"));
        Assertions.assertTrue(isValid, "No information found on empty parameters");
    }

    @Test
    void testUnsupportedModule() {
        String input = "unsupported-module src/test/resources/DAV-PR-ERP-Abgabeinformationen-validity-period-not-started.xml";
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        boolean isValid = appender.getLogs().stream().anyMatch(l -> l.getMessage().toString().contains("unsupported. Supported modules:"));
        Assertions.assertTrue(isValid, "No information found on supported modules");
    }
}
