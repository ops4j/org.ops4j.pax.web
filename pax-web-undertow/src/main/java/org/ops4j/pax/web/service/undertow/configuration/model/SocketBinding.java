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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import static org.ops4j.pax.web.service.undertow.configuration.model.ObjectFactory.NS_WILDFLY;

@XmlType(name = "socket-bindingType", namespace = NS_WILDFLY)
public class SocketBinding {

	@XmlAttribute
	private String name;
	@XmlAttribute(name = "interface")
	private String interfaceRef;
	@XmlAttribute
	private Integer port;

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
