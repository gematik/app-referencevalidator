# Reference Validator Library

This guide shows common ways to use the Reference Validator Library: validate resources with FHIR Core or additional implementation-guide packages, load a validation module, configure validation behavior, and generate package snapshots.

The library API is in the `de.gematik.refv.lib` packages. Validation modules are distributed separately from the core library; obtain compatible module JARs from the [plugin repository](https://github.com/gematik/app-referencevalidator-plugins).

## Prerequisites and dependency

Use JDK 25 or later. Add the library to a Maven project:

```xml
<dependency>
  <groupId>de.gematik.refv</groupId>
  <artifactId>referencevalidator-lib</artifactId>
  <version>${version.referencevalidator}</version>
</dependency>
```

Replace `${version.referencevalidator}` with the version used by your application. Keep the transitive dependency versions supplied by the library, especially HAPI FHIR dependencies; overriding them can change validation behavior.

The following imports are used in the validation examples:

```java
import de.gematik.refv.lib.fhir_context.entity.ContextConfiguration;
import de.gematik.refv.lib.fhir_context.entity.FhirRelease;
import de.gematik.refv.lib.fhir_context.entity.IssueSeverity;
import de.gematik.refv.lib.fhir_context.entity.PackageDownloadConfiguration;
import de.gematik.refv.lib.validation.boundary.Validator;
import de.gematik.refv.lib.validation.boundary.ValidatorFactory;
import de.gematik.refv.lib.validation.entity.FhirResource;
import de.gematik.refv.lib.validation.entity.ProfileCanonical;
import de.gematik.refv.lib.validation.entity.ValidationOptions;
import de.gematik.refv.lib.validation.entity.ValidationRequest;
import de.gematik.refv.lib.validation.entity.ValidationResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
```

## Validate a resource against FHIR Core

Create a validator with the core definitions, wrap a JSON or XML resource in a `ValidationRequest`, and supply validation options. In this example, validation is forced against the R4 `Observation` profile.

```java
var configuration = ContextConfiguration.defaultConfiguration(FhirRelease.asR4());
try (Validator validator = ValidatorFactory.withCoreDefinitions(configuration)) {
  FhirResource resource = FhirResource.fromJson(Path.of("/path/to/observation.json"));
  var request = new ValidationRequest(resource);
  var options =
      new ValidationOptions(
          ProfileCanonical.fromCanonical(
              "http://hl7.org/fhir/StructureDefinition/Observation|4.0.1"),
          null,
          ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
          ValidationOptions.ProfileValidityPeriodCheckStrategy.IGNORE);

  ValidationResult result = validator.validate(request, options);
  boolean valid =
      result.messages().stream()
          .noneMatch(
              message ->
                  message.severity() == IssueSeverity.ERROR
                      || message.severity() == IssueSeverity.FATAL);

  System.out.println("Valid: " + valid);
  result.messages().forEach(System.out::println);
}
```

Use `ContextConfiguration.defaultConfiguration()` for the default R4 configuration. The default package-download policy is offline. Select a different FHIR release with `FhirRelease.asR4()` or `FhirRelease.asR5()`.

## Supply custom FHIR packages

Use `ValidatorFactory.withCustomPackages` when validation requires implementation-guide packages. Package coordinates use the form `name#version`. Include the FHIR release packages as well as the required implementation-guide packages. Package resolution can use the local cache; enable remote downloads if missing packages must be fetched.

```java
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.TerminologyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;

var configuration =
    new ContextConfiguration(
        FhirRelease.asR4(),
        "en-GB",
        DisplayBehaviorConfiguration.defaultConfiguration(),
        new PackageDownloadConfiguration(
            PackageDownloadConfiguration.RemoteDownloadPolicy.ALLOWED),
        TerminologyConfiguration.defaultConfiguration(),
        ValidationPolicyConfiguration.defaultConfiguration());

var packagesToLoad = new ArrayList<>(configuration.fhirRelease().packages());
packagesToLoad.add("replace.with.implementation.guide#1.0.0");

try (Validator validator = ValidatorFactory.withCustomPackages(configuration, packagesToLoad)) {
  var resource = FhirResource.fromJson(Path.of("/path/to/resource.json"));
  var request = new ValidationRequest(resource);
  var options =
      new ValidationOptions(
          ProfileCanonical.fromCanonical(
              "https://example.org/fhir/StructureDefinition/Profile|1.0.0"),
          null,
          ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ALL,
          ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE);

  ValidationResult result = validator.validate(request, options);
}
```

Replace the example package coordinate and profile canonical with values from the implementation guide. `RemoteDownloadPolicy.ALLOWED` permits package downloads and may require network access; use `DISALLOWED` when all packages are already available locally.

## Load packages from a validation module

A validation module JAR contains module metadata and package declarations. Load the module, select the package group applicable to the input resource, then pass those packages and the FHIR core packages to the validator. The package selector uses the resource profile and, when declared by the module, the profile validity date.

```java
import de.gematik.refv.lib.fhir_context.entity.ValidationModuleIndex;
import de.gematik.refv.lib.package_resolver.boundary.PackageResolverFactory;
import de.gematik.refv.lib.validation.boundary.ValidationPackageSelector;
import de.gematik.refv.lib.valmodule.boundary.ModuleLoader;

var configuration = ContextConfiguration.defaultConfiguration(FhirRelease.asR4());
var resource = FhirResource.fromJson(Path.of("/path/to/resource.json"));
var options = ValidationOptions.defaultConfiguration();

var module =
    ModuleLoader.defaultLoader()
        .forValidation(Path.of("./plugins"), "isik5")
        .orElseThrow(() -> new IllegalStateException("Validation module not found"));
var packageResolver = PackageResolverFactory.withConfiguration(configuration.packageLoading());
packageResolver.loadCore(configuration.fhirRelease());
packageResolver.loadModulePackages(module.modulePath());
var moduleIndex = new ValidationModuleIndex(module);
var packageSelector = new ValidationPackageSelector(moduleIndex);

var packagesToLoad = new ArrayList<>(configuration.fhirRelease().packages());
packagesToLoad.addAll(
    packageSelector.getPackagesForResource(resource, configuration.fhirRelease(), options));

try (Validator validator = ValidatorFactory.withCustomPackages(configuration, packagesToLoad)) {
  ValidationResult result = validator.validate(new ValidationRequest(resource), options);
}
```

Replace `isik5` with the module name expected by the loader and `./plugins` with the directory containing its JAR. Configure package download access as needed. Package selection and validation are separate operations: the library loads metadata and selects dependencies, while the caller creates a validator with the selected package coordinates.

## Use JSON, XML, or inline resource content

`FhirResource` provides factories for JSON and XML content from a file path or a string. The validator accepts either representation through the same request API.

```java
var jsonFromFile = FhirResource.fromJson(Path.of("/path/to/resource.json"));
var xmlFromFile = FhirResource.fromXml(Path.of("/path/to/resource.xml"));

var jsonInline = FhirResource.fromJson("""
    {"resourceType":"Patient","id":"example"}
    """);
var xmlInline = FhirResource.fromXml("""
    <Patient xmlns="http://hl7.org/fhir">
      <id value="example"/>
    </Patient>
    """);

var request = new ValidationRequest(jsonInline);
```

## Customize validation behavior

`ValidationOptions` can select a profile, configure a profile-matching regular expression used during module package selection, choose which message severities to retain, and enable or skip profile validity-period checks. The regular expression is consumed by `ValidationPackageSelector`; it does not act as a general-purpose filter on the validator's result. The defaults retain all messages and validate profile validity periods.

```java
import java.util.regex.Pattern;

var options =
    new ValidationOptions(
        ProfileCanonical.fromCanonical(
            "https://example.org/fhir/StructureDefinition/ExampleProfile|1.0.0"),
        Pattern.compile("ExampleProfile"),
        ValidationOptions.ValidationMessagesFilterStrategy.KEEP_ERRORS_AND_WARNINGS_ONLY,
        ValidationOptions.ProfileValidityPeriodCheckStrategy.VALIDATE);
```

Use non-default options only when the application requires them. In particular, disabling validity-period checks or filtering messages changes how results should be interpreted.

## Use the validation context directly

For lower-level use, create a `ValidationContext` through `ContextProvider`. It exposes validation against a collection of profile canonicals and is `AutoCloseable`.

```java
import de.gematik.refv.lib.fhir_context.boundary.ContextProvider;

var configuration = ContextConfiguration.defaultConfiguration(FhirRelease.asR4());
var resource = FhirResource.fromXml(Path.of("/path/to/resource.xml"));
var profiles =
    List.of(ProfileCanonical.fromCanonical("http://hl7.org/fhir/StructureDefinition/Bundle|4.0.1"));

try (var context = ContextProvider.defaultProvider().validationContext(configuration)) {
  var result = context.validate(resource, profiles);
  result.messages().forEach(System.out::println);
}
```

Use `validationContext(configuration, packagesToLoad)` to create a context with additional package coordinates. Close contexts when they are no longer needed.

## Generate package snapshots

Use `SnapshotGeneratorFactory` to generate snapshots for a remote package coordinate or a local package directory. Snapshot generation may download missing dependencies. The request specifies the source and output paths; keep the output separate from the original package data.

```java
import de.gematik.refv.lib.fhir_context.entity.DisplayBehaviorConfiguration;
import de.gematik.refv.lib.fhir_context.entity.TerminologyConfiguration;
import de.gematik.refv.lib.fhir_context.entity.ValidationPolicyConfiguration;
import de.gematik.refv.lib.snapshot.boundary.SnapshotGeneratorFactory;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationOptions;
import de.gematik.refv.lib.snapshot.entity.SnapshotGenerationRequest;
import java.net.URI;

var configuration =
    new ContextConfiguration(
        FhirRelease.asR4(),
        "en-GB",
        DisplayBehaviorConfiguration.defaultConfiguration(),
        PackageDownloadConfiguration.snapshotGenerationMode(),
        new TerminologyConfiguration(
            TerminologyConfiguration.RemoteLoadingPolicy.ALLOWED,
            URI.create("https://tx.fhir.org")),
        ValidationPolicyConfiguration.defaultConfiguration());

try (var generator = SnapshotGeneratorFactory.fromConfiguration(configuration)) {
  var request =
      new SnapshotGenerationRequest(
          Path.of("de.gematik.terminology#1.0.9"), Path.of("./generated-packages"));
  var result = generator.generateSnapshots(request, new SnapshotGenerationOptions());
  result.messages().forEach(System.out::println);
}
```

For a local package, use its directory as `sourcePackagePath`, for example `Path.of("./packages/my.package#1.0.0")`. To process only selected packages from a source directory, pass their coordinates as the third `SnapshotGenerationRequest` argument. To apply package patches, pass the patch directory to `new SnapshotGenerationOptions(Path.of("./patches"))`.

Close validators and snapshot generators with try-with-resources. The library removes terminology-cache directories it creates when the last owning context closes; explicitly configured terminology caches are caller-owned and are preserved. Snapshot-generation package downloads use the persistent `~/.fhir/snapshot-packages` cache, which the operator must retain or remove according to deployment needs.

## Results and exceptions

Validation returns a `ValidationResult` containing messages with severities such as `INFORMATION`, `WARNING`, `ERROR`, and `FATAL`. A result is valid when it has no `ERROR` or `FATAL` messages. Initialization and validation failures are reported with library exceptions; package download or resolution can also fail if a required package is unavailable under the configured download policy.

Snapshot generation returns a `SnapshotGenerationResult`. Inspect its messages for errors and warnings as well as successful completion; a completed call does not by itself mean that no errors occurred.

