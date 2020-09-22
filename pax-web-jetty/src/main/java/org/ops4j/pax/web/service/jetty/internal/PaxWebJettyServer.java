/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.jetty.internal;

import org.eclipse.jetty.server.Server;

class PaxWebJettyServer extends Server {

	private String defaultAuthMethod;
	private String defaultRealmName;

//	@SuppressWarnings("unchecked")
//	private PaxWebServletContextHandler addContext(final OsgiContextModel model) {
//		Map<String, Object> attributes = new HashMap<>();
//		attributes.put("javax.servlet.context.tempdir",
//				configuration.server().getTemporaryDirectory());

//		// Fix for PAXWEB-193
//		jettyServer.setDefaultAuthMethod(configuration.security().getDefaultAuthMethod());
//		jettyServer.setDefaultRealmName(configuration.security().getDefaultRealmName());
//
//		if(this.defaultAuthMethod != null && model.getAuthMethod() == null){
//            model.setAuthMethod(this.defaultAuthMethod);
//        }
//        if(this.defaultRealmName != null && model.getRealmName() == null){
//            model.setRealmName(this.defaultRealmName);
//        }
//		if (model.getRealmName() != null && model.getAuthMethod() != null) {
//			configureSecurity(context, model.getRealmName(), model.getAuthMethod(), model.getFormLoginPage(),
//					model.getFormErrorPage());
//		}
//
//	/**
//	 * Sets the security authentication method and the realm name on the
//	 * security handler. This has to be done before the context is started.
//	 *
//	 * @param context
//	 * @param realmName
//	 * @param authMethod
//	 * @param formLoginPage
//	 * @param formErrorPage
//	 */
//	private void configureSecurity(ServletContextHandler context, String realmName, String authMethod,
//								   String formLoginPage, String formErrorPage) {
//		final SecurityHandler securityHandler = context.getSecurityHandler();
//
//		Authenticator authenticator = null;
//		if (authMethod == null) {
//			LOG.warn("UNKNOWN AUTH METHOD: " + authMethod);
//		} else {
//			switch (authMethod) {
//				case Constraint.__FORM_AUTH:
//					authenticator = new FormAuthenticator();
//					securityHandler.setInitParameter(FormAuthenticator.__FORM_LOGIN_PAGE, formLoginPage);
//					securityHandler.setInitParameter(FormAuthenticator.__FORM_ERROR_PAGE, formErrorPage);
//					break;
//				case Constraint.__BASIC_AUTH:
//					authenticator = new BasicAuthenticator();
//					break;
//				case Constraint.__DIGEST_AUTH:
//					authenticator = new DigestAuthenticator();
//					break;
//				case Constraint.__CERT_AUTH:
//					authenticator = new ClientCertAuthenticator();
//					break;
//				case Constraint.__CERT_AUTH2:
//					authenticator = new ClientCertAuthenticator();
//					break;
////				case Constraint.__SPNEGO_AUTH:
////					authenticator = new SpnegoAuthenticator();
////					break;
//				default:
//					authenticator = getAuthenticator(authMethod);
//					break;
//			}
//		}
//
//		securityHandler.setAuthenticator(authenticator);
//		securityHandler.setRealmName(realmName);
//
//	}
//
//	private Authenticator getAuthenticator(String method) {
//		ServiceLoader<AuthenticatorService> sl = ServiceLoader.load(AuthenticatorService.class, getClass().getClassLoader());
//		for (AuthenticatorService svc : sl) {
//			try {
//				Authenticator auth = svc.getAuthenticatorService(method, Authenticator.class);
//				if (auth != null) {
//					return auth;
//				}
//			} catch (Throwable t) {
//				LOG.debug("Unable to load AuthenticatorService for: " + method, t);
//			}
//		}
//		return null;
//	}

//	public String getDefaultAuthMethod() {
//	    return defaultAuthMethod;
//    }
//
//	public void setDefaultAuthMethod(String defaultAuthMethod) {
//		this.defaultAuthMethod = defaultAuthMethod;
//	}
//	public String getDefaultRealmName() {
//		return defaultRealmName;
//	}
//
//	public void setDefaultRealmName(String defaultRealmName) {
//		this.defaultRealmName = defaultRealmName;
//	}

}
