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
package org.ops4j.pax.web.service.spi.model.info;

import jakarta.servlet.Servlet;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * A read-only class to present information about {@link ServletModel}.
 */
public class ServletInfo implements Comparable<ServletInfo> {

	private final String servletName;
	private String servletClass;
	private final String[] mapping;
	private final String[] contexts;
	private final String type;
	private final String contextFilter;

	private final boolean resourceServlet;
	private final String rawResourcePath;

	private final boolean jspServlet;

	private final Bundle bundle;
	private final String id;

	public ServletInfo(ServletModel model) {
		this.servletName = model.getName();
		this.mapping = model.getUrlPatterns();
		this.resourceServlet = model.isResourceServlet();
		this.jspServlet = model.isJspServlet();
		this.rawResourcePath = model.getRawPath();
		boolean hs = false;
		boolean whiteboard = false;
		boolean wab = false;

		// usually there's only one OCM, but for Whiteboard servlets, we may have more
		for (OsgiContextModel ocm : model.getContextModels()) {
			hs |= !(ocm.isWab() || ocm.isWhiteboard());
			whiteboard |= ocm.isWhiteboard();
			wab |= ocm.isWab();
		}
		if (whiteboard) {
			this.type = "Whiteboard";
		} else if (wab) {
			this.type = "WAB";
		} else {
			this.type = "HttpService";
		}

		this.contexts = model.getContextModels().stream().map(OsgiContextModel::getContextPath).distinct().toArray(String[]::new);
		this.contextFilter = model.getContextFilter() == null ? "-" : model.getContextFilter().toString();

		this.id = model.getId();
		this.bundle = model.getRegisteringBundle();

		this.servletClass = model.getActualClass() == null ? "?" : model.getActualClass().getName();
		if (this.servletClass.equals(Servlet.class.getName())) {
			// let's try to resolve it
			if (model.getElementReference() != null) {
				BundleContext ctx = bundle == null ? null : bundle.getBundleContext();
				if (ctx != null) {
					try {
						Servlet s = ctx.getService(model.getElementReference());
						if (s != null) {
							this.servletClass = s.getClass().getName();
						}
					} finally {
						try {
							ctx.ungetService(model.getElementReference());
						} catch (IllegalStateException ignored) {
						}
					}
				}
			}
		}
	}

	public String getServletName() {
		return servletName;
	}

	public String getServletClass() {
		return servletClass;
	}

	public String[] getMapping() {
		return mapping;
	}

	public String[] getContexts() {
		return contexts;
	}

	public String getType() {
		return type;
	}

	public String getContextFilter() {
		return contextFilter;
	}

	public boolean isResourceServlet() {
		return resourceServlet;
	}

	public String getRawResourcePath() {
		return rawResourcePath;
	}

	public boolean isJspServlet() {
		return jspServlet;
	}

	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public int compareTo(ServletInfo other) {
		// first - by bundle ID
		if (!bundle.equals(other.bundle)) {
			return bundle.getBundleId() < other.bundle.getBundleId() ? -1 : 1;
		}

		// then by servlet name
		if (!servletName.equals(other.servletName)) {
			return servletName.compareTo(other.servletName);
		}

		// finally by ID
		return id.compareTo(other.id);
	}

}
