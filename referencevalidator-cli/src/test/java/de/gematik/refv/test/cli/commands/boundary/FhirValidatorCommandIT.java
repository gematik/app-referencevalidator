/*-
 * #%L
 * referencevalidator-cli
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
package de.gematik.refv.test.cli.commands.boundary;

import de.gematik.refv.cli.commands.boundary.FhirValidatorCommand;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class FhirValidatorCommandIT {

  private Path reportOutputHtml;
  private Path reportOutputJson;
  private static final String PROFILE_URL = "http://hl7.org/fhir/StructureDefinition/Patient|4.0.1";

  @TempDir Path tempDir;

  @BeforeEach
  void beforeEach() {
    reportOutputHtml = tempDir.resolve("output.html");
    reportOutputJson = tempDir.resolve("output.json");
  }

  @DisplayName(
      "Given a valid resource, when validating, then the command succeeds and writes a"
          + " JSON report")
  @Test
  void expectValidResourceIsValidatedSuccessfully() throws IOException {
    // Given a valid FHIR resource
    final Path resource = writeResource(patientJson(true));

    // When the validation is executed
    final int exitCode =
        executeCommand(
            "--profile",
            "http://hl7.org/fhir/StructureDefinition/Patient|4.0.1",
            "--json",
            "--report",
            reportOutputJson.toString(),
            resource.toString());

    // Then the command succeeds and the JSON report has been written
    Assertions.assertEquals(0, exitCode);
    Assertions.assertTrue(Files.exists(reportOutputJson), "Expected the JSON report to be written");
    Assertions.assertTrue(Files.size(reportOutputJson) > 0);
  }

  @DisplayName(
      "Given an invalid resource, when validating, then the command fails with exit code 1")
  @Test
  void expectInvalidResourceFailsValidation() throws IOException {
    // Given a validation module and an invalid FHIR resource (required name missing)
    final Path resource = writeResource(patientJson(false));

    // When the validation is executed
    final int exitCode =
        executeCommand(
            "--modules-folder",
            "../referencevalidator-lib/src/test/resources/modules",
            "--module-name",
            "isik5",
            "--report",
            reportOutputHtml.toString(),
            resource.toString());

    // Then the command reports the resource as invalid
    Assertions.assertEquals(1, exitCode);
  }

  @DisplayName(
      "Given a resource without a profile reference, when validating with an explicit profile,"
          + " then the command fails")
  @Test
  void expectValidationWithExplicitProfileOptionFails() throws IOException {
    // Given a validation module and a FHIR resource without meta.profile
    final Path resource = writeResource(patientJsonWithoutProfile());

    // When the validation is executed with an explicit profile
    final int exitCode =
        executeCommand(
            "--modules-folder",
            "../referencevalidator-lib/src/test/resources/modules",
            "--module-name",
            "isik5",
            "--profile",
            PROFILE_URL,
            "--report",
            reportOutputHtml.toString(),
            resource.toString());

    // Then the command succeeds
    Assertions.assertEquals(1, exitCode);
  }

  @DisplayName(
      "Given a resource referencing an unsupported profile, when validating, then the command"
          + " fails with exit code 1")
  @Test
  void expectUnsupportedProfileFails() throws IOException {
    // Given a validation module and a resource referencing an unknown profile
    final Path resource =
        writeResource(
            """
            {
              "resourceType": "Patient",
              "meta": { "profile": [ "http://example.org/fhir/StructureDefinition/unknown" ] },
              "name": [ { "family": "Doe" } ]
            }
            """);

    // When the validation is executed
    final int exitCode =
        executeCommand(
            "--modules-folder",
            "../referencevalidator-lib/src/test/resources/modules",
            "--module-name",
            "isik5",
            "--report",
            reportOutputHtml.toString(),
            resource.toString());

    // Then the command fails
    Assertions.assertEquals(1, exitCode);
  }

  @DisplayName(
      "Given multiple resources in a directory, when validating, then all files are validated"
          + " concurrently")
  @Test
  void expectDirectoryValidationWorks() throws IOException {
    // Given a validation module and a directory with a valid and an invalid resource
    final Path resourcesDir = Files.createDirectories(tempDir.resolve("resources"));
    Files.writeString(resourcesDir.resolve("valid.json"), patientJson(true));
    Files.writeString(resourcesDir.resolve("invalid.json"), patientJson(false));

    // When the validation is executed on the directory
    final int exitCode =
        executeCommand(
            "--modules-folder",
            "../referencevalidator-lib/src/test/resources/modules",
            "--module-name",
            "isik5",
            "--report",
            "output.html",
            resourcesDir.toString());

    // Then the command fails because one of the resources is invalid
    Assertions.assertEquals(1, exitCode);
  }

  private int executeCommand(String... args) {
    final String[] baseArgs = {"--fhir-version", "R4", "--locale", "de"};
    final String[] allArgs = java.util.Arrays.copyOf(args, args.length + baseArgs.length);
    System.arraycopy(baseArgs, 0, allArgs, args.length, baseArgs.length);
    return new CommandLine(new FhirValidatorCommand()).execute(allArgs);
  }

  private Path writeResource(String content) throws IOException {
    final Path resource = tempDir.resolve("patient-" + System.nanoTime() + ".json");
    Files.writeString(resource, content);
    return resource;
  }

  private static String patientJson(boolean withName) {
    return """
        {
          "resourceType": "Patient",
          "meta": { "profile": [ "%s" ] }
          %s
        }
        """
        .formatted(PROFILE_URL, withName ? ", \"name\": [ { \"family\": \"Doe\" } ]" : "");
  }

  private static String patientJsonWithoutProfile() {
    return """
        {
          "resourceType": "Patient",
          "name": [ { "family": "Doe" } ]
        }
        """;
  }
}
