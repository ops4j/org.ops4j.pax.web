/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.spi.servlet;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.osgi.service.http.context.ServletContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This {@link HttpServletRequestWrapper} ensures that servlets/filters will always get proper
 * {@link ServletContext} that implements proper, specification-defined delegation of selected methods.</p>
 *
 * <p>This wrapper also implements security requirements defined in 140.14.2.11 chapter of OSGi CMPN Whiteboard
 * Service and 102.10.2.6 chapter of OSGi CMPN Http Service specs.<blockquote>
 *     If the specified request has been authenticated, this method must set the AUTHENTICATION_TYPE
 *     request attribute to the type of authentication used, and the REMOTE_USER request attribute to the
 *     remote user (request attributes are set using the setAttribute method on the request). If this method
 *     does not perform any authentication, it must not set these attributes.
 *
 *     If the authenticated user is also authorized to access certain resources, this method must
 *     set the AUTHORIZATION request attribute to the Authorization object obtained from the
 *     org.osgi.service.useradmin.UserAdmin service.
 *
 *     The servlet responsible for servicing the specified request determines the authentication type and
 *     remote user by calling the getAuthType and getRemoteUser methods, respectively, on the request.
 * </blockquote></p>
 */
public class OsgiHttpServletRequestWrapper extends HttpServletRequestWrapper {

	public static final Logger LOG = LoggerFactory.getLogger(OsgiHttpServletRequestWrapper.class);

	private String authType = null;
	private String remoteUser = null;

	/** {@link ServletContext} that'll delegate resource access to proper OSGi context */
	private final ServletContext context;
	/** {@link ServletContext} that'll be used for {@link ServletContextHelper}-scoped session access */
	private final ServletContext osgiContext;

	private final OsgiContextModel osgiContextModel;
	private final OsgiSessionAttributeListener osgiSessionsBridge;

	/**
	 * {@link HttpSession} that ensures session separation between OSGi contexts and proper {@link ServletContext}
	 * access.
	 */
	private volatile OsgiHttpSession session;

	/**
	 * Constructs a request object wrapping the given request.
	 *
	 * @param request the {@link HttpServletRequest} to be wrapped.
	 *
	 * @param request
	 * @param context
	 * @param osgiSessionsBridge
	 * @throws IllegalArgumentException if the request is null
	 */
	public OsgiHttpServletRequestWrapper(HttpServletRequest request, ServletContext context,
			OsgiSessionAttributeListener osgiSessionsBridge) {
		super(request);
		this.context = context;
		if (context instanceof OsgiServletContext) {
			this.osgiContext = context;
			this.osgiContextModel = ((OsgiServletContext) osgiContext).getOsgiContextModel();
		} else if (context instanceof OsgiScopedServletContext) {
			this.osgiContext = context;
			this.osgiContextModel = ((OsgiScopedServletContext) osgiContext).getOsgiContextModel();
		} else {
			this.osgiContext = null;
			this.osgiContextModel = null;
		}
		this.osgiSessionsBridge = osgiSessionsBridge;
	}

	@Override
	public ServletContext getServletContext() {
		return this.context;
	}

	@Override
	public HttpSession getSession() {
		return getSession(true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public HttpSession getSession(boolean create) {
		if (session != null && session.isInvalid()) {
			session = null;
		}
		if (session == null) {
			synchronized (this) {
				if (session == null) {
					HttpSession original = super.getSession(create);
					if (original == null) {
						return null;
					}
					if (original instanceof OsgiHttpSession) {
						session = (OsgiHttpSession) original;
						return session;
					}
					Map<String, Object> localSession = null;
					String key;
					if (osgiContext != null) {
						// there's "global" session for given ServletContext, but we have to manage
						// the "local" session for given OsgiServletContext/OsgiContextModel/ServletContextHelper
						key = String.format("__osgi@session@%s",
								osgiContextModel.isWab() ? osgiContextModel.getContextPath() : osgiContextModel.getId());
						Object localSessionObject = original.getAttribute(key);
						if (localSessionObject != null) {
							if (!(localSessionObject instanceof Map)) {
								LOG.warn("Session for {} is not initialized correctly, reinitializing the session", osgiContextModel);
								localSession = new HashMap<>();
								original.setAttribute(key, localSession);
							} else {
								localSession = (Map<String, Object>) localSessionObject;
							}
						} else if (create) {
							localSession = new HashMap<>();
							original.setAttribute(key, localSession);
						}
					} else {
						// just use global session
						return original;
					}

					if (localSession == null) {
						// no session, but weren't asked to create one
						return null;
					}

					// the only place where org.ops4j.pax.web.service.spi.servlet.OsgiHttpSession
					// is created - it'll manage the session splitting by OsgiContextModel
					this.session = new OsgiHttpSession(original, localSession, key, osgiContextModel,
							osgiContext, context, osgiSessionsBridge);
				}
			}
		}

		return session;
	}

	@Override
	public void setAttribute(String name, Object o) {
		if (ServletContextHelper.AUTHENTICATION_TYPE.equals(name)) {
			if (o != null && !(o instanceof String)) {
				String message = "Wrong type of " + ServletContextHelper.AUTHENTICATION_TYPE
						+ " attribute. Expected String, was " + o.getClass();
				LOG.error(message);
				throw new IllegalArgumentException(message);
			}
			authType = o == null ? null : (String) o;
		} else if (ServletContextHelper.REMOTE_USER.equals(name)) {
			if (o != null && !(o instanceof String)) {
				String message = "Wrong type of " + ServletContextHelper.REMOTE_USER
						+ " attribute. Expected String, was " + o.getClass();
				LOG.error(message);
				throw new IllegalArgumentException(message);
			}
			remoteUser = o == null ? null : (String) o;
		}
		super.setAttribute(name, o);
	}

	@Override
	public String getAuthType() {
		return authType == null ? super.getAuthType() : authType;
	}

	@Override
	public String getRemoteUser() {
		return remoteUser == null ? super.getRemoteUser() : remoteUser;
	}

	/**
	 * Whiteboard/Http Service say nothing about this method synced with
	 * {@code org.osgi.service.http.authentication.remote.user} attribute, so let's not override it.
	 *
	 * @return
	 */
	@Override
	public Principal getUserPrincipal() {
		return super.getUserPrincipal();
	}

}
