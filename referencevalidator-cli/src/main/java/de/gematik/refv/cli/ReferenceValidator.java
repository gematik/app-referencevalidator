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
package de.gematik.refv.cli;

import de.gematik.refv.cli.commands.VersionProvider;
import de.gematik.refv.cli.commands.boundary.ConfigCommand;
import de.gematik.refv.cli.commands.boundary.FhirValidatorCommand;
import de.gematik.refv.cli.commands.boundary.SnapshotGeneratorCommand;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** CLI entry point for the reference validator. */
@CommandLine.Command(
    name = "java -jar referencevalidator-cli.jar",
    mixinStandardHelpOptions = true,
    version = VersionProvider.PROJECT_VERSION,
    description =
        """

          The validator checks conformance of FHIR resources to the underlying specification, through plugins that might bring pre-compiled FHIR-/terminology packages and pre-defined validation configuration.
          It supports expansion of ValueSets, generation of snapshots for profiles, and can output validation reports in both human-readable text and JSON formats.""",
    subcommands = {FhirValidatorCommand.class, SnapshotGeneratorCommand.class, ConfigCommand.class})
public class ReferenceValidator implements Callable<Integer> {
  static void main(String[] args) {
    int exitCode = new CommandLine(new ReferenceValidator()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    IO.print("Please specify a command. Use --help for more information.\n");
    return 0;
  }
}
