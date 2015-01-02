package org.ops4j.pax.web.itest.base.support;

import java.util.Hashtable;

import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;

public class ServletBundleActivator implements BundleActivator {

	private ServiceRegistration<Servlet> servletReg;
	private ServiceRegistration<HttpContext> httpContextReg;

	@Override
	public void start(BundleContext context) throws Exception {

		ServiceReference<WebContainer> serviceReference = context
				.getServiceReference(WebContainer.class);

		while (serviceReference == null) {
			serviceReference = context.getServiceReference(WebContainer.class);
		}

		WebContainer service = (WebContainer) context
				.getService(serviceReference);

		HttpContext httpContext = service.getDefaultSharedHttpContext();

		// register a custom http context that forbids access
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED, "true");
		httpContextReg = context.registerService(HttpContext.class,
				httpContext, props);

		props = new Hashtable<String, String>();
		props.put("alias", "/sharedContext");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED, "true");
		servletReg = context.registerService(Servlet.class, new TestServlet(),
				props);

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (servletReg != null) {
			servletReg.unregister();
		}
		if (httpContextReg != null) {
			httpContextReg.unregister();
		}
	}

}
