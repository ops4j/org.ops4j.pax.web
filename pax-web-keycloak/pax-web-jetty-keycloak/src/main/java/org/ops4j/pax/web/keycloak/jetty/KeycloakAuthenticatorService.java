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
package org.ops4j.pax.web.keycloak.jetty;

import org.ops4j.pax.web.service.AuthenticatorService;

public class KeycloakAuthenticatorService implements AuthenticatorService {

	@Override
	public <T> T getAuthenticatorService(String method, Class<T> iface) {
		if (method == null || iface != org.eclipse.jetty.security.Authenticator.class) {
			return null;
		}

		if ("KEYCLOAK".equalsIgnoreCase(method)) {
			return iface.cast(new org.keycloak.adapters.jetty.KeycloakJettyAuthenticator());
		}

		return null;
	}

}
