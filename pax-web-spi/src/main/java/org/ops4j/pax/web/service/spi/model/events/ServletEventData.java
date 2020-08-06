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
package org.ops4j.pax.web.service.spi.model.events;

import java.util.Arrays;
import javax.servlet.Servlet;

public class ServletEventData extends ElementEventData {

	private final String alias;
	private final String servletName;
	private String[] urlPatterns;
	private final Servlet servlet;
	private boolean resourceServlet;

	// used only by resource servlets
	private String path;

	public ServletEventData(String alias, String servletName, String[] urlPatterns, Servlet servlet) {
		this.alias = alias;
		this.servletName = servletName;
		if (urlPatterns != null) {
			this.urlPatterns = Arrays.copyOf(urlPatterns, urlPatterns.length);
		}
		this.servlet = servlet;
	}

	public String getAlias() {
		return alias;
	}

	public String getServletName() {
		return servletName;
	}

	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	public Servlet getServlet() {
		return servlet;
	}

	public boolean isResourceServlet() {
		return resourceServlet;
	}

	public void setResourceServlet(boolean resourceServlet) {
		this.resourceServlet = resourceServlet;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
