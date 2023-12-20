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
import de.gematik.refv.commons.exceptions.ValidationModuleInitializationException;
import de.gematik.refv.Plugin;
import de.gematik.refv.commons.validation.ProfileValidityPeriodCheckStrategy;
import de.gematik.refv.commons.validation.ValidationMessagesFilter;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.commons.validation.ValidationOptions;
import de.gematik.refv.commons.validation.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Category;
import org.apache.log4j.LogManager;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(
        name="java -jar referencevalidator-cli.jar",
        description = "The validator checks conformance of FHIR resources to the underlying specification based on pre-compiled FHIR-/terminology packages and pre-defined validation configuration"
)
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ReferenceValidator implements Runnable {

    @CommandLine.Parameters(paramLabel = "VALIDATION_MODULE", description = "ID of a validation module or plugin")
    private String module;

    @CommandLine.Parameters(paramLabel = "FILE", description = "Input file", defaultValue = "")
    private File file;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Print debug log messages and INFORMATION/WARNING validation messages", required = false)
    private boolean isVerbose;

    @CommandLine.Option(names = {"-nvp", "--no-profile-validity-period-check"}, description = "Disable profile validity period check", required = false, defaultValue = "false")
    private boolean isNoInstanceValidityCheck;

    @CommandLine.Option(names = {"-p", "--profile"}, description = "Canonical url of a profile to validate against", required = false)
    private String profile;

    @CommandLine.Option(names = {"-mi", "--module-info"}, description = "Print profiles supported by a module", required = false)
    private boolean showModuleConfiguration;

    @CommandLine.Option(names = {"-ae", "--accepted-encodings"}, split = ",", description = "Encodings to accept (XML,JSON). Overwrites the module internal setting.", required = false)
    private List<String> acceptedEncodings;

    private ValidationModuleFactory factory = new ValidationModuleFactory();

    private PluginLoader pluginLoader = new PluginLoader();
    private static final String PLUGINS_DIR = "plugins";

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
        log.info("\r\n{}", output);
    }

    public void run() {
        try {
            if(isVerbose) {
                configureAllLoggersToDebug();
            }

            ValidationModule validator = getValidationModule();
            if (validator == null) {
                log.debug("No suitable validation module found");
                return;
            }

            if(showModuleConfiguration) {
                String moduleConfiguration = new ModuleConfigurationPrinter().moduleConfigurationToString(validator.getConfiguration());
                logWithLineBreak(moduleConfiguration);
                return;
            }

            ValidationOptions validationOptions = getValidationOptions();

            ValidationResult result = validator.validateFile(file.toPath(), validationOptions);

            String outputMessage = buildOutputMessage(result);
            logWithLineBreak(outputMessage);
        } catch (Exception e){
            log.error("Exception", e);
        }
    }

    private ValidationOptions getValidationOptions() {
        ValidationOptions validationOptions = ValidationOptions.getDefaults();
        if(profile != null)
            validationOptions.getProfiles().add(profile);
        if(acceptedEncodings != null && !acceptedEncodings.isEmpty())
            validationOptions.setAcceptedEncodings(acceptedEncodings);
        if(isNoInstanceValidityCheck)
            validationOptions.setProfileValidityPeriodCheckStrategy(ProfileValidityPeriodCheckStrategy.IGNORE);
        if(isVerbose)
            validationOptions.setValidationMessagesFilter(ValidationMessagesFilter.KEEP_ALL);
        return validationOptions;
    }

    // The selection mechanism below leaves plugins away, whose ids are the same as any id of the integrated validation modules, without any notice.
    // This is unfortunate and can be improved in the future. At the same time we avoid the overhead of plugin loading routines in case when user
    // requests an integrated validation module (the main use case)
    private ValidationModule getValidationModule() throws ValidationModuleInitializationException, IOException {
        ValidationModule validator;

        Optional<SupportedValidationModule> integratedValidationModule = SupportedValidationModule.fromString(module);
        if(integratedValidationModule.isPresent()) {
            validator = factory.createValidationModule(integratedValidationModule.get());
        }
        else {
            var pluginFolder = getPluginsDir();
            log.debug("Looking for plugins in " + pluginFolder + "...");
            if(new File(pluginFolder).exists()) {
                var plugins = pluginLoader.loadPlugins(pluginFolder);
                log.info("Successfully loaded plugins: {}", plugins.keySet());
                Plugin plugin = plugins.get(module);
                if (plugin == null) {
                    var supportedValidationModules = Stream.concat(plugins.keySet().stream(), Arrays.stream(SupportedValidationModule.values())).map(Object::toString).collect(Collectors.toList());
                    log.error("Module [{}] unsupported. Supported modules: {}", module, supportedValidationModules);
                    return null;
                }
                validator = factory.createValidationModuleFromPlugin(plugin);
            }
            else {
                log.error("Module [{}] unsupported. Supported modules: {}", module, SupportedValidationModule.values());
                return null;
            }
        }
        return validator;
    }

    @SneakyThrows
    private static String getPluginsDir() {
        String result;
        URL location = ReferenceValidator.class.getProtectionDomain().getCodeSource().getLocation();
        var isJarFile = ReferenceValidator.class.getResource(ReferenceValidator.class.getSimpleName() + ".class").toString().startsWith("jar:");
        if(isJarFile) {
            result = new File(location.toURI()).getParentFile().getAbsolutePath() + File.separator;
        }
        else {
            result = location.getPath();
        }
        result += PLUGINS_DIR;
        return result;
    }

    private static void showInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("\r\ngematik Referencevalidator " + ReferenceValidator.class.getPackage().getImplementationVersion());
        sb.append("\r\nJava:   " + System.getProperty("java.version")
                + " from " + System.getProperty("java.home")
                + " on " + System.getProperty("os.arch")
                + " (" + System.getProperty("sun.arch.data.model") + "bit). "
                + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "MB available");
        sb.append("\r\nLocale: " + Locale.getDefault() + "\r\n");
        log.info("{}",sb);
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
            if(isVerbose){
                sb.append("See ")
                        .append(getCountFor(ResultSeverityEnum.ERROR, results) + getCountFor(ResultSeverityEnum.FATAL, results))
                        .append(" errors, ")
                        .append(getCountFor(ResultSeverityEnum.WARNING, results))
                        .append(" warnings and ")
                        .append(getCountFor(ResultSeverityEnum.INFORMATION, results))
                        .append(" other notes below.\n\n");
            }
            else {
                sb.append("See ")
                        .append((getCountFor(ResultSeverityEnum.ERROR, results) + getCountFor(ResultSeverityEnum.FATAL, results)))
                        .append(" errors below.\n\n");
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

        return isVerbose;
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
