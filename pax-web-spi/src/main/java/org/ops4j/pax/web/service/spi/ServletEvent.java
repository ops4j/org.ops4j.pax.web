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

/**
 * @author Achim Nierbeck
 *
 */
public class ServletEvent {

	private boolean replay;
	private int type;
	private Bundle bundle;
	private long timestamp;
	private String alias;
	private String servletName;
	private String[] urlParameter;
	private Servlet servlet;
	
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
		this.timestamp = event.getTimestamp();
		this.replay = replay;
	}
	
	public ServletEvent(int type, Bundle bundle, String alias, String servletName, String[] urlParameter, Servlet servlet) {
		this.type = type;
		this.bundle = bundle;
		this.alias = alias;
		this.servletName = servletName;
		this.urlParameter = urlParameter;
		this.servlet = servlet;
		this.timestamp = System.currentTimeMillis();
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ServletEvent [replay=" + replay + ", type=" + type
				+ ", bundle=" + bundle + ", timestamp=" + timestamp
				+ ", alias=" + alias + ", servletName=" +servletName
				+ ", urlParameter="+ urlParameter +", servlet="+ servlet+"]";
	}
	
}
