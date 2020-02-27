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
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * Special {@link HttpSession} that:<ul>
 *     <li>TODO: Implements session separation from 140.2.7 "Relation to the Servlet Container"</li>
 *     <li>Returns OSGi context specific {@link ServletContext}</li>
 * </ul>
 */
public class OsgiHttpSession implements HttpSession {

	private final ServletContext context;
	private final HttpSession original;

	public OsgiHttpSession(HttpSession original, ServletContext context) {
		this.original = original;
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
		return this.context;
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
	@SuppressWarnings("deprecation")
	public HttpSessionContext getSessionContext() {
		return original.getSessionContext();
	}

	@Override
	public Object getAttribute(String name) {
		return original.getAttribute(name);
	}

	@Override
	@SuppressWarnings("deprecation")
	public Object getValue(String name) {
		return original.getValue(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return original.getAttributeNames();
	}

	@Override
	@SuppressWarnings("deprecation")
	public String[] getValueNames() {
		return original.getValueNames();
	}

	@Override
	public void setAttribute(String name, Object value) {
		original.setAttribute(name, value);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void putValue(String name, Object value) {
		original.putValue(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		original.removeAttribute(name);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void removeValue(String name) {
		original.removeValue(name);
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
