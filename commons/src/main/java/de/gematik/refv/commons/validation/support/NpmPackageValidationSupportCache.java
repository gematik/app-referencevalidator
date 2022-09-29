/*
 * Copyright (c) 2022 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.refv.commons.validation.support;

import ca.uhn.fhir.context.FhirContext;
import lombok.NonNull;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modified from <a href="https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/support/NpmPackageValidationSupportCache.java">https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/support/NpmPackageValidationSupportCache.java</a>
 * Copyright 2022 Deutscher Apothekerverband (DAV), Apache License, Version 2.0
 *
 * Erweiterung des std. Lademechanismus zur Unterstützung von Caching. D.h.
 * Versionierte Resourcen werden im Cache abgelegt und nicht bei jeder Anfragen
 * erneut geladen.
 *
 */
public class NpmPackageValidationSupportCache {

	private final Map<String, NpmPackageValidationSupport> packageCache = new ConcurrentHashMap<>(0);
	private final FhirContext fhirContext;

	public NpmPackageValidationSupportCache(@NonNull FhirContext ctx) {
		this.fhirContext = ctx;
	}

	public PrePopulatedValidationSupport createPrePopulatedValidationSupport(List<String> packageFilesToLoad) throws IOException {

		final PrePopulatedValidationSupport result = new PrePopulatedValidationSupport(this.fhirContext);

		for(String packagePath : packageFilesToLoad)
			packageCache.computeIfAbsent(packagePath, p -> new NpmPackageValidationSupport(this.fhirContext));

		for (Map.Entry<String, NpmPackageValidationSupport> entry :
				packageCache.entrySet()) {
			entry.getValue().loadPackageFromClasspath(entry.getKey());
			List<IBaseResource> baseResources = entry.getValue().fetchAllConformanceResources();

			if(baseResources == null)
				continue;

			for(IBaseResource resource : baseResources) {
				result.addResource(resource);
			}
		}

		/*
		 * Scheinbar gibt es Abhängigkeiten oder die erzeugten Instanzen werden nachträglich manipuliert.
		 * Wenn diese Instanzen wiederverwendet werden, führt dies zu Fehlern wenn unterschiedliche
		 * Profilversionen hintereinander validiert werden.
		 */
		this.packageCache.clear();

		return result;
	}


}
