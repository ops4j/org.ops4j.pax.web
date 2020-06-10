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

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;

public class PropertiesIdentityManager implements IdentityManager {

	private final Map<String, String> config;

	public PropertiesIdentityManager(Map<String, String> config) {
		this.config = config;
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
			String userData = config.get(id);
			if (userData != null) {
				List<String> pieces = Arrays.asList(userData.split(","));
				if (pieces.get(0).equals(new String(password))) {
					Principal principal = new SimplePrincipal(id);
					Set<String> roles = new HashSet<>(pieces.subList(1, pieces.size()));
					return new AccountImpl(principal, roles);
				}
			}
		}
		return null;
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

}
