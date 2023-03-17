package de.gematik.refv.valmodule.base.helper;

import java.nio.file.Path;

public class ValidFolderDetector {

    public static boolean isInValidFolder(Path path) {
        Path current = path;
        do {
            if(current.getFileName().toString().equals("valid"))
                return true;

            current = current.getParent();
        } while (current != null);

        return false;
    }
}
