/*
 * Copyright (c) 2023 gematik GmbH
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

package de.gematik.refv.commons;

import ca.uhn.fhir.rest.api.EncodingEnum;
import com.ctc.wstx.stax.WstxInputFactory;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
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
import java.util.Optional;

/**
 * Modified from <a href="https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/util/ProfileHelper.java">https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/util/ProfileHelper.java</a>
 * Copyright 2022 Deutscher Apothekerverband (DAV), Apache License, Version 2.0
 */
public class ReferencedProfileLocator {

    static Logger logger = LoggerFactory.getLogger(ReferencedProfileLocator.class);
    private static final WstxInputFactory inputFactory = new WstxInputFactory();

    private static final JsonFactory jsonfactory = new JsonFactory();

    public Optional<Profile> locate(String resourceBody) throws IllegalArgumentException {
        EncodingEnum detectedEncoding = EncodingEnum.detectEncoding(resourceBody);

        if(detectedEncoding.equals(EncodingEnum.JSON)) {
            try {
                return locateInJson(resourceBody);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not parse resource", e);
            }
        }
        else
            return locateInXml(resourceBody);
    }

    private Optional<Profile> locateInXml(String resource) throws IllegalArgumentException {
        try {
            final XMLEventReader xmlEventReader;
            xmlEventReader = inputFactory.createXMLEventReader(new StringReader(resource));
            while (xmlEventReader.hasNext()) {
                XMLEvent event = xmlEventReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement nextTag = event.asStartElement();
                    if (nextTag.getName().getLocalPart().equalsIgnoreCase("meta")) {

                        return locateProfileInMetaTagXml(xmlEventReader);
                    }
                }

            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse resource", e);
        }
        logger.debug("No meta element found");
        return Optional.empty();
    }

    private Optional<Profile> locateProfileInMetaTagXml(XMLEventReader xmlEventReader) throws XMLStreamException {
        StartElement nextTag;
        XMLEvent event;
        while (xmlEventReader.hasNext()) {
            event = xmlEventReader.nextEvent();
            if (event.isStartElement()) {
                nextTag = event.asStartElement();
                if (nextTag.getName().getLocalPart().equalsIgnoreCase("profile")) {
                    Attribute valueAttribute = nextTag.asStartElement().getAttributeByName(new QName("", "value", ""));
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

    private Optional<Profile> locateInJson(String resource) throws IOException {
        try (JsonParser jsonParser = jsonfactory.createParser(resource)) {
            jsonParser.nextToken();
            while (jsonParser.hasCurrentToken()) {
                String fieldName = jsonParser.getCurrentName();
                jsonParser.nextToken();
                if ("meta".equals(fieldName)) {

                    return locateProfileInMetaJson(jsonParser);
                }
            }
        }
        logger.debug("No meta element found");
        return Optional.empty();
    }

    private Optional<Profile> locateProfileInMetaJson(JsonParser jsonParser) throws IOException {
        while(jsonParser.hasCurrentToken()) {
            String fieldName = jsonParser.getCurrentName();
            jsonParser.nextToken();
            if("profile".equals(fieldName)) {
                jsonParser.nextToken();
                return readProfileValueAsStringJson(jsonParser);
            }
        }
        logger.debug("No profile element found");
        return Optional.empty();
    }

    private Optional<Profile> readProfileValueAsStringJson(JsonParser jsonParser) throws IOException {
            String profileValueAsString = jsonParser.getText();
            if (StringUtils.isEmpty(profileValueAsString) || "]".equals(profileValueAsString) || "}".equals(profileValueAsString)) {
                logger.debug("Profile element has no value");
                return Optional.empty();
            }
            return Optional.of(Profile.parse(profileValueAsString));
    }
}
