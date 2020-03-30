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
package org.ops4j.pax.web.service.undertow.internal.configuration.model;

import javax.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

	static final String NS_PAXWEB_UNDERTOW = "urn:org.ops4j.pax.web:undertow:1.0";
	static final String NS_IO = "urn:jboss:domain:io:3.0";
	static final String NS_UNDERTOW = "urn:jboss:domain:undertow:4.0";
	static final String NS_WILDFLY = "urn:jboss:domain:5.0";

	public UndertowConfiguration createConfiguration() {
		return new UndertowConfiguration();
	}

}
