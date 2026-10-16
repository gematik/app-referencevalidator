/*-
 * #%L
 * Reference Validator Library
 * %%
 * Copyright (C) 2024 - 2026 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.lib.config_parser.boundary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MapperProviderTest {

  static final LocalDate DATE = LocalDate.of(2026, 10, 15);
  static final DateTimeConfiguration DATE_TIME_CONFIGURATION =
      new DateTimeConfiguration(
          DATE, LocalDateTime.of(2026, 10, 15, 12, 30), Instant.parse("2026-10-15T12:30:00Z"));
  static final String JSON_DATES =
      """
      {
        "date": "2026-10-15",
        "dateTime": "2026-10-15T12:30:00",
        "instant": "2026-10-15T12:30:00Z"
      }
      """;
  static final String YAML_DATES =
      """
      date: "2026-10-15"
      dateTime: "2026-10-15T12:30:00"
      instant: "2026-10-15T12:30:00Z"
      """;
  static final String JSON_UNKNOWN_PROPERTY =
      """
      {"known": "value", "unexpected": true}
      """;
  static final String YAML_UNKNOWN_PROPERTY =
      """
      known: value
      unexpected: true
      """;

  @ParameterizedTest(name = "{0}")
  @MethodSource("jsonRequirements")
  void jsonMapperRequirements(String requirement, MapperScenario scenario) throws Exception {
    scenario.verify(JSONMapperProvider::getMapper);
  }

  static Stream<Arguments> jsonRequirements() {
    return Stream.of(
        requirement("R2.1", MapperProviderTest::sharedMapperIsNonNull),
        requirement("R2.2", provider -> readsDateTimeValues(provider, JSON_DATES)),
        requirement("R2.3", MapperProviderTest::writesCalendarDatesAsIsoStrings),
        requirement("R2.4", provider -> rejectsUnknownProperties(provider, JSON_UNKNOWN_PROPERTY)),
        requirement("R2.5", MapperProviderTest::writesIndentedConfiguration));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("yamlRequirements")
  void yamlMapperRequirements(String requirement, MapperScenario scenario) throws Exception {
    scenario.verify(YAMLMapperProvider::getMapper);
  }

  static Stream<Arguments> yamlRequirements() {
    return Stream.of(
        requirement("R3.1", MapperProviderTest::sharedMapperIsNonNull),
        requirement("R3.2", provider -> readsDateTimeValues(provider, YAML_DATES)),
        requirement("R3.3", MapperProviderTest::writesCalendarDatesAsIsoStrings),
        requirement("R3.4", provider -> rejectsUnknownProperties(provider, YAML_UNKNOWN_PROPERTY)),
        requirement("R3.5", MapperProviderTest::writesIndentedConfiguration),
        requirement("R3.6", MapperProviderTest::yamlMappingFailuresIdentifyFieldAndReason),
        requirement("R3.7", MapperProviderTest::yamlSyntaxFailuresIncludeReasonAndLocation));
  }

  static Arguments requirement(String id, MapperScenario scenario) {
    return Arguments.of(id, scenario);
  }

  static void sharedMapperIsNonNull(Supplier<? extends ObjectMapper> provider) {
    var mapper = provider.get();
    Assertions.assertNotNull(mapper);
    Assertions.assertSame(mapper, provider.get());
  }

  static void readsDateTimeValues(Supplier<? extends ObjectMapper> provider, String input)
      throws Exception {
    var configuration = provider.get().readValue(input, DateTimeConfiguration.class);
    Assertions.assertEquals(DATE_TIME_CONFIGURATION, configuration);
  }

  static void writesCalendarDatesAsIsoStrings(Supplier<? extends ObjectMapper> provider)
      throws Exception {
    var mapper = provider.get();
    var output = mapper.writeValueAsString(new DateConfiguration(DATE));
    var date = mapper.readTree(output).get("date");
    Assertions.assertTrue(date.isTextual(), output);
    Assertions.assertEquals("2026-10-15", date.textValue());
    Assertions.assertEquals(
        new DateConfiguration(DATE), mapper.readValue(output, DateConfiguration.class));
  }

  static void rejectsUnknownProperties(Supplier<? extends ObjectMapper> provider, String input)
      throws Exception {
    var mapper = provider.get();
    var knownConfiguration = new KnownConfiguration("value");
    var validInput = mapper.writeValueAsString(knownConfiguration);
    Assertions.assertEquals(
        knownConfiguration, mapper.readValue(validInput, KnownConfiguration.class));
    var exception =
        Assertions.assertThrows(
            UnrecognizedPropertyException.class,
            () -> mapper.readValue(input, KnownConfiguration.class));
    Assertions.assertEquals("unexpected", exception.getPropertyName());
    Assertions.assertEquals(
        new PermissiveConfiguration("value"),
        mapper.readValue(input, PermissiveConfiguration.class));
  }

  static void writesIndentedConfiguration(Supplier<? extends ObjectMapper> provider)
      throws Exception {
    var mapper = provider.get();
    var configuration = new NestedConfiguration(new KnownConfiguration("value"));
    var output = mapper.writeValueAsString(configuration);
    Assertions.assertTrue(output.lines().count() > 1, output);
    Assertions.assertTrue(output.lines().anyMatch(MapperProviderTest::isIndentedContent), output);
    Assertions.assertEquals(configuration, mapper.readValue(output, NestedConfiguration.class));
  }

  static boolean isIndentedContent(String line) {
    return !line.isBlank() && line.length() > line.stripLeading().length();
  }

  static void yamlMappingFailuresIdentifyFieldAndReason(Supplier<? extends ObjectMapper> provider) {
    var mapper = provider.get();
    var unknownProperty =
        Assertions.assertThrows(
            UnrecognizedPropertyException.class,
            () -> mapper.readValue(YAML_UNKNOWN_PROPERTY, KnownConfiguration.class));
    Assertions.assertTrue(unknownProperty.getOriginalMessage().contains("unexpected"));
    Assertions.assertTrue(unknownProperty.getOriginalMessage().contains("Unrecognized field"));
    var incompatibleValue =
        Assertions.assertThrows(
            MismatchedInputException.class,
            () -> mapper.readValue("enabled: [true, false]", BooleanConfiguration.class));
    Assertions.assertTrue(incompatibleValue.getMessage().contains("enabled"));
    Assertions.assertTrue(incompatibleValue.getOriginalMessage().contains("boolean"));
  }

  static void yamlSyntaxFailuresIncludeReasonAndLocation(
      Supplier<? extends ObjectMapper> provider) {
    var mapper = provider.get();
    var failure =
        Assertions.assertThrows(
            JsonParseException.class,
            () -> mapper.readValue("known: \"unterminated", KnownConfiguration.class));
    Assertions.assertFalse(failure.getOriginalMessage().isBlank());
    Assertions.assertNotNull(failure.getLocation());
    Assertions.assertTrue(failure.getLocation().getLineNr() > 0);
    Assertions.assertTrue(failure.getLocation().getColumnNr() > 0);
  }

  @FunctionalInterface
  interface MapperScenario {
    void verify(Supplier<? extends ObjectMapper> provider) throws Exception;
  }

  record DateTimeConfiguration(LocalDate date, LocalDateTime dateTime, Instant instant) {}

  record DateConfiguration(LocalDate date) {}

  record KnownConfiguration(String known) {}

  record BooleanConfiguration(boolean enabled) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record PermissiveConfiguration(String known) {}

  record NestedConfiguration(KnownConfiguration settings) {}
}
