package org.ops4j.pax.web.samples.authentication.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

/**
 * Extension of the default OSGi bundle activator
 */
public final class Activator implements BundleActivator {

	private ServiceReference<HttpService> httpServiceRef;
	private HttpService httpService;

	/**
	 * Called whenever the OSGi framework starts our bundle
	 */
	public void start(BundleContext bc) throws Exception {
		httpServiceRef = bc.getServiceReference(HttpService.class);
		if (httpServiceRef != null) {
			httpService = (HttpService) bc.getService(httpServiceRef);
			httpService.registerServlet("/status", new StatusServlet(), null,
					null);
			httpService.registerServlet("/status-with-auth",
					new StatusServlet(), null, new AuthHttpContext());
		}
	}

	/**
	 * Called whenever the OSGi framework stops our bundle
	 */
	public void stop(BundleContext bc) throws Exception {
		if (httpService != null) {
			bc.ungetService(httpServiceRef);
			httpServiceRef = null;
			httpService = null;
		}
	}
}
