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
