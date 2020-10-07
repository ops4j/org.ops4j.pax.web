/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.util.Dictionary;
import java.util.EventListener;
import java.util.Hashtable;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultErrorPageMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultJspMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultResourceMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultWelcomeFileMapping;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.service.whiteboard.JspMapping;
import org.ops4j.pax.web.service.whiteboard.ResourceMapping;
import org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private ServiceRegistration<?> rootServletReg;
	private ServiceRegistration<Servlet> servletReg;
	private ServiceRegistration<Servlet> servlet1FilteredReg;
	private ServiceRegistration<Servlet> servlet2FilteredReg;
	private ServiceRegistration<Servlet> forbiddenServletReg;
	private ServiceRegistration<HttpServlet> exceptionServletRegistration;

	private ServiceRegistration<ResourceMapping> resourcesReg;
	private ServiceRegistration<ResourceMapping> rootResourceMappingRegistration;

	private ServiceRegistration<Filter> filter1Reg;
	private ServiceRegistration<Filter> filter2Reg;
	private ServiceRegistration<Filter> filter3Reg;

	private ServiceRegistration<EventListener> listenerReg;

	private ServiceRegistration<HttpContext> httpContextReg;

	private ServiceRegistration<WelcomeFileMapping> welcomeFileRegistration;

	private ServiceRegistration<ErrorPageMapping> errorpage404Registration;
	private ServiceRegistration<ErrorPageMapping> uncaughtExceptionRegistration;

	private ServiceRegistration<JspMapping> jspMappingRegistration;

	@SuppressWarnings("deprecation")
	public void start(final BundleContext bundleContext) throws Exception {
		Dictionary<String, Object> props;

		// register a custom http context that forbids access
		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "forbidden");
		// we don't want "forbidden" context to "take over" "/" context - it'll still be associated
		// with servlets where referenced explicitly, but the JSPs registered further could
		// register Jasper SCI to wrong context
		props.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
		httpContextReg = bundleContext.registerService(HttpContext.class, new WhiteboardContext(), props);

		// register hello jsp first, because it'll lead to registration of Jasper SCI
		DefaultJspMapping jspMapping = new DefaultJspMapping();
		jspMapping.setUrlPatterns(new String[] { "/jsp/*" });
		jspMappingRegistration = bundleContext.registerService(JspMapping.class, jspMapping, null);

		// and an servlet that cannot be accessed due to the above context
		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS, "/forbidden");
		props.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "forbidden");
		// legacy way to specify the name - it MUST be unique and it defaults to FQCN of the servlet
		props.put(PaxWebConstants.INIT_PARAM_SERVLET_NAME, "forbidden-servlet");
		forbiddenServletReg = bundleContext.registerService(Servlet.class, new WhiteboardServlet("/forbidden"), props);

		// first make sure all mappings are registered, servlets aren't notified of updates since jetty 9.3.5 upgrade

		// register welcome page - interesting how it will work with the root servlet, i.e. will it showdow it
		DefaultWelcomeFileMapping welcomeFileMapping = new DefaultWelcomeFileMapping();
		welcomeFileMapping.setRedirect(true);
		welcomeFileMapping.setWelcomeFiles(new String[] { "index.html", "welcome.html" });
		welcomeFileRegistration = bundleContext.registerService(WelcomeFileMapping.class, welcomeFileMapping, null);

		// register error pages for 404 and java.lang.Exception
		DefaultErrorPageMapping errorpageMapping = new DefaultErrorPageMapping();
		errorpageMapping.setErrors(new String[] { "404" });
		errorpageMapping.setLocation("/404.html");
		errorpage404Registration = bundleContext.registerService(ErrorPageMapping.class, errorpageMapping, null);

		// java.lang.Exception
		DefaultErrorPageMapping exceptionErrorMapping = new DefaultErrorPageMapping();
		exceptionErrorMapping.setErrors(new String[] { java.lang.Exception.class.getName() });
		exceptionErrorMapping.setLocation("/uncaughtException.html");
		uncaughtExceptionRegistration = bundleContext.registerService(ErrorPageMapping.class, exceptionErrorMapping, null);

		// Properties for the service
		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS, "/whiteboard");
		// R6+ way to specify the name - it MUST be unique and it defaults to FQCN of the servlet
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "whiteboard-servlet");
		// registering the servlet as service
		servletReg = bundleContext.registerService(Servlet.class, new WhiteboardServlet("/whiteboard"), props);

		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS, "/root");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "root-servlet");
		rootServletReg = bundleContext.registerService(Servlet.class, new WhiteboardServlet("/root"), props);

		// Registering resource mappings as service
		DefaultResourceMapping resourceMapping = new DefaultResourceMapping();
		resourceMapping.setAlias("/whiteboardresources");
		resourceMapping.setPath("/images");
		resourcesReg = bundleContext.registerService(ResourceMapping.class, resourceMapping, null);

		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS, "/filtered");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "filtered-servlet");
		servlet1FilteredReg = bundleContext.registerService(Servlet.class, new WhiteboardServlet("/filtered"), props);

		// register a filter
		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_URL_PATTERNS, "/filtered/*");
		// filter names also default to FQCN and must me uniqe
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "filter1");
		filter1Reg = bundleContext.registerService(Filter.class, new WhiteboardFilter(), props);

		//registering servlet and two filters on one URL
		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS, "/second");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "second-servlet");
		servlet2FilteredReg = bundleContext.registerService(Servlet.class, new WhiteboardServlet("/second"), props);

		// register a filter
		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_URL_PATTERNS, "/second/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "filter2");
		filter2Reg = bundleContext.registerService(Filter.class, new WhiteboardFilter(), props);

		// register second filter
		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_URL_PATTERNS, "/second/*");
		filter3Reg = bundleContext.registerService(Filter.class, new SecondWhiteboardFilter(), props);

		// register a servlet request listener
		listenerReg = bundleContext.registerService(EventListener.class, new WhiteboardListener(), null);

		// servlet to test exceptions and error pages
		props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS, "/exception");
		exceptionServletRegistration = bundleContext.registerService(HttpServlet.class, new ExceptionServlet(), props);

		// register resource at root of bundle
		DefaultResourceMapping rootResourceMapping = new DefaultResourceMapping();
		rootResourceMapping.setAlias("/");
		rootResourceMapping.setPath("");
		rootResourceMappingRegistration = bundleContext.registerService(ResourceMapping.class, rootResourceMapping, null);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		if (rootResourceMappingRegistration != null) {
			rootResourceMappingRegistration.unregister();
			rootResourceMappingRegistration = null;
		}
		if (uncaughtExceptionRegistration != null) {
			uncaughtExceptionRegistration.unregister();
			uncaughtExceptionRegistration = null;
		}
		if (errorpage404Registration != null) {
			errorpage404Registration.unregister();
			errorpage404Registration = null;
		}
		if (welcomeFileRegistration != null) {
			welcomeFileRegistration.unregister();
			welcomeFileRegistration = null;
		}
		if (rootServletReg != null) {
			rootServletReg.unregister();
			rootServletReg = null;
		}
		if (servletReg != null) {
			servletReg.unregister();
			servletReg = null;
		}
		if (servlet1FilteredReg != null) {
			servlet1FilteredReg.unregister();
			servlet1FilteredReg = null;
		}
		if (servlet2FilteredReg != null) {
			servlet2FilteredReg.unregister();
			servlet2FilteredReg = null;
		}
		if (exceptionServletRegistration != null) {
			exceptionServletRegistration.unregister();
			exceptionServletRegistration = null;
		}
		if (resourcesReg != null) {
			resourcesReg.unregister();
			resourcesReg = null;
		}
		if (filter1Reg != null) {
			filter1Reg.unregister();
			filter1Reg = null;
		}
		if (filter2Reg != null) {
			filter2Reg.unregister();
			filter2Reg = null;
		}
		if (filter3Reg != null) {
			filter3Reg.unregister();
			filter3Reg = null;
		}
		if (listenerReg != null) {
			listenerReg.unregister();
			listenerReg = null;
		}
		if (forbiddenServletReg != null) {
			forbiddenServletReg.unregister();
			forbiddenServletReg = null;
		}
		if (jspMappingRegistration != null) {
			jspMappingRegistration.unregister();
			jspMappingRegistration = null;
		}
		if (httpContextReg != null) {
			httpContextReg.unregister();
			httpContextReg = null;
		}
	}

}
