package de.gematik.refv.valmodule.base.helper;

import de.gematik.refv.valmodule.base.ConfigurationBasedValidationModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public abstract class BaseProfileIntegrationTest {

    protected ConfigurationBasedValidationModule validationModule;

    protected void createValidationModule(String module) {
        this.validationModule = TestConfigurationBasedValidationModuleFactory.createInstance(module);
    }

    protected void createValidationModuleNonCachingInstance(String module) {
        this.validationModule = TestConfigurationBasedValidationModuleFactory.createNonCachingInstance(module);
    }

    @SneakyThrows
    private void validateFile(Path path) {
        var result = validationModule.validateFile(path);
        boolean isValid = ValidFolderDetector.isInValidFolder(path);
        Assertions.assertEquals(isValid, result.isValid(),result.toString());
    }

    @SneakyThrows
    protected Stream<DynamicTest> testValidationBase(String folder) {
        return testValidationBase(folder, "xml");
    }

    @SneakyThrows
    protected Stream<DynamicTest> testValidationBase(String folder, String fileExtension) {
        return Files.walk(Paths.get(String.format("src/test/resources/%s",folder))).filter(path -> path.toString().endsWith(String.format(".%s",fileExtension))).map(
                f -> DynamicTest.dynamicTest(f.toString(),
                        () -> validateFile(f)));
    }
}
