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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * Special {@link HttpSession} that:<ul>
 *     <li>Implements session separation from 140.2.7 "Relation to the Servlet Container"</li>
 *     <li>Returns OSGi context specific {@link ServletContext}</li>
 * </ul>
 */
public class OsgiHttpSession implements HttpSession {

	private final ServletContext osgiContext;
	private final ServletContext context;
	private final HttpSession original;
	private final Map<String, Object> scopedSession;
	private final String key;
	private final OsgiContextModel model;
	private volatile boolean invalid = false;

	private final OsgiSessionAttributeListener osgiSessionsBridge;

	public OsgiHttpSession(HttpSession original, Map<String, Object> scopedSession, String key,
			OsgiContextModel model, ServletContext osgiContext, ServletContext context,
			OsgiSessionAttributeListener osgiSessionsBridge) {
		this.original = original;
		this.scopedSession = scopedSession;
		this.key = key;
		this.model = model;
		this.osgiContext = osgiContext;
		this.context = context;
		this.osgiSessionsBridge = osgiSessionsBridge;
	}

	@Override
	public long getCreationTime() {
		checkInvalid();
		return original.getCreationTime();
	}

	@Override
	public String getId() {
		return original.getId();
	}

	@Override
	public long getLastAccessedTime() {
		checkInvalid();
		return original.getLastAccessedTime();
	}

	@Override
	public ServletContext getServletContext() {
		checkInvalid();
		return osgiContext == null ? context : osgiContext;
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		checkInvalid();
		original.setMaxInactiveInterval(interval);
	}

	@Override
	public int getMaxInactiveInterval() {
		checkInvalid();
		return original.getMaxInactiveInterval();
	}

	@Override
	@SuppressWarnings("deprecation")
	public javax.servlet.http.HttpSessionContext getSessionContext() {
		checkInvalid();
		return original.getSessionContext();
	}

	@Override
	public Object getAttribute(String name) {
		checkInvalid();
		return scopedSession != null ? scopedSession.get(name) : original.getAttribute(name);
	}

	@Override
	@SuppressWarnings("deprecation")
	public Object getValue(String name) {
		checkInvalid();
		return scopedSession != null ? scopedSession.get(name) : original.getValue(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		checkInvalid();
		return scopedSession != null ? Collections.enumeration(scopedSession.keySet()) : original.getAttributeNames();
	}

	@Override
	@SuppressWarnings("deprecation")
	public String[] getValueNames() {
		checkInvalid();
		return scopedSession != null ? scopedSession.keySet().toArray(new String[0]) : original.getValueNames();
	}

	@Override
	public void setAttribute(String name, Object value) {
		if (scopedSession != null) {
			Object old;
			if (value == null) {
				old = scopedSession.remove(name);
			} else {
				old = scopedSession.put(name, value);
			}
			if (value != old) {
				osgiSessionsBridge.callSessionListeners(this, model, name, value, old);
			}
		} else {
			original.setAttribute(name, value);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void putValue(String name, Object value) {
		checkInvalid();
		if (scopedSession != null) {
			setAttribute(name, value);
		} else {
			original.putValue(name, value);
		}
	}

	@Override
	public void removeAttribute(String name) {
		checkInvalid();
		if (scopedSession != null) {
			scopedSession.remove(name);
		} else {
			original.removeAttribute(name);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void removeValue(String name) {
		checkInvalid();
		if (scopedSession != null) {
			scopedSession.remove(name);
		} else {
			original.removeValue(name);
		}
	}

	@Override
	public void invalidate() {
		checkInvalid();
		if (scopedSession != null) {
			invalid = true;
			original.removeAttribute(key);
			// call listeners to inform about attribute removals in invalidated session
			for (Map.Entry<String, Object> e : scopedSession.entrySet()) {
				String key = e.getKey();
				Object value = e.getValue();
				this.osgiSessionsBridge.callSessionListeners(this, model, this.key, null, value);
			}
		} else {
			original.invalidate();
		}
	}

	@Override
	public boolean isNew() {
		checkInvalid();
		return original.isNew();
	}

	private void checkInvalid() {
		if (invalid) {
			throw new IllegalStateException("Session is invalid");
		}
	}

	public boolean isInvalid() {
		return invalid;
	}

}
