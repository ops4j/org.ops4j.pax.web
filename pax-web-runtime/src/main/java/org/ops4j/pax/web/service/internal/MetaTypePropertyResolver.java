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

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.xml.parsers.ParserConfigurationException;

import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.xml.ElementHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Default property resolver that takes properties and values from MetaType XML descriptor.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0 January 23, 2008
 */
public class MetaTypePropertyResolver extends DictionaryPropertyResolver {

	private static final Logger LOG = LoggerFactory.getLogger(MetaTypePropertyResolver.class);
	private static final String METATYPE = "OSGI-INF/metatype/metatype.xml";

	private final Dictionary<String, String> properties;

	public MetaTypePropertyResolver() {
		super(null);

		properties = getDefaltProperties();
		setProperties(properties);
	}

	private static Dictionary<String, String> getDefaltProperties() {
		Dictionary<String, String> properties = new Hashtable<>();

		try (InputStream stream = MetaTypePropertyResolver.class.getClassLoader().getResourceAsStream(METATYPE)) {
			Element rootElement = ElementHelper.getRootElement(stream);
			Element[] children = ElementHelper.getChildren(rootElement, "AD");
			for (Element element : children) {
				String id = element.getAttribute("id");
				String required = element.getAttribute("required");
				String defaultAttribute = element.getAttribute("default");
				if (Boolean.parseBoolean(required)) {
					// it's a mandatory field initialize it even with a null
					properties.put(id, defaultAttribute != null && !"".equals(defaultAttribute.trim())
							? defaultAttribute : null);
				} else {
					if (defaultAttribute != null && defaultAttribute.length() > 0) {
						// it's no mandatory but it is configured, use it anyway if not empty
						properties.put(id, defaultAttribute);
					}
				}
			}
		} catch (ParserConfigurationException | IOException | SAXException e) {
			LOG.error("Could not parse metatype.xml. Reason: " + e.getMessage(), e);
			return properties;
		}

		return properties;
	}

	public Dictionary<String, String> getProperties() {
		return properties;
	}

}
