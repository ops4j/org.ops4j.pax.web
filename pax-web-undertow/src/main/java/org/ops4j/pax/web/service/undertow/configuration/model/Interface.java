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
import java.util.Map;
import javax.xml.namespace.QName;

import org.xml.sax.Locator;

public class Interface {

	private static final QName ATT_NAME = new QName("name");

	private String name;

	private final List<InetAddress> addresses = new ArrayList<>();

	public static Interface create(Map<QName, String> attributes) {
		Interface iface = new Interface();
		iface.name = attributes.get(ATT_NAME);

		return iface;
	}

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

	public static class InetAddress {
		private static final QName ATT_VALUE = new QName("value");

		private String ip;

		public static InetAddress create(Map<QName, String> attributes, Locator locator) {
			InetAddress address = new InetAddress();
			address.ip = attributes.get(ATT_VALUE);

			return address;
		}

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
