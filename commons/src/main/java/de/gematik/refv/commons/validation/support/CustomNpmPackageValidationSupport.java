package de.gematik.refv.commons.validation.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.common.hapi.validation.support.BaseValidationSupport;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.utilities.npm.NpmPackage;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public class CustomNpmPackageValidationSupport extends BaseValidationSupport {

    private static final String PACKAGE = "package";
    private final Function<String, InputStream> packageInputStreamProvider;

    LoadingCache<String, NpmPackage> cachedNpmPackage;

    private Map<String, String> resourcesInThePackage = new HashMap<>();

    private String packagePath;

    private Map<String, IBaseResource> valueSetsAndCodeSystems = new HashMap<>();

    public static CustomNpmPackageValidationSupport create(@Nonnull FhirContext theFhirContext, @Nonnull String packagePath, @Nonnull Function<String, InputStream> packageInputStreamProvider) throws IOException {
        CustomNpmPackageValidationSupport result = new CustomNpmPackageValidationSupport(theFhirContext, packagePath, packageInputStreamProvider);
        result.indexResourcesFromPackage();
        return result;
    }
    /**
     * Constructor
     */
    private CustomNpmPackageValidationSupport(FhirContext theFhirContext, String packagePath, Function<String, InputStream> packageInputStreamProvider) {
        super(theFhirContext);
        this.packagePath = packagePath;
        this.packageInputStreamProvider = packageInputStreamProvider;
        // The NpmPackage is only required during the first validation requests so that used conformance resources can be loaded.
        // After that, the NpmPackage, which is by then held in memory, can be removed. It can be loaded any time again if further conformance resources are needed.
        cachedNpmPackage = Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterAccess(Duration.ofMinutes(1))
                .scheduler(Scheduler.systemScheduler())
                .build(this::loadNpmPackage);
    }

    @Override
    public IBaseResource fetchStructureDefinition(String theUrl) {
        return fetchResource(theUrl);
    }

    @Override
    public IBaseResource fetchValueSet(String theValueSetUrl) {
        if(!resourcesInThePackage.containsKey(theValueSetUrl))
            return null;

        IBaseResource cachedValueSet = valueSetsAndCodeSystems.get(theValueSetUrl);
        if(cachedValueSet == null) {
            var valueSet = fetchResource(theValueSetUrl);
            valueSetsAndCodeSystems.put(theValueSetUrl, valueSet);
            return valueSet;
        }
        else
            return cachedValueSet;
    }

    @Override
    public IBaseResource fetchCodeSystem(String theSystem) {
        if(!resourcesInThePackage.containsKey(theSystem))
            return null;

        IBaseResource cachedCodeSystem = valueSetsAndCodeSystems.get(theSystem);
        if(cachedCodeSystem == null) {
            var codeSystem = fetchResource(theSystem);
            valueSetsAndCodeSystems.put(theSystem, codeSystem);
            return codeSystem;
        }
        else
            return cachedCodeSystem;
    }

    @Override
    public boolean isCodeSystemSupported(ValidationSupportContext theValidationSupportContext, String theSystem) {
        return resourcesInThePackage.containsKey(theSystem);
    }

    @Override
    public boolean isValueSetSupported(ValidationSupportContext theValidationSupportContext, String theValueSetUrl) {
        return resourcesInThePackage.containsKey(theValueSetUrl);
    }

    private IBaseResource fetchResource(String url) {
        if(!resourcesInThePackage.containsKey(url))
            return null;

        String input = new String(getNpmPackage().getFolders().get(PACKAGE).getContent().get(resourcesInThePackage.get(url)), StandardCharsets.UTF_8);
        return getFhirContext().newJsonParser().parseResource(input);
    }

    private Set<String> processResourceAndReturnUrls(IBaseResource theResource, String theResourceName) {
        Validate.notNull(theResource, "the" + theResourceName + " must not be null");
        RuntimeResourceDefinition resourceDef = getFhirContext().getResourceDefinition(theResource);
        String actualResourceName = resourceDef.getName();
        Validate.isTrue(actualResourceName.equals(theResourceName), "the" + theResourceName + " must be a " + theResourceName + " - Got: " + actualResourceName);

        Optional<IBase> urlValue = resourceDef.getChildByName("url").getAccessor().getFirstValueOrNull(theResource);
        String url = urlValue.map(t -> (((IPrimitiveType<?>) t).getValueAsString())).orElse(null);

        Validate.notNull(url, "the" + theResourceName + ".getUrl() must not return null");
        Validate.notBlank(url, "the" + theResourceName + ".getUrl() must return a value");

        String urlWithoutVersion;
        int pipeIdx = url.indexOf('|');
        if (pipeIdx != -1) {
            urlWithoutVersion = url.substring(0, pipeIdx);
        } else {
            urlWithoutVersion = url;
        }

        HashSet<String> retVal = Sets.newHashSet(url, urlWithoutVersion);

        Optional<IBase> versionValue = resourceDef.getChildByName("version").getAccessor().getFirstValueOrNull(theResource);
        String version = versionValue.map(t -> (((IPrimitiveType<?>) t).getValueAsString())).orElse(null);
        if (isNotBlank(version)) {
            retVal.add(urlWithoutVersion + "|" + version);
        }

        return retVal;
    }

    private void indexResourcesFromPackage() throws IOException {
        NpmPackage.NpmPackageFolder packageFolder = getNpmPackage().getFolders().get(PACKAGE);

        for (String nextFile : packageFolder.listFiles()) {
            if(!nextFile.toLowerCase(Locale.US).endsWith(".json"))
                continue;

            try(var is = new ByteArrayInputStream(packageFolder.getContent().get(nextFile))) {
                IBaseResource resource = getFhirContext().newJsonParser().parseResource(is);
                String resourceType = getFhirContext().getResourceType(resource);
                if (resourceType.equals("StructureDefinition") || resourceType.equals("ValueSet") || resourceType.equals("CodeSystem")) {
                    var urls = processResourceAndReturnUrls(resource, resource.getClass().getSimpleName());
                    urls.stream().forEach(u -> resourcesInThePackage.put(u, nextFile));
                }
            }
        }
    }

    private NpmPackage loadNpmPackage(String packagePath) throws IOException {
        NpmPackage npmPackage;
        try(var is = packageInputStreamProvider.apply(packagePath)) {
            npmPackage = NpmPackage.fromPackage(is);
            if(!(npmPackage.getFolders().containsKey(PACKAGE)))
                throw new IllegalArgumentException("Package " + packagePath + " doesn't contain the package subfolder");
        }
        return npmPackage;
    }

    private NpmPackage getNpmPackage() {
        return cachedNpmPackage.get(packagePath);
    }
}
