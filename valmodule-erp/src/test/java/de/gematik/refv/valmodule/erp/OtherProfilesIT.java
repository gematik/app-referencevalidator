package de.gematik.refv.valmodule.erp;

import de.gematik.refv.valmodule.erp.helper.BaseProfileIntegrationTest;
import de.gematik.refv.valmodule.erp.helper.TestErpValidationModuleFactory;
import de.gematik.refv.valmodule.erp.helper.ValidFolderDetector;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OtherProfilesIT extends BaseProfileIntegrationTest {
    private static final String DIR = "OtherProfiles";
    private ErpValidationModule validationModule;

    @BeforeAll
    void beforeAll() {
        this.validationModule = TestErpValidationModuleFactory.createNonCachingInstance();
    }

    @SneakyThrows
    private void validateFile(Path path) {
        var result = validationModule.validateFile(path);
        boolean isValid = ValidFolderDetector.isInValidFolder(path);
        Assertions.assertEquals(isValid, result.isValid(),result.toString());
    }

    @SneakyThrows
    @TestFactory
    Stream<DynamicTest> testValidation() {
        return testValidationBase(DIR, "xml");
    }

    @SneakyThrows
    protected Stream<DynamicTest> testValidationBase(String folder, String fileExtension) {
        return Files.walk(Paths.get(String.format("src/test/resources/%s",folder))).filter(path -> path.toString().endsWith(String.format(".%s",fileExtension))).map(
                f -> DynamicTest.dynamicTest(f.toString(),
                        () -> validateFile(f)));
    }
}
