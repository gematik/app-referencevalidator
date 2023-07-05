package de.gematik.refv.valmodule.diga;

import de.gematik.refv.valmodule.base.helper.BaseProfileIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;


class KBV_MIO_DIGA_IT extends BaseProfileIntegrationTest {

    private static final String DIR = "KBV_MIO_DIGA";

    @BeforeAll
    void beforeAll() {
        super.createValidationModule("diga");
    }

    @TestFactory
    Stream<DynamicTest> testValidation() {
        return super.testValidationBase(DIR);
    }
}
