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
package org.ops4j.pax.web.service.undertow.internal.configuration;

import java.util.Map;

import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class ResolvingContentHandler implements ContentHandler {

	public static Logger LOG = LoggerFactory.getLogger(ResolvingContentHandler.class);

	private final Map<String, String> config;
	private final ContentHandler target;

	public ResolvingContentHandler(Map<String, String> config, ContentHandler target) {
		this.config = config;
		this.target = target;
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		target.setDocumentLocator(locator);
	}

	@Override
	public void startDocument() throws SAXException {
		target.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		target.endDocument();
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		target.startPrefixMapping(prefix, uri);
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		target.endPrefixMapping(prefix);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		AttributesImpl resolvedAttributes = new AttributesImpl(atts);
		for (int i = 0; i < atts.getLength(); i++) {
			resolvedAttributes.setAttribute(i, atts.getURI(i), atts.getLocalName(i), atts.getQName(i),
					atts.getType(i), Utils.resolve(config, atts.getValue(i), null));
		}
		target.startElement(uri, localName, qName, resolvedAttributes);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		target.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		target.characters(ch, start, length);
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		target.ignorableWhitespace(ch, start, length);
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		this.target.processingInstruction(target, data);
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		target.skippedEntity(name);
	}

}
