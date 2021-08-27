/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.undertow.internal;

import java.util.Set;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.server.session.SessionManagerStatistics;

/**
 * The only responsibility of this {@link SessionManager} is to filter out events about {@code __osgi@session@}
 * prefixed session attribute names
 */
public class PaxWebSessionManager implements SessionManager {

	private final SessionManager delegate;

	public PaxWebSessionManager(SessionManager sessionManager) {
		this.delegate = sessionManager;
	}

	@Override
	public String getDeploymentName() {
		return delegate.getDeploymentName();
	}

	@Override
	public void start() {
		delegate.start();
	}

	@Override
	public void stop() {
		delegate.stop();
	}

	@Override
	public Session createSession(HttpServerExchange serverExchange, SessionConfig sessionCookieConfig) {
		return delegate.createSession(serverExchange, sessionCookieConfig);
	}

	@Override
	public Session getSession(HttpServerExchange serverExchange, SessionConfig sessionCookieConfig) {
		return delegate.getSession(serverExchange, sessionCookieConfig);
	}

	@Override
	public Session getSession(String sessionId) {
		return delegate.getSession(sessionId);
	}

	@Override
	public void registerSessionListener(final SessionListener listener) {
		delegate.registerSessionListener(new SessionListener() {
			@Override
			public void sessionCreated(Session session, HttpServerExchange exchange) {
				listener.sessionCreated(session, exchange);
			}

			@Override
			public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
				listener.sessionDestroyed(session, exchange, reason);
			}

			@Override
			public void attributeAdded(Session session, String name, Object value) {
				if (!name.startsWith("__osgi@session@")) {
					SessionListener.super.attributeAdded(session, name, value);
				}
			}

			@Override
			public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
				if (!name.startsWith("__osgi@session@")) {
					SessionListener.super.attributeUpdated(session, name, newValue, oldValue);
				}
			}

			@Override
			public void attributeRemoved(Session session, String name, Object oldValue) {
				if (!name.startsWith("__osgi@session@")) {
					SessionListener.super.attributeRemoved(session, name, oldValue);
				}
			}

			@Override
			public void sessionIdChanged(Session session, String oldSessionId) {
				listener.sessionIdChanged(session, oldSessionId);
			}

			@Override
			public int hashCode() {
				return listener.hashCode();
			}

			@Override
			@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
			public boolean equals(Object obj) {
				return listener.equals(obj);
			}
		});
	}

	@Override
	public void removeSessionListener(SessionListener listener) {
		delegate.removeSessionListener(listener);
	}

	@Override
	public void setDefaultSessionTimeout(int timeout) {
		delegate.setDefaultSessionTimeout(timeout);
	}

	@Override
	public Set<String> getTransientSessions() {
		return delegate.getTransientSessions();
	}

	@Override
	public Set<String> getActiveSessions() {
		return delegate.getActiveSessions();
	}

	@Override
	public Set<String> getAllSessions() {
		return delegate.getAllSessions();
	}

	@Override
	public SessionManagerStatistics getStatistics() {
		return delegate.getStatistics();
	}

}
