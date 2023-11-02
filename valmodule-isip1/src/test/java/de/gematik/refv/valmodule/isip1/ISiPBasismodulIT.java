package de.gematik.refv.valmodule.isip1;

import de.gematik.refv.valmodule.base.helper.BaseProfileIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;


import java.util.stream.Stream;


class ISiPBasismodulIT extends BaseProfileIntegrationTest {

    private static final String DIR = "ISiP-Basismodul";

    @BeforeAll
    void beforeAll() {
        super.createValidationModule("isip1");
    }

    @TestFactory
    Stream<DynamicTest> testValidation() {
        return super.testValidationBase(DIR, "json");
    }
}
