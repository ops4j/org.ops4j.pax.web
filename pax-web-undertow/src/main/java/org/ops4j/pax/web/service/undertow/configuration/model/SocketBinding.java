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
package org.ops4j.pax.web.service.undertow.configuration.model;

import java.util.Map;
import javax.xml.namespace.QName;

import org.ops4j.pax.web.service.undertow.internal.configuration.ParserUtils;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

public class SocketBinding {

	private static final QName ATT_NAME = new QName("name");
	private static final QName ATT_INTERFACE = new QName("interface");
	private static final QName ATT_PORT = new QName("port");

	private String name;
	private String interfaceRef;
	private Integer port;

	public static SocketBinding create(Map<QName, String> attributes, Locator locator) throws SAXParseException {
		SocketBinding binding = new SocketBinding();
		binding.name = attributes.get(ATT_NAME);
		binding.interfaceRef = attributes.get(ATT_INTERFACE);
		binding.port = ParserUtils.toInteger(attributes.get(ATT_PORT), locator, null);

		return binding;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInterfaceRef() {
		return interfaceRef;
	}

	public void setInterfaceRef(String interfaceRef) {
		this.interfaceRef = interfaceRef;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "{ name: " + name +
				", interface: " + interfaceRef +
				", port: " + port +
				" }";
	}

}
