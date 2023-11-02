package de.gematik.refv.valmodule.isik2;

import de.gematik.refv.valmodule.base.helper.BaseProfileIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;


class ISiKVitalparameterIT extends BaseProfileIntegrationTest {
    private static final String DIR = "ISiK-Vitalparameter";

    @BeforeAll
    void beforeAll() {
        super.createValidationModule("isik2");
    }

    @TestFactory
    Stream<DynamicTest> testValidation() {
        return super.testValidationBase(DIR, "json");
    }
}
