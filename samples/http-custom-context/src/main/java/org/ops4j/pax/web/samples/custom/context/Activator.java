package org.ops4j.pax.web.samples.custom.context;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private Logger logger = Logger.getLogger(getClass().getName());

	private HttpService httpService;
	private ServiceTracker<HttpService, HttpService> httpServiceTracker;

	@Override
	public void start(BundleContext context) {
		logger.info("starting Jersey TestActivator");
		httpServiceTracker = new ServiceTracker<HttpService, HttpService>(context, HttpService.class, null) {

			@Override
			public HttpService addingService(ServiceReference<HttpService> serviceRef) {
				logger.info("registering servlet");
				httpService = super.addingService(serviceRef);
				
				HttpContext httpContext = new CustomHttpContext(context.getBundle());
				
				registerServlet(httpContext);
				registerResources(httpContext);
				return httpService;
			}

			@Override
			public void removedService(ServiceReference<HttpService> ref, HttpService service) {
				super.removedService(ref, service);
				httpService = null;
			}
		};
		httpServiceTracker.open();
	}

	@Override
	public void stop(BundleContext context) {
		httpServiceTracker.close();
	}
	
	private void registerResources(HttpContext httpContext) {
		try {
			httpService.registerResources("/images/", "/", null);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Registering resources failed", e);
		}
	}
	
	private void registerServlet(HttpContext httpContext) {
		try {
			httpService.registerServlet("/", new HelloServlet(), null, httpContext);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Registering Jersey servlet failed", e);
		}
	}
}
