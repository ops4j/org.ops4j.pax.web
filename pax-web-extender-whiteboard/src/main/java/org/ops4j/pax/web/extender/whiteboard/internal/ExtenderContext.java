/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal;

import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;

/**
 * Whiteboard extender context.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 01, 2008
 */
public class ExtenderContext {

	private final ConcurrentHashMap<ContextKey, WebApplication> webApplications;

	private final ConcurrentHashMap<WebApplication, Integer> sharedWebApplicationCounter;

	private final ExtendedHttpServiceRuntime httpServiceRuntime;

	public ExtenderContext(ExtendedHttpServiceRuntime httpServiceRuntime) {
		webApplications = new ConcurrentHashMap<>();
		sharedWebApplicationCounter = new ConcurrentHashMap<>();
		this.httpServiceRuntime = httpServiceRuntime;
	}

	public WebApplication getWebApplication(final Bundle bundle,
											final String httpContextId,
											final Boolean sharedHttpContext) {
		if (bundle == null) {
			// PAXWEB-500 - it might happen that the bundle is
			// already gone!
			return null;
		}
		final ContextKey contextKey = new ContextKey(bundle, httpContextId,
				sharedHttpContext);
		WebApplication webApplication = webApplications.get(contextKey);
		if (webApplication == null) {
			webApplication = new WebApplication(bundle, httpContextId, sharedHttpContext, httpServiceRuntime);
			// PAXWEB-681 - webApplication and existing webApplication might not be the same.
			WebApplication existingWebApplication = webApplications.putIfAbsent(contextKey, webApplication);
			if (existingWebApplication == null) {
				webApplication.start();
			} else {
				webApplication = existingWebApplication;
			}
		}
		if (sharedHttpContext) {
			Integer counter = sharedWebApplicationCounter.get(webApplication);
			if (counter == null) {
				counter = 0;
			}
			sharedWebApplicationCounter.put(webApplication, ++counter);
		}
		return webApplication;
	}

	public WebApplication getExistingWebApplication(final Bundle bundle, final String httpContextId, final Boolean sharedHttpContext) {
		if (bundle == null) {
			// PAXWEB-500 - it might happen that the bundle is
			// already gone!
			return null;
		}
		final ContextKey contextKey = new ContextKey(bundle, httpContextId, sharedHttpContext);
		return webApplications.get(contextKey);
	}

	public void removeWebApplication(WebApplication webApplication) {
		ContextKey contextKey = new ContextKey(
				webApplication.getBundle(), webApplication.getHttpContextId(),
				webApplication.getSharedHttpContext());
		webApplications.remove(contextKey);
		webApplication.stop();
	}

	public Integer getSharedWebApplicationCounter(WebApplication webApplication) {
		return sharedWebApplicationCounter.get(webApplication);
	}

	public Integer reduceSharedWebApplicationCount(WebApplication webApplication) {
		Integer sharedCounter = sharedWebApplicationCounter.get(webApplication);
		--sharedCounter;
		if (sharedCounter <= 0) {
			sharedWebApplicationCounter.remove(webApplication);
		} else {
			sharedWebApplicationCounter.put(webApplication, sharedCounter);
		}
		return sharedCounter;
	}

	private static class ContextKey {

		Bundle bundle;
		String httpContextId;
		Boolean sharedHttpContext = false;

		private ContextKey(Bundle bundle, String httpContextId,
						   Boolean sharedHttpContext) {
			this.bundle = bundle;
			this.httpContextId = httpContextId;
			this.sharedHttpContext = sharedHttpContext;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			ContextKey that = (ContextKey) o;

			// skip the bundle check in case of shared Http Context
			if (!sharedHttpContext) {
				if (bundle != null ? !bundle.equals(that.bundle)
						: that.bundle != null) {
					return false;
				}
			}
			return httpContextId != null ? httpContextId
					.equals(that.httpContextId) : that.httpContextId == null;

		}

		@Override
		public int hashCode() {
			int result;
			if (!sharedHttpContext) {
				result = (bundle != null ? bundle.hashCode() : 0);
			} else {
				result = 0;
			}
			result = 31 * result
					+ (httpContextId != null ? httpContextId.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName() +
					"{" + "bundle=" + bundle +
					",httpContextId=" + httpContextId +
					"}";
		}

	}

}
