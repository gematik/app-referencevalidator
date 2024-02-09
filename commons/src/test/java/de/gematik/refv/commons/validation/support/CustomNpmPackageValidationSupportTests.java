package de.gematik.refv.commons.validation.support;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class CustomNpmPackageValidationSupportTests {

    @Test
    void testLoadPackageFromPath() throws IOException {
        String packagePath = "simple/package/fhir-package-1.0.0.tgz";
        Function<String, InputStream> packageInputStreamProvider = path -> {
            try {
                return new ByteArrayInputStream(Objects.requireNonNull(CustomNpmPackageValidationSupportTests.class.getClassLoader().getResourceAsStream(path)).readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        CustomNpmPackageValidationSupport validationSupport = CustomNpmPackageValidationSupport.create(FhirContext.forR4(), packagePath, packageInputStreamProvider);

        assertThat(validationSupport.fetchStructureDefinition("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate")).isNotNull();
        assertThat(validationSupport.fetchStructureDefinition("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate|1.0.0")).isNotNull();
        assertThat(validationSupport.fetchCodeSystem("https://test.gematik.de/fhir/isik/v3/CodeSystem/test-category-codes")).isNotNull();
        assertThat(validationSupport.fetchCodeSystem("https://test.gematik.de/fhir/isik/v3/CodeSystem/test-category-codes|1.0.0")).isNotNull();
        assertThat(validationSupport.isCodeSystemSupported(null,"https://test.gematik.de/fhir/isik/v3/CodeSystem/test-category-codes")).isTrue();
        assertThat(validationSupport.isCodeSystemSupported(null,"https://test.gematik.de/fhir/isik/v3/CodeSystem/test-category-codes|1.0.0")).isTrue();
        assertThat(validationSupport.fetchValueSet("http://example.org/fhir/ValueSet/TestValueSet")).isNotNull();
        assertThat(validationSupport.fetchValueSet("http://example.org/fhir/ValueSet/TestValueSet|1.0.0")).isNotNull();
        assertThat(validationSupport.isValueSetSupported(null,"http://example.org/fhir/ValueSet/TestValueSet")).isTrue();
        assertThat(validationSupport.isValueSetSupported(null,"http://example.org/fhir/ValueSet/TestValueSet|1.0.0")).isTrue();

        assertThat(validationSupport.fetchStructureDefinition("http://unknown-structure-definition")).isNull();
        assertThat(validationSupport.fetchStructureDefinition("http://example.gematik.de/fhir/StructureDefinition/patient-with-birthdate|2.0.0")).isNull();
        assertThat(validationSupport.fetchCodeSystem("http://unknown-code-system")).isNull();
        assertThat(validationSupport.fetchCodeSystem("https://test.gematik.de/fhir/isik/v3/CodeSystem/test-category-codes|2.0.0")).isNull();
        assertThat(validationSupport.isCodeSystemSupported(null,"http://unknown-code-system")).isFalse();
        assertThat(validationSupport.fetchValueSet("http://unknown-value-set")).isNull();
        assertThat(validationSupport.fetchValueSet("http://example.org/fhir/ValueSet/TestValueSet|2.0.0")).isNull();
        assertThat(validationSupport.isValueSetSupported(null,"http://unknown-value-set")).isFalse();
    }
}
