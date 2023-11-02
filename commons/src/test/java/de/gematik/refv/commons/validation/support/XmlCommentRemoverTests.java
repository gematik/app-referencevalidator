package de.gematik.refv.commons.validation.support;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class XmlCommentRemoverTests {

    @ParameterizedTest
    @ValueSource( strings = {
            "<root><!-- Comment abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZäöüÄÖÜß1234567890§$%&/}{[]()?!#'``+-:;,.-<>@€|²³{[]}µ^^^°~´´ --><element>Value</element><!-- Comment 2 --></root>",
            "<root><!-- Comment with\nline break --><element>Value</element></root>",
            "<root><element>Value</element></root>"
    })
    @Execution(ExecutionMode.CONCURRENT)
    void testRemoveXmlCommentsFrom(String inputXml) {
        String expectedOutput = "<root><element>Value</element></root>";
        String cleanedXml = XmlCommentRemover.removeXmlCommentsFrom(inputXml);
        assertEquals(expectedOutput, cleanedXml);
    }

    @Test
    void testRemoveXmlCommentsFrom_EmptyString() {
        String inputXml = "";
        String cleanedXml = XmlCommentRemover.removeXmlCommentsFrom(inputXml);
        assertEquals(inputXml, cleanedXml);
    }
}
