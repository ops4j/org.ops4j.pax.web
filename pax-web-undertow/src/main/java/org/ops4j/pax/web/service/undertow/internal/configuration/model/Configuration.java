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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import static org.ops4j.pax.web.service.undertow.internal.configuration.model.ObjectFactory.NS_UNDERTOW;

@XmlRootElement(name = "undertow")
@XmlType(name = "UndertowType", propOrder = {
		"subsystem",
		"securityRealms",
		"interfaces",
		"socketBindings"
})
@XmlAccessorType(XmlAccessType.FIELD)
public class Configuration {

	@XmlElement(namespace = NS_UNDERTOW)
	private UndertowSubsystem subsystem;

	@XmlElement(name = "security-realm")
	private List<SecurityRealm> securityRealms = new ArrayList<>();

	@XmlElement(name = "interface")
	private List<Interface> interfaces = new ArrayList<>();

	@XmlElement(name = "socket-binding")
	private List<SocketBinding> socketBindings = new ArrayList<>();

	public void setSubsystem(UndertowSubsystem subsystem) {
		this.subsystem = subsystem;
	}

	public UndertowSubsystem getSubsystem() {
		return subsystem;
	}

	public List<SecurityRealm> getSecurityRealms() {
		return securityRealms;
	}

	public List<Interface> getInterfaces() {
		return interfaces;
	}

	public List<SocketBinding> getSocketBindings() {
		return socketBindings;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n\tsubsystem: " + subsystem + "\n");
		sb.append("\tsecurity realms: {");
		for (SecurityRealm sr : securityRealms) {
			sb.append("\n\t\t" + sr);
		}
		sb.append("\n\t}\n\tinterfaces: {");
		for (Interface i : interfaces) {
			sb.append("\n\t\t" + i);
		}
		sb.append("\n\t}\n\tsocket bindings: {");
		for (SocketBinding b : socketBindings) {
			sb.append("\n\t\t" + b);
		}
		sb.append("\n\t}\n}\n");
		return sb.toString();
	}

}
