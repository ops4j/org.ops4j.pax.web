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

import javax.servlet.Servlet;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * @author Achim Nierbeck
 *
 */
public class ServletEvent {

	private final boolean replay;
	private final int type;
	private final Bundle bundle;
	private final long timestamp;
	private final String alias;
	private final String servletName;
	private final String[] urlParameter;
	private final Servlet servlet;
	private final Class<? extends Servlet> servletClass;
	private final HttpContext httpContext;
	
	public static final int DEPLOYING = 1;
	public static final int DEPLOYED = 2;
	public static final int UNDEPLOYING = 3;
	public static final int UNDEPLOYED = 4;
	public static final int FAILED = 5;
    public static final int WAITING = 6;

	
	public ServletEvent(ServletEvent event, boolean replay) {
		this.type = event.getType();
		this.bundle = event.getBundle();
		this.alias = event.getAlias();
		this.servletName = event.getServletName();
		this.urlParameter = event.getUrlParameter();
		this.servlet = event.getServlet();
		this.servletClass = event.getServletClass();
		this.timestamp = event.getTimestamp();
		this.httpContext = event.getHttpContext();
		this.replay = replay;
	}
	
	public ServletEvent(int type, Bundle bundle, String alias, String servletName, String[] urlParameter, Servlet servlet, Class<? extends Servlet> servletClass, HttpContext httpContext) {
		this.type = type;
		this.bundle = bundle;
		this.alias = alias;
		this.servletName = servletName;
		this.urlParameter = urlParameter;
		this.servlet = servlet;
		this.servletClass = servletClass;
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
    public Class<? extends Servlet> getServletClass() {
        return servletClass;
    }
	
	/**
	 * @return the httpContext
	 */
	public HttpContext getHttpContext() {
		return httpContext;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ServletEvent [replay=" + replay + ", type=" + type
				+ ", bundle=" + bundle + ", timestamp=" + timestamp
				+ ", alias=" + alias + ", servletName=" +servletName
				+ ", urlParameter="+ urlParameter +", servlet="+ servlet
                + ", servletClass="+ servletClass +"]"
				+ ", httpContext="+ httpContext +"]";
	}

}
