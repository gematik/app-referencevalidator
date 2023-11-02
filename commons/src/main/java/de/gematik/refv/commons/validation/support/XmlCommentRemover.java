package de.gematik.refv.commons.validation.support;

import lombok.experimental.UtilityClass;
import lombok.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class XmlCommentRemover {

    public static String removeXmlCommentsFrom(@NonNull String resource) {
        String pattern = "<!--(.*?)-->";
        Pattern r = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher m = r.matcher(resource);
        return m.replaceAll("");
    }
}
