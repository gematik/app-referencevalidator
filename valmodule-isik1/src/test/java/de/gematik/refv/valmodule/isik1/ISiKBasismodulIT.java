package de.gematik.refv.valmodule.isik1;

import de.gematik.refv.valmodule.base.helper.BaseProfileIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;


class ISiKBasismodulIT extends BaseProfileIntegrationTest {
    private static final String DIR = "ISiK-Basismodul";

    @BeforeAll
    void beforeAll() {
        super.createValidationModule("isik1");
    }

    @TestFactory
    Stream<DynamicTest> testValidation() {
        return super.testValidationBase(DIR, "json");
    }
}
