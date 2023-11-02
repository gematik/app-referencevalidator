package de.gematik.refv.valmodule.isik3;

import de.gematik.refv.valmodule.base.helper.BaseProfileIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

class ISiKDokumentenaustauschIT extends BaseProfileIntegrationTest {
    private static final String DIR = "ISiK-Dokumentenaustausch";

    @BeforeAll
    void beforeAll() {
        super.createValidationModule("isik3");
    }

    @TestFactory
    Stream<DynamicTest> testValidation() {
        return super.testValidationBase(DIR, "json");
    }
}
