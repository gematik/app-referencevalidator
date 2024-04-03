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
package de.gematik.refv.commons.security;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;

/**
 * Protection against the Zip Slip vulnerability
 * https://security.snyk.io/research/zip-slip-vulnerability
 * https://rules.sonarsource.com/java/RSPEC-6096/
 */
@UtilityClass
public class ZipSlipProtect {

    public static File newFile(File destDir, String currentEntryName) throws IOException {
        File destFile = new File(destDir, currentEntryName);

        String destDirPath = destDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new SecurityException("Entry is outside of the target dir: " + currentEntryName);
        }

        return destFile;
    }
}
