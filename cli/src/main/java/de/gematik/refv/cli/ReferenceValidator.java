/*
 * Copyright (c) 2023 gematik GmbH
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

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.gematik.refv.SupportedValidationModule;
import de.gematik.refv.ValidationModuleFactory;
import de.gematik.refv.commons.validation.ProfileValidityPeriodCheckStrategy;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.commons.validation.ValidationOptions;
import de.gematik.refv.commons.validation.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.log4j.Category;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

@CommandLine.Command(
        name="java -jar referencevalidator-cli.jar",
        description = "The validator checks conformance of FHIR resources to the underlying specification based on pre-compiled FHIR-/terminology packages and pre-defined validation configuration"
)
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceValidator implements Runnable {

    @CommandLine.Parameters(paramLabel = "VALIDATION_MODULE", description = "Choice of a validation module (erp, eau, isik1, isik2, isip1, diga)")
    private String module;

    @CommandLine.Parameters(paramLabel = "FILE", description = "Input file", defaultValue = "")
    private File file;

    @CommandLine.Option(names = {"-e", "--errors-only"}, description = "Print only errors in the validation results", required = false)
    private boolean isOnlyErrorsInOutput;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Print debug log messages", required = false)
    private boolean isVerbose;

    @CommandLine.Option(names = {"-nvp", "--no-profile-validity-period-check"}, description = "Disable profile validity period check", required = false, defaultValue = "false")
    private boolean isNoInstanceValidityCheck;

    @CommandLine.Option(names = {"-p", "--profile"}, description = "Canonical url of a profile to validate against", required = false)
    private String profile;

    @CommandLine.Option(names = {"-mi", "--module-info"}, description = "Print profiles supported by a module", required = false)
    private boolean showModuleConfiguration;

    @CommandLine.Option(names = {"-ae", "--accepted-encodings"}, description = "Encodings to accept (XML,JSON). Overwrites the module internal setting.", required = false)
    private List<String> acceptedEncodings;
    static Logger logger = LoggerFactory.getLogger(ReferenceValidator.class);

    private ValidationModuleFactory factory = new ValidationModuleFactory();

    @SneakyThrows
    public static void main(String[] args) {
        showInfo();
        var cli = new CommandLine(new ReferenceValidator()).setCaseInsensitiveEnumValuesAllowed(true);
        if(args.length == 0) {
            try(ByteArrayOutputStream out1 = new ByteArrayOutputStream()) {
                PrintWriter out = new PrintWriter(out1);
                cli.usage(out);
                logWithLineBreak(out1.toString());
            }
        }
        else
            cli.execute(args);
    }

    private static void logWithLineBreak(String output) {
        logger.info("\r\n{}", output);
    }

    public void run() {
        try {
            if(isVerbose) {
                configureAllLoggersToDebug();
            }
            Optional<SupportedValidationModule> supportedValidationModule = SupportedValidationModule.fromString(module);
            if(supportedValidationModule.isEmpty()) {
                logger.error("Module [{}] unsupported. Supported modules: {}", module, SupportedValidationModule.values());
                return;
            }

            ValidationModule validator = factory.createValidationModule(supportedValidationModule.get());

            if(showModuleConfiguration) {
                String moduleConfiguration = new ModuleConfigurationPrinter().moduleConfigurationToString(validator.getConfiguration());
                logWithLineBreak(moduleConfiguration);
                return;
            }
            ValidationOptions validationOptions = ValidationOptions.getDefaults();
            if(profile != null)
                validationOptions.getProfiles().add(profile);
            if(acceptedEncodings != null && !acceptedEncodings.isEmpty())
                validationOptions.setAcceptedEncodings(acceptedEncodings);
            if(isNoInstanceValidityCheck)
                validationOptions.setProfileValidityPeriodCheckStrategy(ProfileValidityPeriodCheckStrategy.IGNORE);

            ValidationResult result = validator.validateFile(file.toPath(), validationOptions);

            String outputMessage = buildOutputMessage(result);
            logWithLineBreak(outputMessage);
        } catch (Exception e){
            logger.error("Exception", e);
        }
    }

    private static void showInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("\r\ngematik Referencevalidator " + ReferenceValidator.class.getPackage().getImplementationVersion());
        sb.append("\r\nJava:   " + System.getProperty("java.version")
                + " from " + System.getProperty("java.home")
                + " on " + System.getProperty("os.arch")
                + " (" + System.getProperty("sun.arch.data.model") + "bit). "
                + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "MB available\r\n");
        logger.info("{}",sb);
    }



    private static void configureAllLoggersToDebug() {
        org.apache.log4j.Logger logger4j = org.apache.log4j.Logger.getRootLogger();
        logger4j.setLevel(org.apache.log4j.Level.toLevel("DEBUG"));
        Enumeration<Category> loggers = LogManager.getCurrentLoggers();
        while (loggers.hasMoreElements()) {
            Category logger = loggers.nextElement();
            logger4j = org.apache.log4j.Logger.getLogger(logger.getName());
            logger4j.setLevel(org.apache.log4j.Level.toLevel("DEBUG"));
        }
    }

    protected String buildOutputMessage(ValidationResult results)
    {
        StringBuilder sb = new StringBuilder("\n");

        sb.append( "=====  Valid: " );
        if( results.isValid() )
            sb.append( "true =====\n\n" );
        else
            sb.append( "false =====\n\n" );

        if(isDetailsShouldBePrinted(results)
        ) {
            if(isOnlyErrorsInOutput){
                sb.append("See ")
                        .append((getCountFor(ResultSeverityEnum.ERROR, results) + getCountFor(ResultSeverityEnum.FATAL, results)))
                        .append(" errors below.\n\n");
            } else {
                sb.append("See ")
                        .append(getCountFor(ResultSeverityEnum.ERROR, results) + getCountFor(ResultSeverityEnum.FATAL, results))
                        .append(" errors, ")
                        .append(getCountFor(ResultSeverityEnum.WARNING, results))
                        .append(" warnings and ")
                        .append(getCountFor(ResultSeverityEnum.INFORMATION, results))
                        .append(" other notes below.\n\n");
            }

            sb.append("  ")
                    .append(padRight("    ", 4))
                    .append(padRight("Severity", 13))
                    .append(padRight("Code", 45))
                    .append("Location (FHIRPath)    ")
                    .append("Profile, Element and Problem description\n")
                    .append("  ")
                    .append(padRight("    ", 4))
                    .append(padRight("--------  ", 13))
                    .append(padRight("----------------------------------------", 45))
                    .append("-------------------    ")
                    .append("----------------------------------------\n");

            int count = 1;

            for (SingleValidationMessage message : results.getValidationMessages()) {
                if (isOnlyErrorsInOutput && !isErrorMessage(message)) {
                    continue;
                }
                sb.append("  ")
                        .append(padRight("" + count++, 4))
                        .append(padRight(message.getSeverity().toString(), 13))
                        .append(padRight(message.getMessageId(), 45))
                        .append(message.getLocationString()).append("   ")
                        .append(message.getMessage()).append('\n');
            }
        }


        return sb.toString();
    }

    private int getCountFor(ResultSeverityEnum severity, ValidationResult results) {
        int count = 0;
        for(SingleValidationMessage message : results.getValidationMessages()){
            if(message.getSeverity() == severity)
                count++;
        }
        return count;
    }

    private boolean isDetailsShouldBePrinted(ValidationResult results) {
        if(results.getValidationMessages().isEmpty())
            return false;

        if(isAtLeastOneErrorInMessages(results))
            return true;

        return !isOnlyErrorsInOutput;
    }

    private static boolean isAtLeastOneErrorInMessages(ValidationResult results) {
        return results.getValidationMessages().stream().anyMatch(ReferenceValidator::isErrorMessage);
    }

    private static boolean isErrorMessage(SingleValidationMessage m) {
        return m.getSeverity() == ResultSeverityEnum.ERROR || m.getSeverity() == ResultSeverityEnum.FATAL;
    }

    public static String padRight(String s, int n) {
        String format = "%-" + n + "s";
        return String.format(format, s);
    }
}
