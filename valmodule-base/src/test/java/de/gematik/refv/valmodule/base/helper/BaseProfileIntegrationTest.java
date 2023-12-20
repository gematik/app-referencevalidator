package de.gematik.refv.valmodule.base.helper;

import de.gematik.refv.commons.validation.IntegratedValidationModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;


@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseProfileIntegrationTest {

    protected IntegratedValidationModule validationModule;

    protected void createValidationModule(String module) {
        this.validationModule = TestConfigurationBasedValidationModuleFactory.createInstance(module);
    }

    @SneakyThrows
    protected void validateFile(Path path) {
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
