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
package de.gematik.refv.lib.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExceptionsTest {

  @DisplayName(
      "Given a ContextFailure, when building the complete message, then code and message are joined")
  @Test
  void expectContextFailureCompleteMessage() {
    final var failure =
        new ContextFailure("ERR-1", "something failed", ContextFailure.FailureCategory.INTERNAL);
    Assertions.assertEquals("ERR-1: something failed", failure.completeErrorMessage());
    Assertions.assertEquals(ContextFailure.FailureCategory.INTERNAL, failure.category());
  }

  @DisplayName("Given a ConfigurationException, when created, then the category is CONFIGURATION")
  @Test
  void expectConfigurationExceptionCategory() {
    final var ex = new ConfigurationException("bad config");
    Assertions.assertEquals(ContextFailure.FailureCategory.CONFIGURATION, ex.getCategory());
    Assertions.assertEquals("bad config", ex.getMessage());
    final var withCause = new ConfigurationException("bad config", new RuntimeException("cause"));
    Assertions.assertEquals(ContextFailure.FailureCategory.CONFIGURATION, withCause.getCategory());
    Assertions.assertNotNull(withCause.getCause());
  }

  @DisplayName(
      "Given package-loading exceptions, when created, then the category is PACKAGE_LOADING")
  @Test
  void expectPackageLoadingCategory() {
    Assertions.assertEquals(
        ContextFailure.FailureCategory.PACKAGE_LOADING,
        new CyclicDependencyException("c").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.PACKAGE_LOADING,
        new DependencyLoadFailedException("d").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.PACKAGE_LOADING,
        new PackageDownloadException("p").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.PACKAGE_LOADING,
        new PackageLoadFailedException("p").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.PACKAGE_LOADING,
        new CyclicDependencyException("c", new RuntimeException()).getCategory());
  }

  @DisplayName(
      "Given an InitializationException, when created, then the category is INITIALIZATION")
  @Test
  void expectInitializationCategory() {
    Assertions.assertEquals(
        ContextFailure.FailureCategory.INITIALIZATION,
        new InitializationException("init").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.INITIALIZATION,
        new InitializationException("init", new RuntimeException()).getCategory());
  }

  @DisplayName("Given validation exceptions, when created, then the category is VALIDATION")
  @Test
  void expectValidationCategory() {
    Assertions.assertEquals(
        ContextFailure.FailureCategory.VALIDATION,
        new MissingProfileDefinitionException("v").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.VALIDATION, new ProfileMismatchException("v").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.VALIDATION,
        new ProfileOutsideValidityPeriodException("v").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.VALIDATION,
        new UnsupportedProfileException("v").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.VALIDATION, new ValidationException("v").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.VALIDATION,
        new ValidationException("v", new RuntimeException()).getCategory());
  }

  @DisplayName("Given snapshot exceptions, when created, then the category is SNAPSHOT")
  @Test
  void expectSnapshotCategory() {
    Assertions.assertEquals(
        ContextFailure.FailureCategory.SNAPSHOT,
        new SnapshotGenerationException("s").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.SNAPSHOT, new ProfilePatchException("v").getCategory());
  }

  @DisplayName("Given a ParsingException, when created, then the category is PARSING")
  @Test
  void expectParsingCategory() {
    Assertions.assertEquals(
        ContextFailure.FailureCategory.PARSING, new ParsingException("p").getCategory());
    Assertions.assertEquals(
        ContextFailure.FailureCategory.PARSING, new InvalidDateFormatException("v").getCategory());
  }

  @DisplayName("Given a TerminologyServerException, when created, then the category is NETWORK")
  @Test
  void expectTerminologyServerCategory() {
    final var ex = new TerminologyServerException("tx down", new RuntimeException("io"));
    Assertions.assertEquals(ContextFailure.FailureCategory.NETWORK, ex.getCategory());
    Assertions.assertEquals("tx down", ex.getMessage());
  }

  @DisplayName("Given module exceptions, when created, then they carry the message and cause")
  @Test
  void expectModuleExceptionsCarryData() {
    final var invalidConfig =
        new InvalidModuleConfigurationException("invalid", new IllegalStateException("x"));
    Assertions.assertEquals("invalid", invalidConfig.getMessage());
    Assertions.assertNotNull(invalidConfig.getCause());
    Assertions.assertEquals(
        "invalid2", new InvalidModuleConfigurationException("invalid2").getMessage());

    final var loadModule = new LoadModuleException("load", new RuntimeException("y"));
    Assertions.assertEquals("load", loadModule.getMessage());
    Assertions.assertEquals("load2", new LoadModuleException("load2").getMessage());
  }

  @DisplayName("Given a FhirContextException, when created, then it is a RuntimeException")
  @Test
  void expectFhirContextExceptionIsRuntime() {
    Assertions.assertInstanceOf(RuntimeException.class, new ValidationException("v"));
  }
}
