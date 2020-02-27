/*
 * Copyright 2011 Achim Nierbeck.
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
package org.ops4j.pax.web.service.spi;

import java.util.Arrays;
import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.http.HttpContext;

/**
 * Event related to registration of single {@link Servlet}.
 *
 * @author Achim Nierbeck
 */
public class ServletEvent {

	public enum State {
		DEPLOYING, DEPLOYED, UNDEPLOYING, UNDEPLOYED, FAILED, WAITING
	}

	private final boolean replay;
	private final State type;
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

	public ServletEvent(State type, Bundle bundle, String alias,
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
		if (servletClass != null) {
			this.servletClassName = servletClass.getCanonicalName();
		} else {
			this.servletClassName = servlet.getClass().getCanonicalName();
		}
		this.httpContext = httpContext;
		this.timestamp = System.currentTimeMillis();
		this.replay = false;
	}

	public ServletEvent(State type, Bundle bundle, ServletModel model) {
		this(type, bundle, model.getAlias(), model.getName(),
				model.getUrlPatterns(), model.getServlet(), model.getServletClass(), null);
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
	public State getType() {
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
		return "ServletEvent [replay=" + replay + ", type=" + type
				+ ", bundle=" + bundleId + "-" + bundleName + ", timestamp=" + timestamp
				+ ", alias=" + alias + ", servletName=" + servletName
				+ ", urlParameter=" + (urlParameter == null ? "null" : Arrays.asList(urlParameter))
				+ ", servletClass=" + servletClassName + "]" + ", httpContext="
				+ httpContext + "]";
	}

}
