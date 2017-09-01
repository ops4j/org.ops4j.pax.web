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
@XmlSchema(namespace = "urn:org.ops4j.pax.web:undertow:1.0",
		elementFormDefault = XmlNsForm.QUALIFIED, attributeFormDefault = XmlNsForm.UNQUALIFIED,
		xmlns = {
			@XmlNs(prefix = "", namespaceURI = "urn:org.ops4j.pax.web:undertow:1.0"),
			@XmlNs(prefix = "wildfly", namespaceURI = "urn:jboss:domain:4.2"),
			@XmlNs(prefix = "undertow", namespaceURI = "urn:jboss:domain:undertow:3.1")
		}
)
@XmlAccessorType(XmlAccessType.FIELD)
package org.ops4j.pax.web.service.undertow.internal.configuration.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
