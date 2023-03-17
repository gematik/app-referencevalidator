package de.gematik.refv.valmodule.eau;

import de.gematik.refv.valmodule.base.ConfigurationBasedValidationModule;
import de.gematik.refv.valmodule.base.helper.TestConfigurationBasedValidationModuleFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

class EauModuleLocalTest {

    private final ConfigurationBasedValidationModule validationModule = TestConfigurationBasedValidationModuleFactory.createInstance("eau");

    @Test
    @SneakyThrows
    void testManual() {
        Path path = Paths.get("C:\\Dev\\gematik-referenzvalidator\\valmodule-eau\\src\\test\\resources\\KBV_PR_EAU_Bundle\\1.1.0\\valid\\KBV_PR_EAU_Bundle-test1.xml");
        var result = validationModule.validateFile(path);
        System.out.println(result);
        Assertions.assertTrue(result.isValid());
    }
}
