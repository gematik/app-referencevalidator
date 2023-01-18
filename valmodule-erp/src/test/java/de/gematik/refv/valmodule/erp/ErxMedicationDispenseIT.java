package de.gematik.refv.valmodule.erp;

import de.gematik.refv.valmodule.erp.helper.BaseProfileIntegrationTest;
import de.gematik.refv.valmodule.erp.helper.TestErpValidationModuleFactory;
import de.gematik.refv.valmodule.erp.helper.ValidFolderDetector;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
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
class ErxMedicationDispenseIT extends BaseProfileIntegrationTest {
    private static final String DIR = "ErxMedicationDispense";
    @TestFactory
    Stream<DynamicTest> testValidation() {
        return super.testValidationBase(DIR);
    }
}
