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

/**
 * Event related to registration of a web application (WAB), described in OSGi CMPN 128 Web Applications Specification.
 * Before Pax Web 8 it was called {@code org.ops4j.pax.web.service.spi.WebEvent}.
 *
 * @author Achim Nierbeck
 */
public class WebApplicationEvent {

	/**
	 * A state described in 128.5 Events
	 */
	public enum State {
		DEPLOYING(1, "org/osgi/service/web/DEPLOYING"),
		DEPLOYED(2, "org/osgi/service/web/DEPLOYED"),
		UNDEPLOYING(3, "org/osgi/service/web/UNDEPLOYING"),
		UNDEPLOYED(4, "org/osgi/service/web/UNDEPLOYED"),
		FAILED(5, "org/osgi/service/web/FAILED"),
		WAITING(6, "org/osgi/service/web/WAITING"); // not mentioned in the specification

		private final int index;
		private final String topic;

		State(int index, String topic) {
			this.index = index;
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

	private final long timestamp;

	private final Exception exception;

	public WebApplicationEvent(State type, Bundle bundle) {
		this(type, bundle, null);
	}

	public WebApplicationEvent(State type, Bundle bundle, Exception exception) {
		this.type = type;
		this.bundle = bundle;
		this.bundleId = bundle.getBundleId();
		this.bundleName = bundle.getSymbolicName();
		this.bundleVersion = bundle.getVersion() == null ? Version.emptyVersion.toString() : bundle.getVersion().toString();

		this.timestamp = System.currentTimeMillis();

		this.exception = exception;
	}

	@Override
	public String toString() {
		return String.format("%s (%s/%s): %s", type, bundleName, bundleVersion,
				exception == null ? "" : " " + exception.getMessage());
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

	public Exception getException() {
		return exception;
	}

}
