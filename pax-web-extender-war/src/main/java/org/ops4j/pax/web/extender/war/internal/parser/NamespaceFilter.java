/*
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
package org.ops4j.pax.web.extender.war.internal.parser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * This filter inserts a missing XML namespace attribute into the root element of an XML document or
 * stream or deletes an existing one. A valid namespace definition is required for unmarshalling the
 * document with JAXB.
 *
 * @author hwellmann
 */
public class NamespaceFilter extends XMLFilterImpl {

	private String namespaceUri;

	private boolean addNamespace;

	private boolean addedNamespace;

	/**
	 * Creates a namespace filter that inserts the given namespace URI.
	 *
	 * @param namespaceUri namespace URI
	 */
	public NamespaceFilter(String namespaceUri) {
		this.namespaceUri = namespaceUri;
		this.addNamespace = true;
	}

	/**
	 * Creates a namespace filter that deletes an existing namespace definition from the root
	 * element.
	 */
	public NamespaceFilter() {
		this.namespaceUri = "";
		this.addNamespace = false;
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		if (addNamespace) {
			startControlledPrefixMapping();
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts)
			throws SAXException {

		super.startElement(namespaceUri, localName, qName, atts);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		super.endElement(namespaceUri, localName, qName);
	}

	@Override
	public void startPrefixMapping(String prefix, String url) throws SAXException {

		if (addNamespace) {
			startControlledPrefixMapping();
		}
	}

	private void startControlledPrefixMapping() throws SAXException {

		if (addNamespace && !addedNamespace) {
			super.startPrefixMapping("", namespaceUri);
			addedNamespace = true;
		}
	}
}
