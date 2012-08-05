package org.ops4j.pax.web.extender.war.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.ops4j.pax.swissbox.tracker.ReplaceableServiceListener;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

public class DefaultWebAppDependencyManager implements ReplaceableServiceListener<HttpService> {

	private BundleContext bundleContext;
	private Map<Long, ServiceRegistration> registrations = new HashMap<Long, ServiceRegistration>();
	private Map<Long, WebApp> webApps = new HashMap<Long, WebApp>();
	private HttpService httpService;

	
	public DefaultWebAppDependencyManager(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}
	
	@Override
	public synchronized void serviceChanged(HttpService oldService, HttpService newService) {
		for (ServiceRegistration registration : registrations.values()) {
			registration.unregister();
		}
		httpService = newService;
		for (long bundleId : webApps.keySet()) {
			register(bundleId);
		}
	}

	private void register(long bundleId) {
		if (httpService != null) {

			HttpService webAppHttpService = getProxiedHttpService(bundleId);
			WebAppDependencyHolder dependencyHolder = new DefaultWebAppDependencyHolder(webAppHttpService);
			Dictionary<String, String> props = new Hashtable<String, String>();
			props.put("bundle.id", Long.toString(bundleId));
			ServiceRegistration registration = bundleContext.registerService(
				WebAppDependencyHolder.class.getName(), dependencyHolder, props);
			registrations.put(bundleId, registration);
		}
	}

	/**
	 * The HTTP Service is proxied per web app (TODO why?) - see {@link HttpServiceFactory} and
	 * its use in the pax-web-runtime Activator. Since the proxied service also wraps the 
	 * referencing bundle, we make sure to obtain the correct proxy via the bundle context
	 * of the extended web bundle instead of using our own {@code httpService} member which
	 * is registered to the extender bundle.
	 * 
	 * @param bundleId  bundle ID of extended web bundle
	 * @return
	 */
	private HttpService getProxiedHttpService(long bundleId) {
		Bundle webAppBundle = bundleContext.getBundle(bundleId);
		BundleContext webAppBundleContext = webAppBundle.getBundleContext();
		ServiceReference httpServiceRef = webAppBundleContext.getServiceReference(HttpService.class.getName());
		HttpService webAppHttpService = (HttpService) webAppBundleContext.getService(httpServiceRef);
		return webAppHttpService;
	}

	private void unregister(long bundleId) {
		ServiceRegistration registration = registrations.get(bundleId);
		if (registration != null) {
			registration.unregister();
		}
	}

	public synchronized void addWebApp(WebApp webApp) {
		long bundleId= webApp.getBundle().getBundleId();
		webApps.put(bundleId, webApp);
		register(bundleId);
	}

	public synchronized void removeWebApp(WebApp webApp) {
		long bundleId= webApp.getBundle().getBundleId();
		unregister(bundleId);
		webApps.remove(bundleId);
	}

}
