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

import java.util.Enumeration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

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

	public OsgiHttpSession(HttpSession original, ServletContext osgiContext, ServletContext context,
			OsgiSessionAttributeListener osgiSessionsBridge) {
		this.original = original;
		this.osgiContext = osgiContext;
		this.context = context;
	}

	@Override
	public long getCreationTime() {
		return original.getCreationTime();
	}

	@Override
	public String getId() {
		return original.getId();
	}

	@Override
	public long getLastAccessedTime() {
		return original.getLastAccessedTime();
	}

	@Override
	public ServletContext getServletContext() {
		return osgiContext == null ? context : osgiContext;
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		original.setMaxInactiveInterval(interval);
	}

	@Override
	public int getMaxInactiveInterval() {
		return original.getMaxInactiveInterval();
	}

	@Override
	public Object getAttribute(String name) {
		return original.getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return original.getAttributeNames();
	}

	@Override
	public void setAttribute(String name, Object value) {
		original.setAttribute(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		original.removeAttribute(name);
	}

	@Override
	public void invalidate() {
		original.invalidate();
	}

	@Override
	public boolean isNew() {
		return original.isNew();
	}

}
