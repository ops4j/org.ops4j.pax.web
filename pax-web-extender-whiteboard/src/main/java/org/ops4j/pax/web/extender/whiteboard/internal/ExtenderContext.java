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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.ops4j.pax.swissbox.core.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * Whiteboard extender context.
 * 
 * @author Alin Dreghiciu
 * @since 0.4.0, April 01, 2008
 */
public class ExtenderContext implements BundleListener {

	private final ConcurrentHashMap<Bundle, HttpServiceTracker> httpServiceTrackers;
	private final ConcurrentHashMap<ContextKey, WebApplication> webApplications;

	public ExtenderContext() {
		httpServiceTrackers = new ConcurrentHashMap<Bundle, HttpServiceTracker>();
		webApplications = new ConcurrentHashMap<ContextKey, WebApplication>();
	}

	public WebApplication getWebApplication(final Bundle bundle,
			final String httpContextId) {
		if (bundle == null) {
			// PAXWEB-500 - it might happen that the bundle is
			// already gone!
			return null;
		}
		final ContextKey contextKey = new ContextKey(bundle, httpContextId);
		WebApplication webApplication = webApplications.get(contextKey);
		if (webApplication == null) {
			webApplication = new WebApplication();
			webApplications.putIfAbsent(contextKey, webApplication);
			HttpServiceTracker httpServiceTracker = getHttpServiceTrackers()
					.get(bundle);
			if (httpServiceTracker == null) {
				httpServiceTracker = new HttpServiceTracker(
						BundleUtils.getBundleContext(bundle));
				httpServiceTracker.open();
				getHttpServiceTrackers()
						.putIfAbsent(bundle, httpServiceTracker);
			}
			httpServiceTracker.addListener(webApplication);
		}
		return webApplication;
	}

	public void removeWebApplications(ContextKey contextKey) {
		WebApplication webApplication = webApplications.remove(contextKey);
		if (webApplication != null) {
			HttpServiceTracker httpServiceTracker = getHttpServiceTrackers()
					.get(contextKey.bundle);
			if (httpServiceTracker != null) {
				httpServiceTracker.removeListener(webApplication);
			}
		}
	}

	private static class ContextKey {

		Bundle bundle;
		String httpContextId;

		private ContextKey(Bundle bundle, String httpContextId) {
			this.bundle = bundle;
			this.httpContextId = httpContextId;
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

			if (bundle != null ? !bundle.equals(that.bundle)
					: that.bundle != null) {
				return false;
			}
			if (httpContextId != null ? !httpContextId
					.equals(that.httpContextId) : that.httpContextId != null) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result;
			result = (bundle != null ? bundle.hashCode() : 0);
			result = 31 * result
					+ (httpContextId != null ? httpContextId.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return new StringBuffer().append(this.getClass().getSimpleName())
					.append("{").append("bundle=").append(bundle)
					.append(",httpContextId=").append(httpContextId)
					.append("}").toString();
		}

	}

	public void bundleChanged(BundleEvent event) {
		switch (event.getType()) {
		case BundleEvent.STOPPED:
			bundleStopped(event.getBundle());
			break;
		default:
			// nothing
		}
	}

	private void bundleStopped(Bundle bundle) {
		HttpServiceTracker httpServiceTracker = getHttpServiceTrackers()
				.remove(bundle);
		if (httpServiceTracker != null) {
			// there is no need to close tracker because BundleContext is no
			// longer valid
			// httpServiceTracker.close();
		}
		for (ContextKey contextKey : getContextKeys(bundle)) {
			removeWebApplications(contextKey);
		}
	}

	/**
	 * Search web applications by bundle and return associated context keys.
	 * 
	 * @param bundle
	 *            to search contexts for
	 * 
	 * @return set of context keys or an emty set if none found
	 */
	private Collection<ContextKey> getContextKeys(final Bundle bundle) {
		final Collection<ContextKey> keys = new ArrayList<ContextKey>();
		for (ContextKey contextKey : webApplications.keySet()) {
			if (contextKey.bundle.equals(bundle)) {
				keys.add(contextKey);
			}
		}
		return keys;
	}

	public void open(BundleContext bundleContext) {
		bundleContext.addBundleListener(this);
	}

	public void close(BundleContext bundleContext) {
		bundleContext.removeBundleListener(this);
		closeServiceTracker();
	}

	void closeServiceTracker() {
		for (Entry<Bundle, HttpServiceTracker> entry : getHttpServiceTrackers()
				.entrySet()) {
			entry.getValue().close();
		}
		getHttpServiceTrackers().clear();
	}

	ConcurrentHashMap<Bundle, HttpServiceTracker> getHttpServiceTrackers() {
		return httpServiceTrackers;
	}

}
