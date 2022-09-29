/*
 * Copyright (c) 2022 gematik GmbH
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

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Optional;

/**
 * Modified from <a href="https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/util/ProfileHelper.java">https://github.com/DAV-ABDA/eRezept-Referenzvalidator/blob/478e8a2e3f0e24f54a331d561f518eeb2817ed58/core/src/main/java/de/abda/fhir/validator/core/util/ProfileHelper.java</a>
 * Copyright 2022 Deutscher Apothekerverband (DAV), Apache License, Version 2.0
 */
public class ReferencedProfileLocator {

    static Logger logger = LoggerFactory.getLogger(ReferencedProfileLocator.class);
    private static final WstxInputFactory inputFactory = new WstxInputFactory();

    public Optional<Profile> locateInXml(String resource) throws IllegalArgumentException {
        try {
            final XMLEventReader xmlEventReader;
            xmlEventReader = inputFactory.createXMLEventReader(new StringReader(resource));
            while (xmlEventReader.hasNext()) {
                XMLEvent event = xmlEventReader.nextEvent();

                if(event.isStartElement()) {
                    StartElement nextTag = event.asStartElement();
                    if (nextTag.getName().getLocalPart().equalsIgnoreCase("meta")) {

                        return locateProfileInMetaTag(xmlEventReader);
                    }
                }

            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse resource", e);
        }
        logger.debug("No meta element found");
        return Optional.empty();
    }

    private Optional<Profile> locateProfileInMetaTag(XMLEventReader xmlEventReader) throws XMLStreamException {
        StartElement nextTag;
        XMLEvent event;
        while (xmlEventReader.hasNext()) {
            event = xmlEventReader.nextEvent();
            if(event.isStartElement()) {
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

}
