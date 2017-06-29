/*
 * Copyright 2013 Achim Nierbeck
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletException;

import org.apache.catalina.Globals;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.SavedRequest;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anierbeck
 */
public abstract class OSGiAuthenticatorValve extends AuthenticatorBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(OSGiAuthenticatorValve.class);

	/**
	 * "Expires" header always set to Date(1), so generate once only
	 */
	private static final String DATE_ONE = (new SimpleDateFormat(
			FastHttpDateFormat.RFC1123_DATE, Locale.US)).format(new Date(1));

	@Override
	public void invoke(Request request, Response response) throws IOException,
			ServletException {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Security checking request " + request.getMethod() + " "
					+ request.getRequestURI());
		}

		// Have we got a cached authenticated Principal to record?
		if (cache) {
			Principal principal = request.getUserPrincipal();
			if (principal == null) {
				Session session = request.getSessionInternal(false);
				if (session != null) {
					principal = session.getPrincipal();
					if (principal != null) {
						//CHECKSTYLE:OFF
						if (LOG.isDebugEnabled()) {
							LOG.debug("We have cached auth type "
									+ session.getAuthType() + " for principal "
									+ session.getPrincipal());
						}
						//CHECKSTYLE:ON
						request.setAuthType(session.getAuthType());
						request.setUserPrincipal(principal);
					}
				}
			}
		}

		// Special handling for form-based logins to deal with the case
		// where the login form (and therefore the "j_security_check" URI
		// to which it submits) might be outside the secured area
		String contextPath = this.context.getPath();
		String requestURI = request.getDecodedRequestURI();
		if (requestURI.startsWith(contextPath)
				&& requestURI.endsWith(Constants.FORM_ACTION)) {
			if (!authenticate(request, response)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(" Failed authenticate() test ??" + requestURI);
				}
				return;
			}
		}

		// Special handling for form-based logins to deal with the case where
		// a resource is protected for some HTTP methods but not protected for
		// GET which is used after authentication when redirecting to the
		// protected resource.
		// TODO: This is similar to the FormAuthenticator.matchRequest() logic
		// Is there a way to remove the duplication?
		Session session = request.getSessionInternal(false);
		if (session != null) {
			SavedRequest savedRequest = (SavedRequest) session
					.getNote(Constants.FORM_REQUEST_NOTE);
			if (savedRequest != null) {
				String decodedRequestURI = request.getDecodedRequestURI();
				if (decodedRequestURI != null
						&& decodedRequestURI.equals(savedRequest
						.getDecodedRequestURI())) {
					if (!authenticate(request, response)) {
						if (LOG.isDebugEnabled()) {
							LOG.debug(" Failed authenticate() test");
						}
						/*
						 * ASSERT: Authenticator already set the appropriate
						 * HTTP status code, so we do not have to do anything
						 * special
						 */
						return;
					}
				}
			}
		}

		// The Servlet may specify security constraints through annotations.
		// Ensure that they have been processed before constraints are checked
		Wrapper wrapper = (Wrapper) request.getMappingData().wrapper;
		if (wrapper != null) {
			wrapper.servletSecurityAnnotationScan();
		}

		Realm realm = this.context.getRealm();
		// Is this request URI subject to a security constraint?
		SecurityConstraint[] constraints = realm.findSecurityConstraints(
				request, this.context);

		if (constraints == null && !context.getPreemptiveAuthentication()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(" Not subject to any constraint");
			}
			getNext().invoke(request, response);
			return;
		}

		// Make sure that constrained resources are not cached by web proxies
		// or browsers as caching can provide a security hole
		if (constraints != null && disableProxyCaching
				&& !"POST".equalsIgnoreCase(request.getMethod())) {
			if (securePagesWithPragma) {
				// Note: These can cause problems with downloading files with IE
				response.setHeader("Pragma", "No-cache");
				response.setHeader("Cache-Control", "no-cache");
			} else {
				response.setHeader("Cache-Control", "private");
			}
			response.setHeader("Expires", DATE_ONE);
		}

		int i;
		if (constraints != null) {
			// Enforce any user data constraint for this security constraint
			if (LOG.isDebugEnabled()) {
				LOG.debug(" Calling hasUserDataPermission()");
			}
			if (!realm.hasUserDataPermission(request, response, constraints)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(" Failed hasUserDataPermission() test");
				}
				/*
				 * ASSERT: Authenticator already set the appropriate HTTP status
				 * code, so we do not have to do anything special
				 */
				return;
			}
		}

		// Since authenticate modifies the response on failure,
		// we have to check for allow-from-all first.
		boolean authRequired;
		if (constraints == null) {
			authRequired = false;
		} else {
			authRequired = true;
			for (i = 0; i < constraints.length && authRequired; i++) {
				if (!constraints[i].getAuthConstraint()) {
					authRequired = false;
				} else if (!constraints[i].getAllRoles()) {
					String[] roles = constraints[i].findAuthRoles();
					if (roles == null || roles.length == 0) {
						authRequired = false;
					}
				}
			}
		}

		if (!authRequired && context.getPreemptiveAuthentication()) {
			authRequired = request.getCoyoteRequest().getMimeHeaders()
					.getValue("authorization") != null;
		}

		if (!authRequired && context.getPreemptiveAuthentication()) {
			X509Certificate[] certs = (X509Certificate[]) request
					.getAttribute(Globals.CERTIFICATES_ATTR);
			authRequired = certs != null && certs.length > 0;
		}

		if (authRequired) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(" Calling authenticate()");
			}
			if (!authenticate(request, response)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(" Failed authenticate() test");
				}
				/*
				 * ASSERT: Authenticator already set the appropriate HTTP status
				 * code, so we do not have to do anything special
				 */
				return;
			}

		}

		if (constraints != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(" Calling accessControl()");
			}
			if (!realm.hasResourcePermission(request, response, constraints,
					this.context)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(" Failed accessControl() test");
				}
				/*
				 * ASSERT: AccessControl method has already set the appropriate
				 * HTTP status code, so we do not have to do anything special
				 */
				return;
			}
		}

		// Any and all specified constraints have been satisfied
		if (LOG.isDebugEnabled()) {
			LOG.debug(" Successfully passed all security constraints");
		}
		getNext().invoke(request, response);

	}
}