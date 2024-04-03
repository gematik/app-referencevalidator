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
package de.gematik.refv.cli;

import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;

public class ModuleConfigurationPrinter {
    public String moduleConfigurationToString(ValidationModuleConfiguration configuration) {
        StringBuilder sb = new StringBuilder();
        addVersion(configuration, sb);
        addSupportedProfiles(configuration, sb);
        addFHIRPackages(configuration, sb);
        return sb.toString();
    }

    private void addVersion(ValidationModuleConfiguration configuration, StringBuilder sb) {
        sb.append("\r\nVersion: " + configuration.getVersion());
    }

    private static void addFHIRPackages(ValidationModuleConfiguration configuration, StringBuilder sb) {
        sb.append("\r\nFHIR-Package lists: \r\n");
        for (var dependencyListsEntrySet :
                configuration.getDependencyLists().entrySet()) {
            sb.append("  * " + dependencyListsEntrySet.getKey());
            if(dependencyListsEntrySet.getValue().getValidFrom() != null || dependencyListsEntrySet.getValue().getValidTill() != null) {
                sb.append("  (Valid ");
                if(dependencyListsEntrySet.getValue().getValidFrom() != null)
                    sb.append("from " + dependencyListsEntrySet.getValue().getValidFrom());
                if(dependencyListsEntrySet.getValue().getValidTill() != null)
                    sb.append(" till " + dependencyListsEntrySet.getValue().getValidTill());
                sb.append(")\r\n");
            }
            else
                sb.append("\n");
            for (var packageFile: dependencyListsEntrySet.getValue().getPackages()){
                sb.append("    - " + packageFile + "\r\n");
            }
        }
    }

    private static void addSupportedProfiles(ValidationModuleConfiguration configuration, StringBuilder sb) {
        sb.append("\r\nSupported profiles:");
        for (var profileEntrySet :
                configuration.getSupportedProfiles().entrySet()) {
            sb.append("\r\n  * " + profileEntrySet.getKey());
            for (var versionsEntrySet :
                    profileEntrySet.getValue().getProfileVersions().entrySet()) {
                sb.append("\r\n    -" + versionsEntrySet.getKey());
            }
        }
    }
}
