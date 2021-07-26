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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static org.ops4j.pax.web.service.undertow.configuration.model.ObjectFactory.NS_WILDFLY;

@XmlType(name = "named-interfaceType", namespace = NS_WILDFLY, propOrder = {
		"addresses"
})
public class Interface {

	@XmlAttribute
	private String name;

	@XmlElement(name = "inet-address")
	private final List<InetAddress> addresses = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<InetAddress> getAddresses() {
		return addresses;
	}

	@Override
	public String toString() {
		return "{ name: " + name +
				", address: " + addresses +
				" }";
	}

	@XmlType(name = "inet-addressType", namespace = NS_WILDFLY)
	public static class InetAddress {
		@XmlAttribute(name = "value")
		private String ip;

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		@Override
		public String toString() {
			return "{ ip: " + ip + " }";
		}
	}

}
