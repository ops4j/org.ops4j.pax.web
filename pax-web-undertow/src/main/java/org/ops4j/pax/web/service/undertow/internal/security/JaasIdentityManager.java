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

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.idm.X509CertificateCredential;

public class JaasIdentityManager implements IdentityManager {

	private final String realm;
	private final String userPrincipalClassName;
	private final Set<String> rolePrincipalClassNames;

	public JaasIdentityManager(Map<String, String> config) {
		this.realm = config.get("realm");
		this.userPrincipalClassName = config.get("userPrincipalClassName");
		this.rolePrincipalClassNames = Collections.singleton(config.get("rolePrincipalClassNames"));
	}

	public JaasIdentityManager(String realm, String userPrincipalClassName, Set<String> rolePrincipalClassNames) {
		this.realm = realm;
		this.userPrincipalClassName = userPrincipalClassName;
		this.rolePrincipalClassNames = rolePrincipalClassNames;
	}

	@Override
	public Account verify(Account account) {
		if (!(account instanceof AccountImpl)) {
			return null;
		}
		AccountImpl accountImpl = (AccountImpl) account;
		return verify(accountImpl.getPrincipal().getName(), accountImpl.getCredential());
	}

	@Override
	public Account verify(Credential credential) {
		if (credential instanceof X509CertificateCredential) {
			X509CertificateCredential certCredential = (X509CertificateCredential) credential;
			X509Certificate certificate = certCredential.getCertificate();
			return verify(certificate.getSubjectDN().getName(), credential);
		}
		throw new IllegalArgumentException("Parameter must be a X509CertificateCredential");
	}

	@Override
	public Account verify(final String id, Credential credential) {
		try {
			if (credential instanceof PasswordCredential) {
				final char[] password = ((PasswordCredential) credential).getPassword();
				Subject subject = new Subject();
				LoginContext loginContext = new LoginContext(realm, subject, new CallbackHandler() {
					@Override
					public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
						for (Callback callback : callbacks) {
							if (callback instanceof NameCallback) {
								((NameCallback) callback).setName(id);
							} else if (callback instanceof PasswordCallback) {
								((PasswordCallback) callback).setPassword(password);
							} else {
								throw new UnsupportedCallbackException(callback);
							}
						}
					}
				});
				loginContext.login();
				Principal userPrincipal = null;
				Set<String> roles = new HashSet<>();
				for (Principal principal : subject.getPrincipals()) {
					String clazz = principal.getClass().getName();
					if (userPrincipalClassName.equals(clazz)) {
						userPrincipal = principal;
					} else if (rolePrincipalClassNames.contains(clazz)) {
						roles.add(principal.getName());
					}
				}
				return new AccountImpl(subject, userPrincipal, roles, credential);
			}
		} catch (LoginException e) {
			return null;
		}
		return null;
	}

	private static class AccountImpl implements Account {

		private final Subject subject;
		private final Principal principal;
		private final Set<String> roles;
		private final Credential credential;

		AccountImpl(Subject subject, Principal principal, Set<String> roles, Credential credential) {
			this.subject = subject;
			this.principal = principal;
			this.roles = roles;
			this.credential = credential;
		}

		public Subject getSubject() {
			return subject;
		}

		@Override
		public Principal getPrincipal() {
			return principal;
		}

		@Override
		public Set<String> getRoles() {
			return roles;
		}

		public Credential getCredential() {
			return credential;
		}
	}
}
