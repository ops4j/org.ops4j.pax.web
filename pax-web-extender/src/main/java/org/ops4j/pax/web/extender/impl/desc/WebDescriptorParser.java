/*
 * Copyright 2014 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.impl.desc;

import java.io.IOException;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;

import org.ops.pax.web.spi.WebAppModel;
import org.ops4j.pax.web.descriptor.gen.WebAppType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


public class WebDescriptorParser {
    
    private static Logger log = LoggerFactory.getLogger(WebDescriptorParser.class);
    
    
    public WebAppModel createWebAppModel(URL url) {
        WebAppType webAppType = parseWebXml(url);
        WebAppModelBuilder builder = new WebAppModelBuilder(webAppType);
        return builder.build();
    }
    
    public WebAppType parseWebXml(URL url)  {
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();

            // Use filter to override the namespace in the document.
            // On JDK 7, JAXB fails to parse the document if the namespace does not match
            // the one indicated by the generated JAXB model classes.
            // For some reason, the JAXB version in JDK 8 is more lenient and does
            // not require this filter.
            NamespaceFilter inFilter = new NamespaceFilter("http://xmlns.jcp.org/xml/ns/javaee");
            inFilter.setParent(reader);
            
            JAXBContext context = JAXBContext.newInstance(WebAppType.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SAXSource source = new SAXSource(inFilter, new InputSource(url.openStream()));
            
            return unmarshaller.unmarshal(source, WebAppType.class).getValue();
        }
        catch (JAXBException | IOException | SAXException  exc) {
            log.error("error parsing web.xml", exc);
        }
        return null;
    }
}
