/*
 * Copyright (c) 2024 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.refv.commons.validation;

import ca.uhn.fhir.rest.api.EncodingEnum;
import com.ctc.wstx.stax.WstxInputFactory;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import de.gematik.refv.commons.Profile;
import de.gematik.refv.commons.configuration.ValidationModuleConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class ReferencedProfileLocator {

    static Logger logger = LoggerFactory.getLogger(ReferencedProfileLocator.class);
    private static final WstxInputFactory inputFactory = new WstxInputFactory();

    private static final JsonFactory jsonfactory = new JsonFactory();
    private static final String PROFILE_STRING = "profile";

    public Optional<Profile> locate(String resourceBody, ValidationModuleConfiguration configuration) throws IllegalArgumentException {
        EncodingEnum detectedEncoding = EncodingEnum.detectEncoding(resourceBody);

        if(detectedEncoding.equals(EncodingEnum.JSON)) {
            try {
                return locateInJson(resourceBody, configuration);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not parse resource", e);
            }
        }
        else
            return locateInXml(resourceBody, configuration);
    }

    private Optional<Profile> locateInXml(String resource, ValidationModuleConfiguration configuration) throws IllegalArgumentException {
        try {
            final XMLEventReader xmlEventReader;
            xmlEventReader = inputFactory.createXMLEventReader(new StringReader(resource));
            while (xmlEventReader.hasNext()) {
                XMLEvent event = xmlEventReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement nextTag = event.asStartElement();
                    if (nextTag.getName().getLocalPart().equalsIgnoreCase("meta")) {

                        return locateProfileInMetaTagXml(xmlEventReader, configuration);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse resource", e);
        }
        logger.debug("No meta element found");
        return Optional.empty();
    }

    private Optional<Profile> locateProfileInMetaTagXml(XMLEventReader xmlEventReader, ValidationModuleConfiguration configuration) throws XMLStreamException {
        StartElement nextTag;
        XMLEvent event;
        while (xmlEventReader.hasNext()) {
            event = xmlEventReader.nextEvent();
            if (event.isStartElement()) {
                nextTag = event.asStartElement();
                if (nextTag.getName().getLocalPart().equalsIgnoreCase(PROFILE_STRING)) {
                    Attribute valueAttribute = checkForMultipleProfilesXml(xmlEventReader, nextTag, configuration);
                    if (valueAttribute == null || StringUtils.isEmpty(valueAttribute.getValue())) {
                        logger.debug("Profile element has no value");
                        return Optional.empty();
                    }
                    String canonical = valueAttribute.getValue();
                    return Optional.of(Profile.parse(canonical));
                }
            }
        }
        logger.debug("No profile element found");
        return Optional.empty();
    }

    private Attribute checkForMultipleProfilesXml(XMLEventReader xmlEventReader, StartElement nextTag, ValidationModuleConfiguration configuration) throws XMLStreamException {
        Attribute valueAttribute = nextTag.asStartElement().getAttributeByName(new QName("", "value", ""));
        List<Attribute> profilesFound = getProfiles(xmlEventReader, valueAttribute);
        List<Attribute> supportedProfilesFound = getSupportedProfiles(profilesFound, configuration);
        if(!supportedProfilesFound.isEmpty())
            return supportedProfilesFound.get(0);
        else
            return valueAttribute;
    }

    private List<Attribute> getProfiles(XMLEventReader xmlEventReader, Attribute valueAttribute) throws XMLStreamException {
        XMLEvent event;
        StartElement nextTag;
        List<Attribute> profilesFound = new ArrayList<>();
        profilesFound.add(valueAttribute);
        while(xmlEventReader.hasNext()) {
            event = xmlEventReader.nextEvent();
            if(event.isEndElement() && event.asEndElement().getName().getLocalPart().equalsIgnoreCase("meta"))
                break;
            if(event.isStartElement()) {
                nextTag = event.asStartElement();
                if(nextTag.getName().getLocalPart().equalsIgnoreCase(PROFILE_STRING) &&
                        !nextTag.getName().getLocalPart().equalsIgnoreCase("meta")) {
                    valueAttribute = nextTag.asStartElement().getAttributeByName(new QName("", "value", ""));
                    profilesFound.add(valueAttribute);
                }
            }
        }
        return profilesFound;
    }

    private List<Attribute> getSupportedProfiles(List<Attribute> profilesFound, ValidationModuleConfiguration configuration) {
        List<Attribute> supportedProfilesFound = new ArrayList<>();
        for(Attribute attribute : profilesFound) {
            if(attribute != null && !"".equals(attribute.getValue()) &&
                    configuration.getSupportedProfiles().containsKey(attribute.getValue())){
                supportedProfilesFound.add(attribute);
            }
        }
        return supportedProfilesFound;
    }

    private Optional<Profile> locateInJson(String resource, ValidationModuleConfiguration configuration) throws IOException {
        try (JsonParser jsonParser = jsonfactory.createParser(resource)) {
            jsonParser.nextToken();
            while (jsonParser.hasCurrentToken()) {
                String fieldName = jsonParser.getCurrentName();
                jsonParser.nextToken();
                if ("meta".equals(fieldName)) {

                    return locateProfileInMetaJson(jsonParser, configuration);
                }
            }
        }
        logger.debug("No meta element found");
        return Optional.empty();
    }

    private Optional<Profile> locateProfileInMetaJson(JsonParser jsonParser, ValidationModuleConfiguration configuration) throws IOException {
        while(jsonParser.hasCurrentToken()) {
            String fieldName = jsonParser.getCurrentName();
            jsonParser.nextToken();
            if(PROFILE_STRING.equals(fieldName)) {
                jsonParser.nextToken();
                return readProfileValueAsStringJson(jsonParser, configuration);
            }
        }
        logger.debug("No profile element found");
        return Optional.empty();
    }

    private Optional<Profile> readProfileValueAsStringJson(JsonParser jsonParser, ValidationModuleConfiguration configuration) throws IOException {
        String profileValueAsString = checkForMultipleProfilesJson(jsonParser, configuration);
        if (StringUtils.isEmpty(profileValueAsString) || "]".equals(profileValueAsString) || "}".equals(profileValueAsString)) {
            logger.debug("Profile element has no value");
            return Optional.empty();
        }
        return Optional.of(Profile.parse(profileValueAsString));
    }

    private String checkForMultipleProfilesJson(JsonParser jsonParser, ValidationModuleConfiguration configuration) throws IOException {
        String profileValueAsString = jsonParser.getText();
        List<String> supportedProfilesFound = new ArrayList<>();
        while (!"]".equals(jsonParser.getText()) && !"}".equals(jsonParser.getText()) ){
            String currentProfile = jsonParser.getText();
            if(!StringUtils.isEmpty(currentProfile) &&
                    !"}".equals(currentProfile) &&
                    configuration.getSupportedProfiles().containsKey(currentProfile)){
                supportedProfilesFound.add(currentProfile);
            }
            jsonParser.nextToken();
        }
        if(!supportedProfilesFound.isEmpty())
            return supportedProfilesFound.get(0);
        else
            return profileValueAsString;
    }
}
