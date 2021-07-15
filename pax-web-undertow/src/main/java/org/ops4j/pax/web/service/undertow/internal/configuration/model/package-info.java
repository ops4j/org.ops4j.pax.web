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

/**
 * <p>Undertow itself doesn't define means to parse {@code undertow.xml} file. This is done by Wildfly using class
 * like {@code org.wildfly.extension.undertow.UndertowSubsystemParser_12_0}.</p>
 */
@XmlSchema(namespace = ObjectFactory.NS_PAXWEB_UNDERTOW,
		elementFormDefault = XmlNsForm.QUALIFIED, attributeFormDefault = XmlNsForm.UNQUALIFIED,
		xmlns = {
			@XmlNs(prefix = "", namespaceURI = ObjectFactory.NS_PAXWEB_UNDERTOW),
			@XmlNs(prefix = "wildfly", namespaceURI = ObjectFactory.NS_WILDFLY),
			@XmlNs(prefix = "undertow", namespaceURI = ObjectFactory.NS_UNDERTOW)
		}
)
@XmlAccessorType(XmlAccessType.FIELD)
package org.ops4j.pax.web.service.undertow.internal.configuration.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
