/*
Copyright (c) 2022-2024 gematik GmbH

Licensed under the Apache License, Version 2.0 (the License);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package de.gematik.refv.plugins.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginDefinition {
    @NonNull
    private String configSpecVersion;
    @NonNull
    private String id;
    @NonNull
    private String version;
    @NonNull
    private String author;
    @NonNull
    private String description;
    private String specUrl;
    @NonNull
    private PluginDefinitionValidationSection validation;
    private List<String> snapshotDependencies = new LinkedList<>();

    private static final String MALFORMED_PACKAGE_DECLARATION_TEXT = "Malformed package name '%s' (expected format: packageName#packageVersion)";
    private static final String PACKAGE_FILENAME_FORMATTER = "%s-%s.tgz";

    public String getValidationFhirPackageAsFilename() throws MalformedPackageDeclarationException {
        String validationFhirPackage = this.getValidation().getFhirPackage();
        String[] validationFhirPackageNameAndVersion = validationFhirPackage.split("#");
        if(validationFhirPackageNameAndVersion.length < 2) {
            throw new MalformedPackageDeclarationException(String.format(MALFORMED_PACKAGE_DECLARATION_TEXT, validationFhirPackage));
        }
        return String.format(PACKAGE_FILENAME_FORMATTER, validationFhirPackageNameAndVersion[0], validationFhirPackageNameAndVersion[1]);
    }

    public List<String> getValidationDependenciesAsFilenames() throws MalformedPackageDeclarationException {
        if(this.getValidation().getDependencies() != null) {
            List<String> result = new ArrayList<>();
            for(String s : this.getValidation().getDependencies()) {
                String[] split = s.split("#");
                if(split.length < 2) {
                    throw new MalformedPackageDeclarationException(String.format(MALFORMED_PACKAGE_DECLARATION_TEXT, s));
                }
                String filename = String.format(PACKAGE_FILENAME_FORMATTER, split[0], split[1]);
                result.add(filename);
            }
            return result;
        }
        return new ArrayList<>();
    }

}

