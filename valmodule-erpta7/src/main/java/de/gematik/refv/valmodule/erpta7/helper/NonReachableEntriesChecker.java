/*-
 * #%L
 * valmodule-erpta7
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * *******
 * 
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.valmodule.erpta7.helper;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@NoArgsConstructor
public class NonReachableEntriesChecker {

    private static final String RULE_BDL_7_MESSAGE = "Entry '%s' isn't reachable by traversing links (forward or backward) from the Composition";
    private static final String RULE_BDL_7_CODE = "Bundle_BUNDLE_Entry_Orphan_DOCUMENT";
    private final XPathFactory xPathFactory = XPathFactory.newInstance();

    public List<SingleValidationMessage> checkForNonReachableEntries(Document resource) {
        if(!isDocumentType(resource))
            return new LinkedList<>();

        var result = new LinkedList<SingleValidationMessage>();

        var allFullUrls = getAllFullUrls(resource);
        var referencedEntries = getEntriesReferencedInComposition(resource);
        Arrays.stream(allFullUrls).filter(fullUrl -> Arrays.stream(referencedEntries).noneMatch(fullUrl::endsWith)).forEach(fullUrl -> {
            SingleValidationMessage validationMessage = new SingleValidationMessage();
            validationMessage.setMessage(String.format(RULE_BDL_7_MESSAGE, fullUrl));
            validationMessage.setSeverity(ResultSeverityEnum.ERROR);
            validationMessage.setMessageId(RULE_BDL_7_CODE);
            result.add(validationMessage);
        });
        return result;
    }

    @SneakyThrows
    private boolean isDocumentType(Document resource) {
        XPath xPath = xPathFactory.newXPath();
        XPathExpression exp = xPath.compile("/Bundle/type/@value");
        return "document".equals(exp.evaluate(resource, XPathConstants.STRING));
    }

    @SneakyThrows
    private String[] getEntriesReferencedInComposition(Document resource) {
        XPath xPath = xPathFactory.newXPath();
        XPathExpression exp = xPath.compile("/Bundle/entry/resource/Composition/section/entry/reference/@value");
        NodeList references = (NodeList) exp.evaluate(resource, XPathConstants.NODESET);
        var result = new String[references.getLength()];
        for (int i = 0; i < references.getLength(); i++) {
            result[i] = references.item(i).getNodeValue();
        }
        return result;
    }

    @SneakyThrows
    private String[] getAllFullUrls(Document resource) {
        XPath xPath = xPathFactory.newXPath();
        XPathExpression exp = xPath.compile("/Bundle/entry[not(self::entry/resource/Composition)]/fullUrl/@value");
        NodeList fullUrls = (NodeList) exp.evaluate(resource, XPathConstants.NODESET);
        var result = new String[fullUrls.getLength()];
        for (int i = 0; i < fullUrls.getLength(); i++) {
            result[i] = fullUrls.item(i).getNodeValue();
        }
        return result;
    }
}
