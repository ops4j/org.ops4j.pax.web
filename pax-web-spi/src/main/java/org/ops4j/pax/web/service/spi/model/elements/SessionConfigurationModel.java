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
package org.ops4j.pax.web.service.spi.model.elements;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * <p>Set of parameters configuring HTTP Sessions, referenced from {@link OsgiContextModel}. Eventually
 * these parameters are used to configure server context session management, but nothing in practice prevents
 * the conflict, where different, bundle-scoped {@link org.ops4j.pax.web.service.WebContainer} services are used
 * to configure session parameters in the same target (wrt <em>context path</em>) server context.</p>
 *
 * <p>This model reflects {@code <session-config>} element from {@code web.xml}.</p>
 */
public class SessionConfigurationModel extends ElementModel {

	/** {@code <session-config>/<session-timeout>} */
	private Integer sessionTimeout;

	/** {@code <session-config>/<cookie-config>} */
	private SessionCookieConfig sessionCookieConfig;

	/** {@code <session-config>/<tracking-mode>} */
	private final List<SessionTrackingMode> trackingModes = new ArrayList<>(3);

	/**
	 * Name of URL parameter used for session URL rewriting.
	 * See {@code org.eclipse.jetty.server.session.SessionHandler#_sessionIdPathParameterNamePrefix}
	 */
	private String sessionURLParameter;

	/** PAXWEB-144: Name appended to session id, used to assist session affinity in a load balancer. */
	private String sessionWorkerName;

	SessionConfigurationModel(List<OsgiContextModel> contextModels) {
//		super(contextModels);
	}

	public Integer getSessionTimeout() {
		return sessionTimeout;
	}

	public void setSessionTimeout(Integer sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public SessionCookieConfig getSessionCookieConfig() {
		return sessionCookieConfig;
	}

	public void setSessionCookieConfig(SessionCookieConfig sessionCookieConfig) {
		this.sessionCookieConfig = sessionCookieConfig;
	}

	public List<SessionTrackingMode> getTrackingModes() {
		return trackingModes;
	}

	public String getSessionURLParameter() {
		return sessionURLParameter;
	}

	public void setSessionURLParameter(String sessionURLParameter) {
		this.sessionURLParameter = sessionURLParameter;
	}

	public String getSessionWorkerName() {
		return sessionWorkerName;
	}

	public void setSessionWorkerName(String sessionWorkerName) {
		this.sessionWorkerName = sessionWorkerName;
	}
	@Override
	public Boolean performValidation() {
		return Boolean.TRUE;
	}

}
