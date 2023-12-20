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
