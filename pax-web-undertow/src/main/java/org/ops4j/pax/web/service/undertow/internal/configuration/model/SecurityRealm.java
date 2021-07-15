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
import javax.xml.bind.annotation.XmlType;

import static org.ops4j.pax.web.service.undertow.internal.configuration.model.ObjectFactory.NS_PAXWEB_UNDERTOW;
import static org.ops4j.pax.web.service.undertow.internal.configuration.model.ObjectFactory.NS_WILDFLY;

@XmlType(name = "security-realmType", namespace = NS_PAXWEB_UNDERTOW, propOrder = {
		"identities",
		"authentication",
		"userPrincipalClassName",
		"rolePrincipalClassNames"
})
public class SecurityRealm {

	@XmlAttribute
	private String name;

	@XmlElement(name = "server-identities", namespace = NS_WILDFLY)
	private ServerIdentities identities;

	@XmlElement(name = "authentication", namespace = NS_WILDFLY)
	private Authentication authentication;

	@XmlElement(name = "user-principal-class-name")
	private String userPrincipalClassName;

	@XmlElement(name = "role-principal-class-name")
	private final List<String> rolePrincipalClassNames = new ArrayList<>();

	public ServerIdentities getIdentities() {
		return identities;
	}

	public void setIdentities(ServerIdentities identities) {
		this.identities = identities;
	}

	public Authentication getAuthentication() {
		return authentication;
	}

	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUserPrincipalClassName() {
		return userPrincipalClassName;
	}

	public void setUserPrincipalClassName(String userPrincipalClassName) {
		this.userPrincipalClassName = userPrincipalClassName;
	}

	public List<String> getRolePrincipalClassNames() {
		return rolePrincipalClassNames;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{\n");
		sb.append("\t\t\tname: ").append(name);
		if (identities != null) {
			sb.append("\n\t\t\tssl: ").append(identities.getSsl());
		}
		sb.append("\n\t\t\tauthentication: ").append(authentication);
		sb.append("\n\t\t\tuser principal class name: ").append(userPrincipalClassName);
		sb.append("\n\t\t\trole principal class names: ").append(rolePrincipalClassNames);
		sb.append("\n\t\t}");
		return sb.toString();
	}

	@XmlType(name = "server-identitiesType", namespace = NS_WILDFLY, propOrder = {
			"ssl"
	})
	public static class ServerIdentities {
		@XmlElement(name = "ssl")
		private SSLConfig ssl;

		public SSLConfig getSsl() {
			return ssl;
		}

		public void setSsl(SSLConfig ssl) {
			this.ssl = ssl;
		}
	}

	@XmlType(name = "sslType", namespace = NS_WILDFLY, propOrder = {
			"engine",
			"keystore"
	})
	public static class SSLConfig {
		@XmlElement(name = "engine")
		private Engine engine;

		@XmlElement(name = "keystore")
		private Keystore keystore;

		public Engine getEngine() {
			return engine;
		}

		public void setEngine(Engine engine) {
			this.engine = engine;
		}

		public Keystore getKeystore() {
			return keystore;
		}

		public void setKeystore(Keystore keystore) {
			this.keystore = keystore;
		}

		@Override
		public String toString() {
			return "{\n\t\t\t\tengine: " + engine +
					"\n\t\t\t\tkeystore: " + keystore +
					"\n\t\t\t}";
		}
	}

	@XmlType(name = "authenticationType", namespace = NS_WILDFLY, propOrder = {
			"truststore",
			"jaas",
			"properties",
			"users"
	})
	public static class Authentication {
		@XmlElement
		private Truststore truststore;
		@XmlElement
		private JaasAuth jaas;
		@XmlElement
		private PropertiesAuth properties;
		@XmlElement
		private UsersAuth users;

		public Truststore getTruststore() {
			return truststore;
		}

		public void setTruststore(Truststore truststore) {
			this.truststore = truststore;
		}

		public JaasAuth getJaas() {
			return jaas;
		}

		public void setJaas(JaasAuth jaas) {
			this.jaas = jaas;
		}

		public PropertiesAuth getProperties() {
			return properties;
		}

		public void setProperties(PropertiesAuth properties) {
			this.properties = properties;
		}

		public UsersAuth getUsers() {
			return users;
		}

		public void setUsers(UsersAuth users) {
			this.users = users;
		}

		@Override
		public String toString() {
			return "{ truststore: " + truststore +
					", jaas: " + jaas +
					", properties: " + properties +
					", users: " + users +
					" }";
		}
	}

	@XmlType(name = "engineType", namespace = NS_WILDFLY)
	public static class Engine {
		@XmlAttribute(name = "enabled-cipher-suites")
		private final List<String> enabledCipherSuites = new ArrayList<>();
		@XmlAttribute(name = "enabled-protocols")
		private final List<String> enabledProtocols = new ArrayList<>();

		public List<String> getEnabledCipherSuites() {
			return enabledCipherSuites;
		}

		public List<String> getEnabledProtocols() {
			return enabledProtocols;
		}

		@Override
		public String toString() {
			return "{ enabled ciphers: " + enabledCipherSuites +
					", enabled protocols: " + enabledProtocols +
					" }";
		}
	}

	@XmlType(name = "realmKeyStoreType", namespace = NS_WILDFLY)
	public static class Truststore {
		// Watch out for provider vs. type confusion
		@XmlAttribute(name = "provider")
		private String type;
		@XmlAttribute
		private String path;
		@XmlAttribute(name = "keystore-password")
		private String password;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		@Override
		public String toString() {
			return "{ provider: " + type +
					", path: " + path +
					" }";
		}
	}

	@XmlType(name = "realmExtendedKeyStoreType", namespace = NS_WILDFLY)
	public static class Keystore extends Truststore {
		@XmlAttribute
		private String alias;
		@XmlAttribute(name = "key-password")
		private String keyPassword;
		@XmlAttribute(name = "generate-self-signed-certificate-host")
		private String generateSelfSignedCertificateHost;

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		public String getKeyPassword() {
			return keyPassword;
		}

		public void setKeyPassword(String keyPassword) {
			this.keyPassword = keyPassword;
		}

		public String getGenerateSelfSignedCertificateHost() {
			return generateSelfSignedCertificateHost;
		}

		public void setGenerateSelfSignedCertificateHost(String generateSelfSignedCertificateHost) {
			this.generateSelfSignedCertificateHost = generateSelfSignedCertificateHost;
		}

		@Override
		public String toString() {
			return "{ provider: " + getType() +
					", path: " + getPath() +
					", alias: " + alias +
					" }";
		}
	}

	@XmlType(name = "jaasAuthenticationType", namespace = NS_WILDFLY)
	public static class JaasAuth {
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
			return "{ name: " + name + " }";
		}
	}

	@XmlType(name = "propertiesAuthenticationType", namespace = NS_WILDFLY)
	public static class PropertiesAuth {
		@XmlAttribute
		private String path;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		@Override
		public String toString() {
			return "{ path: " + path + " }";
		}
	}

	@XmlType(name = "usersAuthenticationType", namespace = NS_WILDFLY)
	public static class UsersAuth {
		@XmlElement(name = "user")
		private final List<User> users = new ArrayList<>();

		public List<User> getUsers() {
			return users;
		}

		@Override
		public String toString() {
			return "{ users: " + users + " }";
		}
	}

	@XmlType(name = "userType", namespace = NS_WILDFLY)
	public static class User {
		@XmlAttribute
		private String username;
		@XmlElement
		private String password;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		@Override
		public String toString() {
			return username + "/***";
		}
	}

}
