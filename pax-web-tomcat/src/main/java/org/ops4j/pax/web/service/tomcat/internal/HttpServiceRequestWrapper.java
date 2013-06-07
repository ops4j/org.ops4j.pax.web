/*  Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.IOException;
import java.security.Principal;
import java.util.Iterator;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Group;
import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.users.AbstractUser;
import org.apache.catalina.users.MemoryUserDatabaseFactory;
import org.ops4j.lang.NullArgumentException;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A http servlet request wrapper that can handle authentication as specified
 * for http service.
 * 
 * @author Alin Dreghiciu
 * @since December 10, 1007
 */
class HttpServiceRequestWrapper extends HttpServletRequestWrapper {

	protected static final String CONTAINER_REQUEST_ATTR_NAME = "org.ops4j.pax.web.service.internal.containerRequest";

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServiceRequestWrapper.class);

	/**
	 * Jetty request.
	 */
	private final Request request;
	/**
	 * Original request.
	 */
	private final HttpServletRequest originalRequest;

	/**
	 * Constructs a request object wrapping the given request .
	 * 
	 * @param servletRequest
	 *            original request to be wrapped
	 * 
	 * @throws IllegalArgumentException
	 *             if the request is null
	 */
	HttpServiceRequestWrapper(final HttpServletRequest servletRequest) {
		super(servletRequest);
		originalRequest = servletRequest;
		if (servletRequest instanceof Request) {
			request = (Request) servletRequest;
		} else {
			// try to find jetty request as an attrinute (set in the initial
			// request by HttpServiceServletHandler
			final Object requestAttrValue = servletRequest
					.getAttribute(CONTAINER_REQUEST_ATTR_NAME);
			if (requestAttrValue != null && requestAttrValue instanceof Request) {
				request = (Request) requestAttrValue;
			} else {
				request = null;
				LOG.debug("HttpService specific authentication is disabled because the ServletRequest object cannot be used, "
						+ "and "
						+ CONTAINER_REQUEST_ATTR_NAME
						+ " attribute is not set."
						+ " Expected to be an instance of "
						+ Request.class.getName()
						+ " but got "
						+ originalRequest.getClass().getName() + ".");
			}
		}
	}

	/**
	 * Filter the setting of authentication related attributes. If one of
	 * HttpContext.AUTHENTICATION_TYPE or HTTPContext.REMOTE_USER set the
	 * corresponding values in original request.
	 * 
	 * @see javax.servlet.http.HttpServletRequest#setAttribute(String, Object)
	 */
	@Override
	public void setAttribute(final String name, Object value) {
		if (HttpContext.AUTHENTICATION_TYPE.equals(name)) {
			handleAuthenticationType(value);
		} else if (HttpContext.REMOTE_USER.equals(name)) {
			handleRemoteUser(value);
		}
		super.setAttribute(name, value);
	}

	/**
	 * Handles setting of authentication type attribute.
	 * 
	 * @param authenticationType
	 *            new authentication type
	 */
	private void handleAuthenticationType(final Object authenticationType) {
		if (request != null) {
			if (authenticationType != null) {
				// be defensive
				if (!(authenticationType instanceof String)) {
					final String message = "Attribute "
							+ HttpContext.AUTHENTICATION_TYPE
							+ " expected to be a String but was an ["
							+ authenticationType.getClass() + "]";
					LOG.error(message);
					throw new IllegalArgumentException(message);
				}
			}
			getOsgiAuth().setAuthMethod((String) authenticationType);
		} else {
			LOG.warn("Authentication type cannot be set to "
					+ authenticationType
					+ ". HttpService specific authentication is disabled because the ServletRequest object cannot be used: "
					+ "Expected to be an instance of "
					+ Request.class.getName() + " but got "
					+ originalRequest.getClass().getName() + ".");
		}
	}

	/**
	 * Handles setting of remote user attribute.
	 * 
	 * @param remoteUser
	 *            new remote user name
	 */
	private void handleRemoteUser(final Object remoteUser) {
		if (request != null) {
			Principal userPrincipal = null;
			if (remoteUser != null) {
				// be defensive
				if (!(remoteUser instanceof String)) {
					final String message = "Attribute "
							+ HttpContext.REMOTE_USER
							+ " expected to be a String but was an ["
							+ remoteUser.getClass() + "]";
					LOG.error(message);
					throw new IllegalArgumentException(message);
				}
				MemoryUserCreationFactory.
				userPrincipal = new User((String) remoteUser);
			}
			getOsgiAuth().setUserPrincipal(userPrincipal);
		} else {
			LOG.warn("Remote user cannot be set to "
					+ remoteUser
					+ ". HttpService specific authentication is disabled because the ServletRequest object cannot be used: "
					+ "Expected to be an instance of "
					+ Request.class.getName() + " but got "
					+ originalRequest.getClass().getName() + ".");
		}
	}

	private OsgiAuth getOsgiAuth() {
		OsgiAuth auth;
		if (request.getAuthentication() instanceof OsgiAuth) {
			auth = (OsgiAuth) request.getAuthentication();
		} else {
			auth = new OsgiAuth();
			request.setAuthentication(auth);
		}
		return auth;
	}

	/**
	 * A simple user authentication
	 */
	private static class OsgiAuth extends BasicAuthenticator {

		private Principal userPrincipal;
		private String authMethod;
		
		public void setUserPrincipal(Principal principal) {
			userPrincipal = principal;
		}
		
		public Principal getUserPrincipal() {
			return userPrincipal;
		}
		
		public String getAuthMethod() {
			return authMethod;
		}
	}
}
