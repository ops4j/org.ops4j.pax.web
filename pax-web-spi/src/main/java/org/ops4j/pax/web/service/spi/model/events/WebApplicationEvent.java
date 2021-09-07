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

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.http.HttpContext;

/**
 * Event related to registration of a web application (WAB), described in OSGi CMPN 128 Web Applications Specification.
 * Before Pax Web 8 it was called {@code org.ops4j.pax.web.service.spi.WebEvent}.
 *
 * @author Achim Nierbeck
 */
public class WebApplicationEvent {

	/**
	 * A state described in 128.5 Events + one extra {@code WAITING} state. These are states as seen by observers
	 * of the deployment process, not as seen by the WAB itself (which may use some more internal states, like
	 * waiting for {@link org.ops4j.pax.web.service.WebContainer} service reference or waiting for undeployment of
	 * an application with conflicting context path (possibly including Virtual Hosts config).
	 */
	public enum State {
		DEPLOYING("org/osgi/service/web/DEPLOYING"),
		DEPLOYED("org/osgi/service/web/DEPLOYED"),
		UNDEPLOYING("org/osgi/service/web/UNDEPLOYING"),
		UNDEPLOYED("org/osgi/service/web/UNDEPLOYED"),
		FAILED("org/osgi/service/web/FAILED"),
		WAITING("org/osgi/service/web/WAITING"); // not mentioned in the specification

		private final String topic;

		State(String topic) {
			this.topic = topic;
		}

		public String getTopic() {
			return topic;
		}
	}

	private final WebApplicationEvent.State type;

	private final Bundle bundle;
	private final long bundleId;
	private final String bundleName;
	private final String bundleVersion;

	private final String contextPath;

	private final long timestamp;

	private final Throwable throwable;

	private final HttpContext context;

	private boolean awaitingAllocation = false;

	public WebApplicationEvent(State type, Bundle bundle, String contextPath, HttpContext context) {
		this(type, bundle, contextPath, context, null);
	}

	public WebApplicationEvent(State type, Bundle bundle, String contextPath, HttpContext context, Throwable throwable) {
		this.type = type;
		this.bundle = bundle;
		this.contextPath = contextPath;
		this.bundleId = bundle.getBundleId();
		this.bundleName = bundle.getSymbolicName();
		this.bundleVersion = bundle.getVersion() == null ? Version.emptyVersion.toString() : bundle.getVersion().toString();

		this.timestamp = System.currentTimeMillis();

		this.throwable = throwable;

		this.context = context;
	}

	@Override
	public String toString() {
		return String.format("%s (%s/%s)%s", type, bundleName, bundleVersion,
				throwable == null ? "" : ": " + throwable.getMessage());
	}

	public State getType() {
		return type;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public long getBundleId() {
		return bundleId;
	}

	public String getBundleName() {
		return bundleName;
	}

	public String getBundleVersion() {
		return bundleVersion;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Throwable getException() {
		return throwable;
	}

	public HttpContext getContext() {
		return context;
	}

	public String getContextPath() {
		return contextPath;
	}

	public boolean isAwaitingAllocation() {
		return awaitingAllocation;
	}

	public void setAwaitingAllocation(boolean awaitingAllocation) {
		this.awaitingAllocation = awaitingAllocation;
	}

}
