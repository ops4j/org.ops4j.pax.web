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
package org.ops4j.pax.web.service.spi.model.events;

import javax.servlet.Servlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Event related to registration of single {@link org.ops4j.pax.web.service.spi.model.elements.ElementModel} which
 * may represent any <em>web element</em> like {@link Servlet} or {@link javax.servlet.Filter}.
 *
 * @author Achim Nierbeck
 */
public class ElementEvent {

	public enum State {
		DEPLOYING, DEPLOYED, UNDEPLOYING, UNDEPLOYED, FAILED, WAITING
	}

	private final boolean replay;

	private final State type;

	private final Bundle bundle;
	private final long bundleId;
	private final String bundleName;
	private final String bundleVersion;

	private final long timestamp;

	private final ElementEventData data;
	private final Exception exception;

	public ElementEvent(ElementEvent event, boolean replay) {
		this.type = event.getType();
		this.bundle = event.getBundle();
		this.bundleId = event.getBundleId();
		this.bundleName = event.getBundleName();
		this.bundleVersion = event.getBundleVersion();
		this.timestamp = event.getTimestamp();
		this.exception = event.exception;

		this.data = event.getData();
		this.replay = replay;
	}

	public ElementEvent(State type, ElementEventData data) {
		this(type, data, null);
	}

	public ElementEvent(State type, ElementEventData data, Exception exception) {
		this.type = type;
		this.bundle = data.getOriginBundle();
		this.bundleId = bundle.getBundleId();
		this.bundleName = bundle.getSymbolicName();
		this.bundleVersion = bundle.getVersion() == null ? Version.emptyVersion.toString() : bundle.getVersion().toString();

		this.timestamp = System.currentTimeMillis();

		this.data = data;
		this.exception = exception;

		this.replay = false;
	}

	public boolean isReplay() {
		return replay;
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

	public ElementEventData getData() {
		return data;
	}

}
