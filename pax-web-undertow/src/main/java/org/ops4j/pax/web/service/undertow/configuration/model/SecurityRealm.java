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

import org.ops4j.pax.web.service.undertow.internal.configuration.ParserUtils;
import org.xml.sax.Locator;

public class SecurityRealm {

	private static final QName ATT_NAME = new QName("name");

	private String name;

	private ServerIdentities identities;

	private Authentication authentication;

	private String userPrincipalClassName;

	private final List<String> rolePrincipalClassNames = new ArrayList<>();

	public static SecurityRealm create(Map<QName, String> attributes) {
		SecurityRealm realm = new SecurityRealm();
		realm.name = attributes.get(ATT_NAME);

		return realm;
	}

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

	public static class ServerIdentities {
		private SSLConfig ssl;

		public SSLConfig getSsl() {
			return ssl;
		}

		public void setSsl(SSLConfig ssl) {
			this.ssl = ssl;
		}
	}

	public static class SSLConfig {
		private Engine engine;

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

	public static class Authentication {
		private Truststore truststore;
		private JaasAuth jaas;
		private PropertiesAuth properties;
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

	public static class Engine {
		private static final QName ATT_ENABLED_CIPHER_SUITES = new QName("enabled-cipher-suites");
		private static final QName ATT_ENABLED_PROTOCOLS = new QName("enabled-protocols");

		private final List<String> enabledCipherSuites = new ArrayList<>();
		private final List<String> enabledProtocols = new ArrayList<>();

		public static Engine create(Map<QName, String> attributes, Locator locator) {
			Engine engine = new Engine();
			engine.getEnabledCipherSuites().addAll(ParserUtils.toStringList(attributes.get(ATT_ENABLED_CIPHER_SUITES), locator));
			engine.getEnabledProtocols().addAll(ParserUtils.toStringList(attributes.get(ATT_ENABLED_PROTOCOLS), locator));

			return engine;
		}

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

	public static class Truststore {
		protected static final QName ATT_PROVIDER = new QName("provider");
		protected static final QName ATT_PATH = new QName("path");
		protected static final QName ATT_KEYSTORE_PASSWORD = new QName("keystore-password");

		// Watch out for provider vs. type confusion
		protected String type;
		protected String path;
		protected String password;

		public static Truststore create(Map<QName, String> attributes, Locator locator) {
			Truststore truststore = new Truststore();
			truststore.type = attributes.get(ATT_PROVIDER);
			truststore.path = attributes.get(ATT_PATH);
			truststore.password = attributes.get(ATT_KEYSTORE_PASSWORD);

			return truststore;
		}

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

	public static class Keystore extends Truststore {
		protected static final QName ATT_ALIAS = new QName("alias");
		protected static final QName ATT_KEY_PASSWORD = new QName("key-password");
		protected static final QName ATT_GENERATE_SELF_SIGNED_CERTIFICATE_HOST = new QName("generate-self-signed-certificate-host");

		private String alias;
		private String keyPassword;
		private String generateSelfSignedCertificateHost;

		public static Keystore create(Map<QName, String> attributes, Locator locator) {
			Keystore keystore = new Keystore();
			keystore.type = attributes.get(ATT_PROVIDER);
			keystore.path = attributes.get(ATT_PATH);
			keystore.password = attributes.get(ATT_KEYSTORE_PASSWORD);
			keystore.alias = attributes.get(ATT_ALIAS);
			keystore.keyPassword = attributes.get(ATT_KEY_PASSWORD);
			keystore.generateSelfSignedCertificateHost = attributes.get(ATT_GENERATE_SELF_SIGNED_CERTIFICATE_HOST);

			return keystore;
		}

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

	public static class JaasAuth {
		protected static final QName ATT_NAME = new QName("name");

		private String name;

		public static JaasAuth create(Map<QName, String> attributes, Locator locator) {
			JaasAuth auth = new JaasAuth();
			auth.name = attributes.get(ATT_NAME);
			return auth;
		}

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

	public static class PropertiesAuth {
		protected static final QName ATT_PATH = new QName("path");

		private String path;

		public static PropertiesAuth create(Map<QName, String> attributes, Locator locator) {
			PropertiesAuth auth = new PropertiesAuth();
			auth.path = attributes.get(ATT_PATH);

			return auth;
		}

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

	public static class UsersAuth {
		private final List<User> users = new ArrayList<>();

		public List<User> getUsers() {
			return users;
		}

		@Override
		public String toString() {
			return "{ users: " + users + " }";
		}
	}

	public static class User {
		protected static final QName ATT_USERNAME = new QName("username");

		private String username;
		private String password;

		public static User create(Map<QName, String> attributes, Locator locator) {
			User user = new User();
			user.username = attributes.get(ATT_USERNAME);

			return user;
		}

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
