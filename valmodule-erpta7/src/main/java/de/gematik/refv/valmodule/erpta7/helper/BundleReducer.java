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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.valmodule.erpta7.helper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.XmlParser;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Slf4j
public class BundleReducer {

    private final DocumentBuilder documentBuilder;
    private final XPathFactory xPathFactory = XPathFactory.newInstance();
    private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private final Document document;
    private final XmlParser xmlParser = (XmlParser) FhirContext.forR4().newXmlParser();

    @SneakyThrows
    public BundleReducer(String resourceBody) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
        this.document = documentBuilder.parse(new InputSource(new StringReader(resourceBody)));

    }

    @SneakyThrows
    public BundleReducer(Document document) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
        this.document = document;
    }


    public List<String> getAllRezeptBundlesAsString() throws XPathExpressionException, TransformerException {
        List<String> allEntriesAsStrings = new ArrayList<>();
        XPath xpath = xPathFactory.newXPath();
        NodeList resources = (NodeList) xpath.compile("/Bundle/entry/resource/Bundle").evaluate(document, XPathConstants.NODESET);

        for (int i = 0; i < resources.getLength(); i++) {
            Node resource = resources.item(i);
            Element rootElement = (Element) resource;
            rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "http://hl7.org/fhir");
            allEntriesAsStrings.add(serializeToString(rootElement));
        }

        return allEntriesAsStrings;
    }

    public String getReducedResourceBody() throws XPathExpressionException, TransformerException {
        NodeList fixedChildren = getFixedChildren(document);
        NodeList entries = getEntries(document);

        Document newDoc = documentBuilder.newDocument();
        Element rootElement = newDoc.createElementNS("http://hl7.org/fhir", "Bundle");
        newDoc.appendChild(rootElement);

        XPath xPath = xPathFactory.newXPath();
        XPathExpression compositionExpression = xPath.compile("resource/Composition");
        NodeList compositionNodes = (NodeList) compositionExpression.evaluate(entries.item(0), XPathConstants.NODESET);
        if(compositionNodes.getLength() == 0) {
            // First entry is not Composition --> keep it in the reduced body (which will produce validation errors later if Bundle is of type document)
            Node importedNode = newDoc.importNode(entries.item(0), true);
            rootElement.appendChild(importedNode);
            copyFixedChildren(newDoc, rootElement, fixedChildren);
        }
        else {
            copyFixedChildren(newDoc, rootElement, fixedChildren);
            copyFirstRezeptBundle(newDoc, rootElement, entries);
        }

        var reducedBodyAsString = serializeToString(newDoc);

        return removeDangledReferencesFromComposition(reducedBodyAsString);
    }

    private String removeDangledReferencesFromComposition(String reducedBodyAsString) {
        var bundle = xmlParser.parseResource(Bundle.class, reducedBodyAsString);
        var compositionOptional = bundle.getEntry().stream().filter(e -> e.getResource().hasType("Composition")).findFirst();
        if(compositionOptional.isEmpty()) {
            log.warn("Could not find composition in reduced bundle");
            return reducedBodyAsString;
        }

        var allFullUrls = bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getFullUrl).collect(Collectors.toList());
        Composition c = (Composition)(compositionOptional.get().getResource());
        var sectionsWithRechnungBundles = c.getSection().stream().filter(BundleReducer::isSectionWithReferenceToRechnungBundle).collect(Collectors.toList());
        sectionsWithRechnungBundles.stream().filter(s -> isSectionReferenceMissingInReducedBundle(s, allFullUrls)).forEach(s -> c.getSection().remove(s));

        return xmlParser.encodeResourceToString(bundle);
    }

    private static boolean isSectionReferenceMissingInReducedBundle(Composition.SectionComponent s, List<String> allFullUrls) {
        return !allFullUrls.contains(s.getEntry().get(0).getReference());
    }

    private static boolean isSectionWithReferenceToRechnungBundle(Composition.SectionComponent s) {
        return !s.getEntry().isEmpty() &&
                s.getCode().hasCoding("https://fhir.gkvsv.de/CodeSystem/GKVSV_CS_ERP_TA7", "RB");
    }

    private NodeList getFixedChildren(Document doc) throws XPathExpressionException {
        XPath xpath = xPathFactory.newXPath();
        XPathExpression expr = xpath.compile("/Bundle/*[not(self::entry/resource/Bundle)]");
        return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
    }

    private NodeList getEntries(Document doc) throws XPathExpressionException {
        XPath xpath = xPathFactory.newXPath();
        return (NodeList) xpath.compile("/Bundle/entry").evaluate(doc, XPathConstants.NODESET);
    }

    private void copyFixedChildren(Document newDoc, Element rootElement, NodeList fixedChildren) {
        for (int i = 0; i < fixedChildren.getLength(); i++) {
            Node fixedNode = fixedChildren.item(i);
            Node importedNode = newDoc.importNode(fixedNode, true);
            rootElement.appendChild(importedNode);
        }
    }

    private void copyFirstRezeptBundle(Document newDoc, Element rootElement, NodeList entries) throws XPathExpressionException {
        XPath xPath = xPathFactory.newXPath();
        XPathExpression bundleExpression = xPath.compile("resource/Bundle");

        for (int i = 0; i < entries.getLength(); i++) {
            Node entryNode = entries.item(i);
            NodeList bundleNodes = (NodeList) bundleExpression.evaluate(entryNode, XPathConstants.NODESET);
            if (bundleNodes.getLength() > 0) {
                Node importedNode = newDoc.importNode(entryNode, true);
                rootElement.appendChild(importedNode);
                break;
            }
        }
    }

    // This implementation produces different line endings in different environments (e.g. local vs jenkins).
    // But this doesn't affect the validation process and is faster than other implementations that set fix line endings (e.g. LSSerializer).
    private String serializeToString(Node newDoc) throws TransformerException {
        StringWriter sw = new StringWriter();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(newDoc), new StreamResult(sw));
        return sw.toString();
    }
}
