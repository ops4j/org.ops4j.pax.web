package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

	private ServiceRegistration<HttpContextMapping> httpContextMappingReg;
	private ServiceRegistration<HttpContextMapping> httpContextMappingReg2;
	private ServiceRegistration<HttpContextMapping> httpContextMappingReg3;
	private ServiceRegistration<Servlet> servletReg;
	private ServiceRegistration<Servlet> servletReg2;
	private ServiceRegistration<Servlet> servletReg3;

	public void start(final BundleContext bundleContext) throws Exception {
		Dictionary<String, String> props;

		// register the first context
		props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended");
		HashMap<String, String> contextMappingParams = new HashMap<String, String>();
		// contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS,
		// "localhost");
		contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_CONNECTORS,
				"jettyConn1");
		httpContextMappingReg = bundleContext.registerService(
				HttpContextMapping.class, new WhiteboardHttpContextMapping(
						"extended", "foo", contextMappingParams), props);

		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended2");
		// contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS,
		// "127.0.0.1");
		contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_CONNECTORS,
				"default");
		httpContextMappingReg = bundleContext.registerService(
				HttpContextMapping.class, new WhiteboardHttpContextMapping(
						"extended2", "bar", contextMappingParams), props);

		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended3");
		// contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_VIRTUAL_HOSTS,
		// "127.0.0.1");
		contextMappingParams.put(ExtenderConstants.PROPERTY_HTTP_CONNECTORS,
				"jettyConn1");
		httpContextMappingReg = bundleContext.registerService(
				HttpContextMapping.class, new WhiteboardHttpContextMapping(
						"extended3", null, contextMappingParams), props);

		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended4");
		contextMappingParams.clear();
		httpContextMappingReg = bundleContext.registerService(
				HttpContextMapping.class, new WhiteboardHttpContextMapping(
						"extended4", "default", contextMappingParams), props);

		props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_ALIAS, "/whiteboard");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended");
		servletReg = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/whiteboard"), props);

		props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_ALIAS, "/whiteboard2");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended2");
		servletReg = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/whiteboard2"), props);

		props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_ALIAS, "/whiteboard3");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended3");
		servletReg = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/whiteboard3"), props);

		props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_ALIAS, "/whiteboard4");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "extended4");
		servletReg = bundleContext.registerService(Servlet.class,
				new WhiteboardServlet("/whiteboard4"), props);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		if (servletReg != null) {
			servletReg.unregister();
			servletReg = null;
		}
		if (servletReg2 != null) {
			servletReg2.unregister();
			servletReg2 = null;
		}
		if (servletReg3 != null) {
			servletReg3.unregister();
			servletReg3 = null;
		}
		if (httpContextMappingReg != null) {
			httpContextMappingReg.unregister();
			httpContextMappingReg = null;
		}
		if (httpContextMappingReg2 != null) {
			httpContextMappingReg2.unregister();
			httpContextMappingReg2 = null;
		}
		if (httpContextMappingReg3 != null) {
			httpContextMappingReg3.unregister();
			httpContextMappingReg3 = null;
		}
	}
}
