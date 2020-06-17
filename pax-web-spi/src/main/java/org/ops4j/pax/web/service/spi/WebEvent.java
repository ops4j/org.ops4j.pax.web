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

import java.util.Collection;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

public class WebEvent {

	public enum WebTopic {
		DEPLOYING("org/osgi/service/web/DEPLOYING"),
		DEPLOYED("org/osgi/service/web/DEPLOYED"),
		UNDEPLOYING("org/osgi/service/web/UNDEPLOYING"),
		UNDEPLOYED("org/osgi/service/web/UNDEPLOYED"),
		FAILED("org/osgi/service/web/FAILED"),
		WAITING("org/osgi/service/web/WAITING");

		private final String topic;

		WebTopic(String topic) {
			this.topic = topic;
		}

		@Override
		public String toString() {
			return topic;
		}

	}

	public static final int DEPLOYING = 1;
	public static final int DEPLOYED = 2;
	public static final int UNDEPLOYING = 3;
	public static final int UNDEPLOYED = 4;
	public static final int FAILED = 5;
	public static final int WAITING = 6;

	private boolean replay;
	private int type;
	private Bundle bundle;
	private long bundleId;
	private String bundleName;
	private String bundleVersion;
	private Bundle extenderBundle;
	private long extenderBundleId;
	private String extenderBundleName;
	private String extenderBundleVersion;
	private Throwable cause;
	private long timestamp;
	private String contextPath;
	private Collection<Long> collisionIds;
	private HttpService httpService;
	private HttpContext httpContext;

	public WebEvent(WebEvent event, boolean replay) {
		this.type = event.getType();
		this.contextPath = event.getContextPath();
		this.bundle = event.getBundle();
		this.bundleId = event.getBundleId();
		this.bundleName = event.getBundleName();
		this.bundleVersion = event.getBundleVersion();
		this.extenderBundle = event.getExtenderBundle();
		this.extenderBundleId = event.getExtenderBundleId();
		this.extenderBundleName = event.getExtenderBundleName();
		this.extenderBundleVersion = event.getExtenderBundleVersion();
		this.collisionIds = event.getCollisionIds();
		this.cause = event.getCause();
		this.timestamp = event.getTimestamp();
		this.httpService = event.getHttpService();
		this.httpContext = event.getHttpContext();
		this.replay = replay;
	}

	public WebEvent(int type, String contextPath, Bundle bundle,
					Bundle extenderBundle) {
		this.timestamp = System.currentTimeMillis();
		this.type = type;
		this.contextPath = contextPath;
		this.bundle = bundle;
		this.bundleId = bundle.getBundleId();
		this.bundleName = bundle.getSymbolicName();
		this.bundleVersion = bundle.getHeaders().get(Constants.BUNDLE_VERSION);
		this.extenderBundle = extenderBundle;
		this.extenderBundleId = extenderBundle.getBundleId();
		this.extenderBundleName = extenderBundle.getSymbolicName();
		this.extenderBundleVersion = extenderBundle.getHeaders().get(Constants.BUNDLE_VERSION);
	}

	public WebEvent(int type, String contextPath, Bundle bundle,
					Bundle extenderBundle, Throwable cause) {
		this(type, contextPath, bundle, extenderBundle);
		this.cause = cause;
	}

	public WebEvent(int type, String contextPath, Bundle bundle,
					Bundle extenderBundle, Collection<Long> ids) {
		this(type, contextPath, bundle, extenderBundle);
		this.collisionIds = ids;
	}

	public WebEvent(int type, String contextPath, Bundle bundle,
					Bundle extenderBundle, HttpService httpService,
					HttpContext httpContext) {
		this(type, contextPath, bundle, extenderBundle);
		this.httpContext = httpContext;
		this.httpService = httpService;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * @return the replay
	 */
	public boolean isReplay() {
		return replay;
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
	 * @return the extenderBundle
	 */
	public Bundle getExtenderBundle() {
		return extenderBundle;
	}

	public Long getExtenderBundleId() {
		return extenderBundleId;
	}

	public String getExtenderBundleName() {
		return extenderBundleName;
	}

	public String getExtenderBundleVersion() {
		return extenderBundleVersion;
	}

	/**
	 * @return the cause
	 */
	public Throwable getCause() {
		return cause;
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
	public String getContextPath() {
		return contextPath;
	}

	/**
	 * @return the collisionIds
	 */
	public Collection<Long> getCollisionIds() {
		return collisionIds;
	}

	/**
	 * @return the HTTP service.
	 */
	public HttpService getHttpService() {
		return httpService;
	}

	/**
	 * @return the HTTP context
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
		return "WebEvent [replay=" + replay + ", type=" + getEventType(type)
				+ ", bundle=" + bundleId + "-" + bundleName
				+ ", extenderBundle=" + extenderBundleId + "-" + extenderBundleName
				+ ", cause=" + cause + ", timestamp=" + timestamp
				+ ", contextPath=" + contextPath + ", collisionIds="
				+ collisionIds + ", httpService=" + httpService
				+ ", httpContext=" + httpContext + "]";
	}

	private static String getEventType(int type) {
		switch (type) {
			case WebEvent.DEPLOYING:
				return "DEPLOYING";
			case WebEvent.DEPLOYED:
				return "DEPLOYED";
			case WebEvent.UNDEPLOYING:
				return "UNDEPLOYING";
			case WebEvent.UNDEPLOYED:
				return "UNDEPLOYED";
			case WebEvent.FAILED:
				return "FAILED";
			case WebEvent.WAITING:
				return "WAITING";
			default:
				return "UNKNOWN(" + type + ")";
		}
	}

}
