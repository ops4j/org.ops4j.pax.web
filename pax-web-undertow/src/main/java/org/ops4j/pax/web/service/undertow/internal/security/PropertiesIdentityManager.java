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
package org.ops4j.pax.web.service.undertow.internal.security;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link IdentityManager} for {@code <w:properties>} and {@code <w:users>} authentication
 * from {@code undertow.xml}.
 */
public class PropertiesIdentityManager implements IdentityManager {

	public static final Logger LOG = LoggerFactory.getLogger(PropertiesIdentityManager.class);

	private final Map<String, String> users = new HashMap<>();
	private final Map<String, Set<String>> roles = new HashMap<>();

	public PropertiesIdentityManager(Map<String, String> config) {
		config.forEach((user, credentials) -> {
			String[] creds = credentials != null ? credentials.split("\\s*,\\s*") : new String[0];
			if (creds.length == 0) {
				return;
			}
			this.users.put(user, creds[0].trim());
			if (creds.length > 1) {
				Set<String> roles = new HashSet<>();
				for (int i = 1; i < creds.length; i++) {
					roles.add(creds[i].trim());
				}
				this.roles.put(user, roles);
			} else {
				this.roles.put(user, Collections.emptySet());
			}
		});
	}

	@Override
	public Account verify(Account account) {
		return null;
	}

	@Override
	public Account verify(Credential credential) {
		return null;
	}

	@Override
	public Account verify(String id, Credential credential) {
		if (credential instanceof PasswordCredential) {
			char[] password = ((PasswordCredential) credential).getPassword();
			String pwd = users.get(id);
			if (pwd != null) {
				if (compare(pwd, new String(password))) {
					return new AccountImpl(new SimplePrincipal(id), roles.get(id));
				}
			}
		}
		return null;
	}

	private boolean compare(String stored, String provided) {
		if (stored.contains(":")) {
			String alg = stored.substring(0, stored.indexOf(':'));
			stored = stored.substring(stored.indexOf(':') + 1).toUpperCase();
			try {
				MessageDigest md = MessageDigest.getInstance(alg);
				byte[] hash = md.digest(provided.getBytes(StandardCharsets.UTF_8));
				String encoded = encode(hash);
				return stored.equals(encoded);
			} catch (NoSuchAlgorithmException e) {
				LOG.warn("Can't verify credentials: {}", e.getMessage(), e);
				return false;
			}
		} else {
			return stored.equals(provided);
		}
	}

	static class SimplePrincipal implements Principal {
		private final String name;

		SimplePrincipal(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	static class AccountImpl implements Account {
		private final Principal principal;
		private final Set<String> roles;

		AccountImpl(Principal principal, Set<String> roles) {
			this.principal = principal;
			this.roles = roles;
		}

		@Override
		public Principal getPrincipal() {
			return principal;
		}

		@Override
		public Set<String> getRoles() {
			return roles;
		}
	}

	public static String encode(byte[] bytes) {
		StringWriter sw = new StringWriter();
		for (byte b : bytes) {
			sw.append(String.format("%02x", b));
		}

		return sw.toString().toUpperCase();
	}

}
