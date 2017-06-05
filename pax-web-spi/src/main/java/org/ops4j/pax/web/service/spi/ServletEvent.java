/* Copyright 2011 Achim Nierbeck.
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

package org.ops4j.pax.web.service.spi;

import java.util.Arrays;

import javax.servlet.Servlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.http.HttpContext;

/**
 * @author Achim Nierbeck
 */
public class ServletEvent {

	public static final int DEPLOYING = 1;
	public static final int DEPLOYED = 2;
	public static final int UNDEPLOYING = 3;
	public static final int UNDEPLOYED = 4;
	public static final int FAILED = 5;
	public static final int WAITING = 6;

	private final boolean replay;
	private final int type;
	private final Bundle bundle;
	private final long bundleId;
	private final String bundleName;
	private final long timestamp;
	private final String alias;
	private final String servletName;
	private final String[] urlParameter;
	private final Servlet servlet;
	private final String servletClassName;
	private final HttpContext httpContext;
	private final String bundleVersion;

	public ServletEvent(ServletEvent event, boolean replay) {
		this.type = event.getType();
		this.bundle = event.getBundle();
		this.bundleId = event.getBundleId();
		this.bundleName = event.getBundleName();
		this.bundleVersion = event.getBundleVersion();
		this.alias = event.getAlias();
		this.servletName = event.getServletName();
		this.urlParameter = event.getUrlParameter();
		this.servlet = event.getServlet();
		this.servletClassName = event.getServletClassName();
		this.timestamp = event.getTimestamp();
		this.httpContext = event.getHttpContext();
		this.replay = replay;
	}

	public ServletEvent(int type, Bundle bundle, String alias,
						String servletName, String[] urlParameter,
						Servlet servlet,
						Class<? extends Servlet> servletClass, HttpContext httpContext) {
		this.type = type;
		this.bundle = bundle;
		this.bundleId = bundle.getBundleId();
		this.bundleName = bundle.getSymbolicName();
		this.bundleVersion = bundle.getHeaders().get(Constants.BUNDLE_VERSION);
		this.alias = alias;
		this.servletName = servletName;
		if (urlParameter != null) {
			this.urlParameter = Arrays.copyOf(urlParameter, urlParameter.length);
		} else {
			this.urlParameter = null;
		}
		this.servlet = servlet;
		this.servletClassName = servletClass.getCanonicalName();
		this.httpContext = httpContext;
		this.timestamp = System.currentTimeMillis();
		this.replay = false;
	}

	/**
	 * @return the replay
	 */
	public boolean isReplay() {
		return replay;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * @return the bundle
	 */
	public Bundle getBundle() {
		return bundle;
	}

	public Long getBundleId() {
		return bundleId;
	}

	public String getBundleName() {
		return bundleName;
	}

	public String getBundleVersion() {
		return bundleVersion;
	}

	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the contextPath
	 */
	public String getAlias() {
		return alias;
	}

	public String getServletName() {
		return servletName;
	}

	/**
	 * @return the urlParameter
	 */
	public String[] getUrlParameter() {
		return urlParameter;
	}

	/**
	 * @return the servlet
	 */
	public Servlet getServlet() {
		return servlet;
	}

	/**
	 * @return the servletClass
	 */
	public String getServletClassName() {
		return servletClassName;
	}

	/**
	 * @return the httpContext
	 */
	public HttpContext getHttpContext() {
		return httpContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ServletEvent [replay=" + replay + ", type=" + type(type)
				+ ", bundle=" + bundleId + "-" + bundleName + ", timestamp=" + timestamp
				+ ", alias=" + alias + ", servletName=" + servletName
				+ ", urlParameter=" + (urlParameter == null ? "null" : Arrays.asList(urlParameter))
				+ ", servletClass=" + servletClassName + "]" + ", httpContext="
				+ httpContext + "]";
	}

	private String type(int type) {
		String name = "UNKNOWN";
		switch (type) {
			case DEPLOYING:
				name = "DEPLOYING";
				break;
			case DEPLOYED:
				name = "DEPLOYED";
				break;
			case UNDEPLOYING:
				name = "UNDEPLOYING";
				break;
			case UNDEPLOYED:
				name = "UNDEPLOYED";
				break;
			case FAILED:
				name = "FAILED";
				break;
			case WAITING:
				name = "WAITING";
				break;
		}
		return name;
	}

}
