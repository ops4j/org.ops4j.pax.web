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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static org.ops4j.pax.web.service.undertow.internal.configuration.model.ObjectFactory.NS_WILDFLY;

@XmlType(name = "named-interfaceType", namespace = NS_WILDFLY, propOrder = {
		"address"
})
public class Interface {

	@XmlAttribute
	private String name;

	@XmlElement(name = "inet-address")
	private InetAddress address;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{ ");
		sb.append("name: " + name);
		sb.append(", address: " + (address == null ? "?" : address.ip));
		sb.append(" }");
		return sb.toString();
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

	}

}
