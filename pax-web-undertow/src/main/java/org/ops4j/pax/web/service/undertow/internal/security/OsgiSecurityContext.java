/*
 * Copyright 2023 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.undertow.internal.security;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.AbstractSecurityContext;
import io.undertow.server.HttpServerExchange;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class OsgiSecurityContext extends AbstractSecurityContext {

	private final Account principal;
	private final String authMechanism;

	public OsgiSecurityContext(HttpServerExchange exchange, Object principal, Object authMechanism) {
		super(exchange);
		this.principal = principal instanceof String ? new OsgiContextAccount((String) principal) : null;
		this.authMechanism = authMechanism instanceof String ? (String) authMechanism : null;
	}

	@Override
	public boolean isAuthenticated() {
		return principal != null;
	}

	@Override
	public Account getAuthenticatedAccount() {
		return principal;
	}

	@Override
	public String getMechanismName() {
		return authMechanism;
	}

	@Override
	public boolean authenticate() {
		return isAuthenticated();
	}

	@Override
	public boolean login(String username, String password) {
		return false;
	}

	@Override
	public void addAuthenticationMechanism(AuthenticationMechanism mechanism) {

	}

	@Override
	public List<AuthenticationMechanism> getAuthenticationMechanisms() {
		return Collections.emptyList();
	}

	@Override
	public IdentityManager getIdentityManager() {
		return null;
	}

	private static final class OsgiContextAccount implements Account {

		private final OsgiContextPrincipal principal;

		private OsgiContextAccount(String principal) {
			this.principal = new OsgiContextPrincipal(principal);
		}

		@Override
		public Principal getPrincipal() {
			return principal;
		}

		@Override
		public Set<String> getRoles() {
			return Collections.emptySet();
		}
	}

	private static final class OsgiContextPrincipal implements Principal {

		private final String name;

		private OsgiContextPrincipal(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
	}

}
