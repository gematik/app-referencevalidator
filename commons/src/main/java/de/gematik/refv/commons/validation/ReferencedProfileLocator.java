/*-
 * #%L
 * commons
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
package de.gematik.refv.commons.validation;

import ca.uhn.fhir.rest.api.EncodingEnum;
import com.ctc.wstx.stax.WstxInputFactory;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class ReferencedProfileLocator {

    private static final WstxInputFactory inputFactory;
    private static final JsonFactory jsonfactory = new JsonFactory();
    private static final String PROFILE_STRING = "profile";

    static {
        inputFactory = new WstxInputFactory();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    public List<String> getAllReferencedProfilesInResource(String resourceBody) {
        List<String> allProfilesInResource;

        EncodingEnum detectedEncoding = EncodingEnum.detectEncoding(resourceBody);

        if(detectedEncoding.equals(EncodingEnum.JSON)) {
            try {
                allProfilesInResource = locateInJson(resourceBody);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not parse resource", e);
            }
        }
        else
            allProfilesInResource = locateInXml(resourceBody);

        return allProfilesInResource;
    }

    private List<String> locateInXml(String resource) throws IllegalArgumentException {
        XMLEventReader xmlEventReader = null;

        int treeLevel = 0;
        try (StringReader stringReader = new StringReader(resource)) {
            xmlEventReader = inputFactory.createXMLEventReader(stringReader);
            while (xmlEventReader.hasNext()) {
                XMLEvent event = xmlEventReader.nextEvent();

                if (event instanceof DTD)
                    throw new SecurityException("DTD is not allowed");

                if (event.isStartElement()) {
                    treeLevel++;

                    // Scan top level only and skip inner resources (e.g. in Bundles)
                    if(treeLevel > 2) {
                        continue;
                    }

                    StartElement nextTag = event.asStartElement();

                    if (nextTag.getName().getLocalPart().equalsIgnoreCase("meta")) {

                        return locateProfileInMetaTagXml(xmlEventReader);
                    }
                }
                else if(event.isEndElement()) {
                    treeLevel--;
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse resource", e);
        } finally {
            if (xmlEventReader != null) {
                try {
                    xmlEventReader.close();
                } catch (XMLStreamException e) {
                    log.error("Error occurred while closing XMLEventReader", e);
                }
            }
        }
        log.debug("No meta element found");
        return new ArrayList<>();
    }

    private List<String> locateProfileInMetaTagXml(XMLEventReader xmlEventReader) throws XMLStreamException {
        StartElement nextTag;
        XMLEvent event;
        while (xmlEventReader.hasNext()) {
            event = xmlEventReader.nextEvent();
            if (event.isStartElement()) {
                nextTag = event.asStartElement();
                if (nextTag.getName().getLocalPart().equalsIgnoreCase(PROFILE_STRING)) {
                    Attribute valueAttribute = nextTag.asStartElement().getAttributeByName(new QName("", "value", ""));
                    return getAllProfilesInXml(xmlEventReader, valueAttribute);
                }
            }
        }
        log.debug("No profile element found");
        return new ArrayList<>();
    }


    private List<String> getAllProfilesInXml(XMLEventReader xmlEventReader, Attribute valueAttribute) throws XMLStreamException {
        List<String> profilesFound = new ArrayList<>();
        if (valueAttribute != null && !StringUtils.isEmpty(valueAttribute.getValue())) {
            profilesFound.add(valueAttribute.getValue());
        }
        parseXmlEventsForProfiles(xmlEventReader, profilesFound);
        return profilesFound;
    }

    private void parseXmlEventsForProfiles(XMLEventReader xmlEventReader, List<String> profilesFound) throws XMLStreamException {
        XMLEvent event;
        StartElement nextTag;
        while (xmlEventReader.hasNext()) {
            event = xmlEventReader.nextEvent();
            if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equalsIgnoreCase("meta"))
                break;
            if (event.isStartElement()) {
                nextTag = event.asStartElement();
                if (nextTag.getName().getLocalPart().equalsIgnoreCase(PROFILE_STRING) &&
                        !nextTag.getName().getLocalPart().equalsIgnoreCase("meta")) {
                    Attribute valueAttribute = nextTag.getAttributeByName(new QName("", "value", ""));
                    if (!StringUtils.isEmpty(valueAttribute.getValue())) {
                        profilesFound.add(valueAttribute.getValue());
                    }
                }
            }
        }
    }

    private List<String> locateInJson(String resource) throws IOException {
        try (JsonParser jsonParser = jsonfactory.createParser(resource)) {

            int treeLevel = 0;
            var token = jsonParser.nextToken();
            while (jsonParser.hasCurrentToken()) {
                if (token.isStructStart()) treeLevel++;
                else if (token.isStructEnd()) treeLevel--;
                else {
                    if (treeLevel <= 2) {
                        String fieldName = jsonParser.currentName();
                        if ("meta".equals(fieldName)) {
                            return locateProfileInMetaJson(jsonParser);
                        }
                    }
                }
                token = jsonParser.nextToken();
            }
        }
        log.debug("No meta element found");
        return new ArrayList<>();
    }

    private List<String> locateProfileInMetaJson(JsonParser jsonParser) throws IOException {
        while(jsonParser.hasCurrentToken()) {
            String fieldName = jsonParser.currentName();
            jsonParser.nextToken();
            if(PROFILE_STRING.equals(fieldName)) {
                jsonParser.nextToken();
                return extractProfileValuesFromJson(jsonParser);
            }
        }
        log.debug("No profile element found");
        return new ArrayList<>();
    }

    private List<String> extractProfileValuesFromJson(JsonParser jsonParser) throws IOException {
        List<String> allProfiles = getAllProfilesInJson(jsonParser);
        if (allProfiles.isEmpty()) {
            log.debug("Profile element has no value");
        }
        return allProfiles;
    }

    private List<String> getAllProfilesInJson(JsonParser jsonParser) throws IOException {
        List<String> allProfiles = new ArrayList<>();
        while (!"]".equals(jsonParser.getText()) && !"}".equals(jsonParser.getText()) ){
            String currentProfile = jsonParser.getText();
            String[] split = currentProfile.split("\\|");
            String profileUrlWithoutVersion = split[0];
            if(!StringUtils.isEmpty(profileUrlWithoutVersion) &&
                    !"}".equals(profileUrlWithoutVersion)){
                allProfiles.add(currentProfile);
            }
            jsonParser.nextToken();
        }
        return allProfiles;
    }
}
