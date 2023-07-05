package de.gematik.refv.valmodule.erp;

import de.gematik.refv.valmodule.base.helper.BaseProfileIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInstance;
import java.util.stream.Stream;


class OtherProfilesIT extends BaseProfileIntegrationTest {
    private static final String DIR = "OtherProfiles";

    @BeforeAll
    void beforeAll() {
        super.createValidationModule("erp");
    }

    @TestFactory
    Stream<DynamicTest> testValidation() {
        return super.testValidationBase(DIR);
    }
}
