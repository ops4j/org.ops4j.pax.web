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

class WebEvent {

	private boolean replay;
	private Bundle extenderBundle;
	private long extenderBundleId;
	private String extenderBundleName;
	private String extenderBundleVersion;
	private String contextPath;
	private Collection<Long> collisionIds;
	private HttpService httpService;
	private HttpContext httpContext;

	WebEvent(WebEvent event, boolean replay) {
		this.contextPath = event.getContextPath();
		this.extenderBundle = event.getExtenderBundle();
		this.extenderBundleId = event.getExtenderBundleId();
		this.extenderBundleName = event.getExtenderBundleName();
		this.extenderBundleVersion = event.getExtenderBundleVersion();
		this.collisionIds = event.getCollisionIds();
		this.httpService = event.getHttpService();
		this.httpContext = event.getHttpContext();
		this.replay = replay;
	}

	WebEvent(int type, String contextPath, Bundle bundle,
					Bundle extenderBundle) {
		this.contextPath = contextPath;
		this.extenderBundle = extenderBundle;
		this.extenderBundleId = extenderBundle.getBundleId();
		this.extenderBundleName = extenderBundle.getSymbolicName();
		this.extenderBundleVersion = extenderBundle.getHeaders().get(Constants.BUNDLE_VERSION);
	}

	WebEvent(int type, String contextPath, Bundle bundle,
					Bundle extenderBundle, Throwable cause) {
		this(type, contextPath, bundle, extenderBundle);
	}

	WebEvent(int type, String contextPath, Bundle bundle,
					Bundle extenderBundle, Collection<Long> ids) {
		this(type, contextPath, bundle, extenderBundle);
		this.collisionIds = ids;
	}

	WebEvent(int type, String contextPath, Bundle bundle,
					Bundle extenderBundle, HttpService httpService,
					HttpContext httpContext) {
		this(type, contextPath, bundle, extenderBundle);
		this.httpContext = httpContext;
		this.httpService = httpService;
	}

	/**
	 * @return the replay
	 */
	public boolean isReplay() {
		return replay;
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
//	@Override
//	public String toString() {
//		return "WebEvent [replay=" + replay + ", type=" + getEventType(type)
//				+ ", bundle=" + bundleId + "-" + bundleName
//				+ ", extenderBundle=" + extenderBundleId + "-" + extenderBundleName
//				+ ", cause=" + cause + ", timestamp=" + timestamp
//				+ ", contextPath=" + contextPath + ", collisionIds="
//				+ collisionIds + ", httpService=" + httpService
//				+ ", httpContext=" + httpContext + "]";
//	}

	private static String getEventType(int type) {
		switch (type) {
//			case WebEvent.DEPLOYING:
//				return "DEPLOYING";
//			case WebEvent.DEPLOYED:
//				return "DEPLOYED";
//			case WebEvent.UNDEPLOYING:
//				return "UNDEPLOYING";
//			case WebEvent.UNDEPLOYED:
//				return "UNDEPLOYED";
//			case WebEvent.FAILED:
//				return "FAILED";
//			case WebEvent.WAITING:
//				return "WAITING";
			default:
				return "UNKNOWN(" + type + ")";
		}
	}

}
