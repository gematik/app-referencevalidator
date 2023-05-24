package de.gematik.refv.valmodule.diga;

import de.gematik.refv.valmodule.base.helper.BaseProfileIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;

import java.util.stream.Stream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KBV_MIO_DIGA_IT extends BaseProfileIntegrationTest {

    private static final String DIR = "KBV_MIO_DIGA";

    @BeforeAll
    void beforeAll() {
        super.createValidationModuleNonCachingInstance("diga");
    }

    @TestFactory
    Stream<DynamicTest> testValidation() {
        return super.testValidationBase(DIR);
    }
}
