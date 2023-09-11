package de.gematik.refv.valmodule.core;

import de.gematik.refv.commons.validation.ValidationOptions;
import de.gematik.refv.valmodule.base.helper.BaseProfileIntegrationTest;
import de.gematik.refv.valmodule.base.helper.ValidFolderDetector;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

class FHIR_CORE_IT extends BaseProfileIntegrationTest {

    private static final String DIR = "FHIR_CORE";

    @BeforeAll
    void beforeAll() {
        super.createValidationModule("core");
    }

    @SneakyThrows
    protected void validateFile(Path path) {
        ValidationOptions validationOptions = ValidationOptions.getDefaults();

        Pattern pattern = Pattern.compile("(\\w+)[/\\\\](in)?valid");
        Matcher matcher = pattern.matcher(path.toString());

        if(!matcher.find())
            throw new IllegalStateException("Could not determine resource type from the file path " + path);

        var resourceType = matcher.group(1);
        String profile = "http://hl7.org/fhir/StructureDefinition/" + resourceType;
        validationOptions.setProfiles(List.of(profile));
        var result = validationModule.validateFile(path, validationOptions);
        boolean isValid = ValidFolderDetector.isInValidFolder(path);
        Assertions.assertEquals(isValid, result.isValid(), result.toString());
    }

    @TestFactory
    Stream<DynamicTest> testValidation() {
        return super.testValidationBase(DIR);
    }
}
