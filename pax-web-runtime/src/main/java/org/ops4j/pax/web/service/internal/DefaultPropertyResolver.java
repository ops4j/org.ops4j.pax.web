/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.internal;

import static org.ops4j.pax.web.service.WebContainerConstants.PROPERTY_TEMP_DIR;
import static org.ops4j.util.xml.ElementHelper.getChildren;
import static org.ops4j.util.xml.ElementHelper.getRootElement;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.xml.parsers.ParserConfigurationException;

import org.ops4j.util.property.DictionaryPropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Default property resolver.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0 January 23, 2008
 */
class DefaultPropertyResolver
    extends DictionaryPropertyResolver
{

    private static final Logger LOG = LoggerFactory.getLogger( DefaultPropertyResolver.class );

    public DefaultPropertyResolver()
    {
        super( getDefaltProperties() );
    }

    private static Dictionary<String, String> getDefaltProperties()
    {
        Dictionary<String, String> properties = new Hashtable<String, String>();
        
        InputStream stream = DefaultPropertyResolver.class.getClassLoader().getResourceAsStream("OSGI-INF/metatype/metatype.xml");
        try {
			Element rootElement = getRootElement(stream);
			Element[] children = getChildren(rootElement, "AD");
			for (Element element : children) {
				String id = element.getAttribute("id");
				String required = element.getAttribute("required");
				String defaultAttribute = element.getAttribute("default");
				if (Boolean.parseBoolean(required)) {
					//it's a mandatory field initialize it even with a null
					properties.put(id, defaultAttribute);
				} else {
					if(defaultAttribute != null && defaultAttribute.length() > 0) {
						//it's no mandatory but it is configured, use it anyway
						properties.put(id, defaultAttribute);
					}
				}
			}
			
		} catch (ParserConfigurationException e) {
			LOG.error( "Could not parse metatype.xml. Reason: " + e.getMessage() );
			return properties;
		} catch (IOException e) {
			LOG.error( "Could not parse metatype.xml. Reason: " + e.getMessage() );
			return properties;
		} catch (SAXException e) {
			LOG.error( "Could not parse metatype.xml. Reason: " + e.getMessage() );
			return properties;
		}
        
        // create a temporary directory
		if (properties.get(PROPERTY_TEMP_DIR) != null) {
			//check if the provided temp directory exists
			File temporaryDirectory = new File(properties.get(PROPERTY_TEMP_DIR));
			if (!temporaryDirectory.exists()) {
				temporaryDirectory.mkdirs(); //since this is a provided temp directory it is not cleared after shutdown.
			}
		} else {
			//temp directory doesn't exist create it. 
	        try
	        {
	            File temporaryDirectory = File.createTempFile( ".paxweb", "" );
	            temporaryDirectory.delete();
	            temporaryDirectory = new File( temporaryDirectory.getAbsolutePath() );
	            temporaryDirectory.mkdirs();
	            temporaryDirectory.deleteOnExit();
	            properties.put( PROPERTY_TEMP_DIR, temporaryDirectory.getCanonicalPath() );
	        }
	        catch( Exception e )
	        {
	            LOG.warn( "Could not create temporary directory. Reason: " + e.getMessage() );
	            //properties.put( PROPERTY_TEMP_DIR, null );
	        }
		}
        return properties; 
    }

}
