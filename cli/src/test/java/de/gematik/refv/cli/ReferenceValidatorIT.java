/*-
 * #%L
 * referencevalidator-cli
 * %%
 * Copyright (C) 2025 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.cli;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.gematik.refv.cli.support.TestAppender;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
class ReferenceValidatorIT {


    TestAppender appender;
    IParser xmlParser = FhirContext.forR4().newXmlParser();

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

    @Test
    void testNoFilesForValidation() {
        String input = "erp -o target/output.json";
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        boolean isValid = appender.getLogs().stream().anyMatch(l -> l.getMessage().toString().equals("No file(s) for validation. You need specify at least one file or directory that contains resources for validation"));
        assertThat(isValid).isTrue();
    }

    @Test
    @SneakyThrows
    void testValidateEmptyDirectory() {
        File emptyDir = Files.createTempDirectory("emptyDir").toFile();
        String emptyDirectoryPath = emptyDir.getAbsolutePath();
        String input = "erp " + emptyDirectoryPath;
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        boolean isValid = appender.getLogs().stream().anyMatch(l -> l.getMessage().toString().contains("Directory is empty!") && l.getMessage().toString().contains("does not contain any resources for validation"));
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "erp src/test/resources/operation-outcome/valid.xml -o target/valid-output.json, src/test/resources/operation-outcome/valid-operation-outcome.json",
            "erp src/test/resources/operation-outcome/invalid.xml -o target/invalid-output.json, src/test/resources/operation-outcome/invalid-operation-outcome.json",
            "erp src/test/resources/operation-outcome/exception.xml -o target/exception-output.json, src/test/resources/operation-outcome/exception-operation-outcome.json",
            "erp src/test/resources/operation-outcome/valid.xml -o target/valid-output.xml, src/test/resources/operation-outcome/valid-operation-outcome.xml",
    })
    @SneakyThrows
    void testWriteToFileAsOperationOutcome(String input, String expectedOutputPath) {
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        String expectedOutput = FileUtils.readFileToString(new File(expectedOutputPath), StandardCharsets.UTF_8);
        String actualOutput = FileUtils.readFileToString(new File(args[3]), StandardCharsets.UTF_8);

        assertThat(actualOutput).isEqualTo(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("multipleFileValidationTestData")
    void testValidationOfMultipleFiles(String input, String expectedFilePath) throws Exception {
        String[] args = input.split(" ");
        ReferenceValidator.main(args);

        String expectedOutput = FileUtils.readFileToString(new File(expectedFilePath), StandardCharsets.UTF_8)
                .replaceAll("urn:uuid:[^\"]+", "UUID");
        String actualOutput = FileUtils.readFileToString(new File(args[3]), StandardCharsets.UTF_8)
                .replaceAll("urn:uuid:[^\"]+", "UUID")
                .replaceAll("[^\"]+\\.xml", "FILE_PATH");

        Bundle expectedBundle = (Bundle) xmlParser.parseResource(expectedOutput);
        Bundle actualBundle = (Bundle) xmlParser.parseResource(actualOutput);

        assertThat(actualBundle.equalsShallow(expectedBundle)).isTrue();
    }

    private static Stream<Arguments> multipleFileValidationTestData() {
        return Stream.of(
                Arguments.of("erp src/test/resources/erp-test.xml,src/test/resources/erp-test1.xml -o target/operation-outcome-bundle.xml", "src/test/resources/operation-outcome/operation-outcome-bundle.xml"),
                Arguments.of("erp src/test/resources/operation-outcome/directory1 -o target/operation-outcome-directory.xml", "src/test/resources/operation-outcome/operation-outcome-directory.xml"),
                Arguments.of("erp src/test/resources/operation-outcome/directory1,src/test/resources/operation-outcome/1.xml -o target/operation-outcome-directory-and-file.xml", "src/test/resources/operation-outcome/operation-outcome-directory-and-file.xml"),
                Arguments.of("erp src/test/resources/operation-outcome/directory1,src/test/resources/operation-outcome/1.xml,src/test/resources/operation-outcome/directory2,src/test/resources/operation-outcome/2.xml -o target/operation-outcome-multiple-directories-and-files.xml", "src/test/resources/operation-outcome/operation-outcome-multiple-directories-and-files.xml"),
                Arguments.of("erp src/test/resources/operation-outcome/nested-directory -o target/operation-outcome-nested-directory.xml", "src/test/resources/operation-outcome/operation-outcome-nested-directory.xml")
        );
    }

    @Test
    @SneakyThrows
    void testValidationOfDirectoryFileAndFileWithException() {
        String input = "erp src/test/resources/operation-outcome/directory1,src/test/resources/operation-outcome/1.xml,src/test/resources/eau-test.xml -o target/operation-outcome-folder-file-and-unsupported-file.xml";
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        String expectedOutput = FileUtils.readFileToString(new File("src/test/resources/operation-outcome/operation-outcome-directory-file-and-exception.xml"), StandardCharsets.UTF_8)
                .replaceAll("urn:uuid:[^\"]+", "UUID");
        String actualOutput = FileUtils.readFileToString(new File(args[3]), StandardCharsets.UTF_8)
                .replaceAll("urn:uuid:[^\"]+", "UUID")
                .replaceAll("[^\"]+\\d?\\.xml", "FILE_PATH")
                .replaceAll("[^\"]+eau-test\\d?\\.xml", "FILE_PATH");

        Bundle expectedBundle = (Bundle) xmlParser.parseResource(expectedOutput);
        Bundle actualBundle = (Bundle) xmlParser.parseResource(actualOutput);

        assertThat(actualBundle.equalsShallow(expectedBundle)).isTrue();
    }

    @Test
    @SneakyThrows
    void testProfileFilterWhichDoesntMatchAnyMetaProfileReference() {
        String input = "erp src/test/resources/erp-test.xml --profile-filter NON-MATCHING-PATTERN";
        String [] args = input.split(" ");

        ReferenceValidator.main(args);

        assertThat(appender.getLogs().stream()).as("No errors found while profile filter doesn't match").anyMatch(e -> e.getMessage().toString().contains("REFV_PROFILE_FILTER_MISMATCH"));
    }
}
