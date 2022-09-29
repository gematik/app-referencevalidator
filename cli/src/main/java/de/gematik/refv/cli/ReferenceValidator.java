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

package de.gematik.refv.cli;

import de.gematik.refv.SupportedValidationModule;
import de.gematik.refv.ValidationModuleFactory;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.commons.validation.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(
        name="",
        description = "Validates a file"
)
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceValidator implements Runnable {

    @CommandLine.Option(names = {"-m", "--module"}, required = true)
    private String module;

    @CommandLine.Option(names = {"-i", "--input"}, required = true)
    private Path filePath;

    static Logger logger = LoggerFactory.getLogger(ReferenceValidator.class);

    private ValidationModuleFactory factory;

    public static void main(String[] args) {
        var cli = new CommandLine(new ReferenceValidator(null, null, new ValidationModuleFactory())).setCaseInsensitiveEnumValuesAllowed(true);
        cli.execute(args);
    }

    public void run() {
        try {
            if(!SupportedValidationModule.ERP.toString().equalsIgnoreCase(module)) {
                logger.error("No module {} found", module);
                return;
            }

            ValidationModule validator = factory.createValidationModule(SupportedValidationModule.ERP);
            ValidationResult result = validator.validateFile(filePath);
            logger.info("Validation result: {}", result);
        } catch (Exception e){
            logger.error("Exception", e);
        }
    }
}
