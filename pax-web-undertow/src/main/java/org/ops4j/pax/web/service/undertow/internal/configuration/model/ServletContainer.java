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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import static org.ops4j.pax.web.service.undertow.internal.configuration.model.ObjectFactory.NS_UNDERTOW;
import static org.ops4j.pax.web.service.undertow.internal.configuration.model.ObjectFactory.NS_WILDFLY;

@XmlType(name = "servletContainerType", namespace = NS_UNDERTOW, propOrder = {
		"jspConfig",
		"websockets",
		"welcomeFiles"
})
public class ServletContainer {

	@XmlAttribute
	private String name;

	@XmlElement(name = "jsp-config")
	private JspConfig jspConfig;

	@XmlElement
	private Websockets websockets;

	@XmlElementWrapper(name = "welcome-files")
	@XmlElement(name = "welcome-file")
	private List<WelcomeFile> welcomeFiles = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public JspConfig getJspConfig() {
		return jspConfig;
	}

	public void setJspConfig(JspConfig jspConfig) {
		this.jspConfig = jspConfig;
	}

	public Websockets getWebsockets() {
		return websockets;
	}

	public void setWebsockets(Websockets websockets) {
		this.websockets = websockets;
	}

	public List<WelcomeFile> getWelcomeFiles() {
		return welcomeFiles;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{ ");
		sb.append("name: ").append(name);
		sb.append(", jsp config: ").append(jspConfig);
		sb.append(", websockets: ").append(websockets);
		sb.append(", welcome files: ").append(welcomeFiles);
		sb.append(" }");
		return sb.toString();
	}

	@XmlType(name = "jsp-configurationType", namespace = NS_UNDERTOW)
	public static class JspConfig {
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("}");
			return sb.toString();
		}
	}

	@XmlType(name = "websocketsType", namespace = NS_UNDERTOW)
	public static class Websockets {
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("}");
			return sb.toString();
		}
	}

	@XmlType(name = "welcome-fileType", namespace = NS_UNDERTOW)
	public static class WelcomeFile {
		@XmlAttribute
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("name: ").append(name);
			sb.append(" }");
			return sb.toString();
		}
	}

}
