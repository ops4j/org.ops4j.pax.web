/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.spi.servlet;

import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.views.DynamicJEEWebContainerView;
import org.ops4j.pax.web.service.spi.servlet.dynamic.DynamicEventListenerRegistration;
import org.ops4j.pax.web.service.spi.servlet.dynamic.DynamicFilterRegistration;
import org.ops4j.pax.web.service.spi.servlet.dynamic.DynamicServletRegistration;
import org.ops4j.pax.web.service.spi.task.Change;
import org.ops4j.pax.web.service.spi.task.EventListenerModelChange;
import org.ops4j.pax.web.service.spi.task.FilterModelChange;
import org.ops4j.pax.web.service.spi.task.OpCode;
import org.ops4j.pax.web.service.spi.task.ServletModelChange;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * <p>This class may be used by container-specific classes to collect dynamic servlets/filters/listeners
 * being registered by {@link javax.servlet.ServletContainerInitializer}s.</p>
 *
 * <p>Just as dynamic registration methods of {@link javax.servlet.ServletContext}, we don't bother with
 * unregistration of the elements. We will eventually clean things up, but only after given context is somehow
 * destroyed/closed. Registration declarations stored in this class are cleared after invocation of
 * context's {@link javax.servlet.ServletContainerInitializer} ends.</p>
 */
public class DynamicRegistrations {

	// --- maps for dynamic servlet/filter/listener registrations
	//     they're populated when SCIs are invoked and actually used by the last (special) ServletContainerInitializer

	private final Map<String, DynamicServletRegistration> dynamicServletRegistrations = new HashMap<>();
	private final Map<String, DynamicFilterRegistration> dynamicFilterRegistrations = new HashMap<>();
	private final Map<Integer, DynamicEventListenerRegistration> dynamicListenerRegistrations = new HashMap<>();

	// this map allows us to remember the dynamic model used to register a listener, so if there's a need,
	// we can remove such listener from runtime-specific context
	private final Map<EventListener, EventListenerModel> dynamicListenerModels = new IdentityHashMap<>();

	public Map<String, DynamicServletRegistration> getDynamicServletRegistrations() {
		return dynamicServletRegistrations;
	}

	public Collection<DynamicFilterRegistration> getDynamicFilterRegistrations() {
		return dynamicFilterRegistrations.values();
	}

	public Collection<DynamicEventListenerRegistration> getDynamicListenerRegistrations() {
		return dynamicListenerRegistrations.values();
	}

	public Map<EventListener, EventListenerModel> getDynamicListenerModels() {
		return dynamicListenerModels;
	}

// In order to transition from Servlet API to OSGi HttpService/Whiteboard APIs, we need a bundle-scoped
	// instance of WebContainer even if the ServletContainerInitializer that calls these methods is completely
	// unaware of OSGi
	//
	// so we'll grab a reference to WebContainer OSGi service scoped to a bundle of the Filter/Servlet/Listener
	// instance's class' bundle. If the element is specified only using class NAME, we'll use the WebContainer
	// scoped to the bundle of this OsgiServletContext's OsgiContextModel
	//
	// we won't have to care about unregistration, as everything should be done when the registering bundle stops,
	// thanks to this chain of invocations (Felix):
	// org.ops4j.pax.web.service.internal.StoppableHttpServiceFactory.ungetService()
	//   org.apache.felix.framework.ServiceRegistrationImpl.ungetFactoryUnchecked()
	//     org.apache.felix.framework.ServiceRegistrationImpl.ungetService()
	//       org.apache.felix.framework.ServiceRegistry.ungetService()
	//         org.apache.felix.framework.ServiceRegistry.ungetServices()
	//           org.apache.felix.framework.Felix.stopBundle()
	//
	// TOUNGET: we will also not care about ungetService
	//
	// According to javadocs, addXXX methods may return null if a filter/servlet with given name is already
	// registered. On the other hand, org.apache.myfaces.ee6.MyFacesContainerInitializer.onStartup() doesn't
	// check for null, but checks existence of the servlet using javax.servlet.ServletContext.getServletRegistrations()
	// for javax.faces.webapp.FacesServlet class of the servlet
	// to keep Servlet API compliance, we'll check existence by name in ServerModel/FilterModel and return null if
	// a servlet/filter is already registered
	// for servlet mapping (javax.servlet.ServletRegistration.addMapping()), we'll either register the ServletModel
	// or return a set of conflicting mappings (even if Whiteboard specification allows shadowing of the servlets
	// by service ranking rules)

	public FilterRegistration.Dynamic addFilter(OsgiServletContext context, String filterName, String className) {
		OsgiContextModel osgiContextModel = context.getOsgiContextModel();
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Filter> filterClass = (Class<? extends Filter>) osgiContextModel.getOwnerBundle().loadClass(className);

			FilterModel.Builder builder = new FilterModel.Builder()
					.withFilterName(filterName)
					.withFilterClass(filterClass)
					// osgiContextModel from the model of this OsgiServletContext
					.withOsgiContextModel(osgiContextModel);

			return register(context, new DynamicFilterRegistration(builder.build(), osgiContextModel, this));
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Can't load filter class using bundle " + osgiContextModel.getOwnerBundle(), e);
		}
	}

	public FilterRegistration.Dynamic addFilter(OsgiServletContext context, String filterName, Filter filter) {
		OsgiContextModel osgiContextModel = context.getOsgiContextModel();

		FilterModel.Builder builder = new FilterModel.Builder()
				.withFilterName(filterName)
				.withFilter(filter)
				// osgiContextModel from the model of this OsgiServletContext
				.withOsgiContextModel(osgiContextModel);

		return register(context, new DynamicFilterRegistration(builder.build(), osgiContextModel, this));
	}

	public FilterRegistration.Dynamic addFilter(OsgiServletContext context, String filterName, Class<? extends Filter> filterClass) {
		OsgiContextModel osgiContextModel = context.getOsgiContextModel();

		FilterModel.Builder builder = new FilterModel.Builder()
				.withFilterName(filterName)
				.withFilterClass(filterClass)
				// osgiContextModel from the model of this OsgiServletContext
				.withOsgiContextModel(osgiContextModel);

		return register(context, new DynamicFilterRegistration(builder.build(), osgiContextModel, this));
	}

	/**
	 * Prepares and tracks instance of {@link FilterRegistration.Dynamic}, which may be further configured. This
	 * method also checks if given context already contains a filter with this name and returns {@code null} in
	 * this case (as in {@link ServletContext#addFilter(String, Filter)}.
	 *
	 * @param context
	 * @param reg
	 * @return
	 */
	private FilterRegistration.Dynamic register(OsgiServletContext context, DynamicFilterRegistration reg) {
		ServletContextModel scModel = context.getServletContextModel();
		if (scModel.getFilterNameMapping().containsKey(reg.getName())) {
			// according to javax.servlet.ServletContext.addFilter(java.lang.String, ...)
			return null;
		}

		// there's no filter with such name, so there's no conflict. Even if there are disabled filters with such
		// name for given context, we'll be registering new FilterModel with highest priority (Servlet spec over
		// Whiteboard spec...)
		FilterModel model = reg.getModel();
		model.setServiceRank(Integer.MAX_VALUE);
		model.setDynamic(true);
		configureBundle(context, model, reg.getModel().getActualClass());

		// JavaEE doesn't provide a way to unregister filters registered by
		// javax.servlet.ServletContext.addFilter(), but Pax Web does!
		configureUnregistration(context.getOsgiContextModel(), new FilterModelChange(OpCode.DELETE, model));

		dynamicFilterRegistrations.put(reg.getName(), reg);

		// return, so SCI configures it further
		return reg;
	}

	public ServletRegistration.Dynamic addServlet(OsgiServletContext context, String servletName, String className) {
		OsgiContextModel osgiContextModel = context.getOsgiContextModel();
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Servlet> filterClass = (Class<? extends Servlet>) osgiContextModel.getOwnerBundle().loadClass(className);

			ServletModel.Builder builder = new ServletModel.Builder()
					.withServletName(servletName)
					.withServletClass(filterClass)
					// osgiContextModel from the model of this OsgiServletContext
					.withOsgiContextModel(osgiContextModel);

			return register(context, new DynamicServletRegistration(builder.build(), osgiContextModel,
					context.getServletContextModel(), this));
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Can't load servlet class using bundle " + osgiContextModel.getOwnerBundle(), e);
		}
	}

	public ServletRegistration.Dynamic addServlet(OsgiServletContext context, String servletName, Servlet servlet) {
		OsgiContextModel osgiContextModel = context.getOsgiContextModel();

		ServletModel.Builder builder = new ServletModel.Builder()
				.withServletName(servletName)
				.withServlet(servlet)
				// osgiContextModel from the model of this OsgiServletContext
				.withOsgiContextModel(osgiContextModel);

		return register(context, new DynamicServletRegistration(builder.build(), osgiContextModel,
				context.getServletContextModel(), this));
	}

	public ServletRegistration.Dynamic addServlet(OsgiServletContext context, String servletName, Class<? extends Servlet> servletClass) {
		OsgiContextModel osgiContextModel = context.getOsgiContextModel();

		ServletModel.Builder builder = new ServletModel.Builder()
				.withServletName(servletName)
				.withServletClass(servletClass)
				// osgiContextModel from the model of this OsgiServletContext
				.withOsgiContextModel(osgiContextModel);

		return register(context, new DynamicServletRegistration(builder.build(), osgiContextModel,
				context.getServletContextModel(), this));
	}

	/**
	 * Prepares and tracks instance of {@link ServletRegistration.Dynamic}, which may be further configured. This
	 * method also checks if given context already contains a servlet with this name and returns {@code null} in
	 * this case (as in {@link ServletContext#addServlet(String, Servlet)}.
	 *
	 * @param context
	 * @param reg
	 * @return
	 */
	private ServletRegistration.Dynamic register(OsgiServletContext context, DynamicServletRegistration reg) {
		ServletContextModel scModel = context.getServletContextModel();
		if (scModel.getServletNameMapping().containsKey(reg.getName())) {
			// according to javax.servlet.ServletContext.addServlet(java.lang.String, ...)
			return null;
		}

		// there's no servlet with such name, so there's no conflict. Even if there are disabled servlets with such
		// name for given context, we'll be registering new ServletModel with highest priority (Servlet spec over
		// Whiteboard spec...)
		ServletModel model = reg.getModel();
		model.setServiceRank(Integer.MAX_VALUE);
		model.setDynamic(true);
		configureBundle(context, model, reg.getModel().getActualClass());

		// JavaEE doesn't provide a way to unregister servlets registered by
		// javax.servlet.ServletContext.addServlet(), but Pax Web does!
		configureUnregistration(context.getOsgiContextModel(), new ServletModelChange(OpCode.DELETE, model));

		dynamicServletRegistrations.put(reg.getName(), reg);

		// return, so SCI configures it further
		return reg;
	}

	public void addListener(OsgiServletContext context, String className) {
		OsgiContextModel osgiContextModel = context.getOsgiContextModel();
		try {
			@SuppressWarnings("unchecked")
			Class<? extends EventListener> filterClass = (Class<? extends EventListener>) osgiContextModel.getOwnerBundle().loadClass(className);

			EventListenerModel model = new EventListenerModel(filterClass.newInstance());
			model.addContextModel(osgiContextModel);

			register(context, new DynamicEventListenerRegistration(model, osgiContextModel));
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			throw new IllegalArgumentException("Can't load event listener class using bundle " + osgiContextModel.getOwnerBundle(), e);
		}
	}

	public <T extends EventListener> void addListener(OsgiServletContext context, T t) {
		OsgiContextModel osgiContextModel = context.getOsgiContextModel();
		EventListenerModel model = new EventListenerModel(t);
		model.addContextModel(osgiContextModel);

		register(context, new DynamicEventListenerRegistration(model, osgiContextModel));
	}

	public void addListener(OsgiServletContext context, Class<? extends EventListener> listenerClass) {
		OsgiContextModel osgiContextModel = context.getOsgiContextModel();
		try {
			EventListenerModel model = new EventListenerModel(listenerClass.newInstance());
			model.addContextModel(osgiContextModel);

			register(context, new DynamicEventListenerRegistration(model, osgiContextModel));
		} catch (IllegalAccessException | InstantiationException e) {
			throw new IllegalArgumentException("Can't instantiate event listener of class " + listenerClass, e);
		}
	}

	/**
	 * Tracks {@link DynamicEventListenerRegistration} for future registration
	 * @param context
	 * @param reg
	 */
	private void register(OsgiServletContext context, DynamicEventListenerRegistration reg) {
		EventListenerModel model = reg.getModel();

		// we should never allow installation of ServletContextListeners this way
		if (!context.acceptsServletContextListeners()) {
			if (model.getEventListener() instanceof ServletContextListener) {
				String message = "Section 4.4.3 of the Servlets specification allows ServletContextListeners" +
						" to be added only by ServletContainerInitializers, declared in web.xml or web-fragment.xml or" +
						" by discovery of @WebListener annotated classes";
				throw new UnsupportedOperationException(message);
			}
		}

		model.setServiceRank(0);
		model.setDynamic(true);
		configureBundle(context, model, reg.getModel().getEventListener().getClass());

		// JavaEE doesn't provide a way to unregister listeners registered by
		// javax.servlet.ServletContext.addListener(), but Pax Web does!
		configureUnregistration(context.getOsgiContextModel(), new EventListenerModelChange(OpCode.DELETE, model));

		dynamicListenerRegistrations.put(System.identityHashCode(model.getEventListener()), reg);

		// remember the model, so we can unregister it later when we have to restart the context
		dynamicListenerModels.put(model.getEventListener(), model);
	}

	private void configureBundle(OsgiServletContext context, ElementModel<?, ?> model, Class<?> aClass) {
		Bundle bundle = FrameworkUtil.getBundle(aClass);
		if (bundle == null || bundle.getBundleContext() == null) {
			// I was able to install (but not start) myfaces-impl bundle, but I had problems
			// obtaining WebContainer instance within the context of this not-started bundle. I think
			// it's safe to have the bundle to be the WAB in such case.
			bundle = context.getOsgiContextModel().getOwnerBundle();
		}
		// this is important because the registering bundle is used to obtain bundle-scoped WebContainer instance
		// later - during actual registration of the model
		model.setRegisteringBundle(bundle);
	}

	/**
	 * This methods takes an {@link org.ops4j.pax.web.service.spi.task.Change undeployment change} related
	 * to dynamic web element registration. Normally such elements are unregistered when bundle-scoped
	 * {@link WebContainer} service is {@link BundleContext#ungetService unget}, but in case of WABs, it's done
	 * in a batch, so we need the dynamic unregistrations earlier - before entire contexts is removed.
	 *
	 * @param osgiContextModel
	 * @param unregistration
	 */
	private void configureUnregistration(OsgiContextModel osgiContextModel, Change unregistration) {
		osgiContextModel.addUnregistrationChange(unregistration);
	}

	/**
	 * Gets a bundle-scoped instance of {@link WebContainer} for dynamic registration of servlets/filters/listeners
	 * in not-yet-started {@link ServletContext}
	 * @param clazz
	 * @return
	 */
	public DynamicJEEWebContainerView getContainer(OsgiContextModel osgiContextModel, Class<?> clazz) {
		Bundle bundle = clazz == null ? null : FrameworkUtil.getBundle(clazz);
		if (bundle == null) {
			bundle = osgiContextModel.getOwnerBundle();
		}

		if (bundle == null) {
			throw new IllegalStateException("Can't obtain WebContainer instance. Dynamic registration not possible.");
		}

		return getContainer(bundle);
	}

	public DynamicJEEWebContainerView getContainer(Bundle bundle) {
		if (bundle == null || bundle.getBundleContext() == null) {
			return null;
		}

		BundleContext bc = bundle.getBundleContext();
		ServiceReference<WebContainer> ref = bc.getServiceReference(WebContainer.class);
		if (ref == null) {
			// can be null in low level, non-OSGi tests
			return null;
		}

		// TOUNGET:
		WebContainer container = bc.getService(ref);
		if (container == null) {
			throw new IllegalStateException("Can't obtain WebContainer instance from " + ref + " reference. Dynamic registration not possible.");
		}

		return container.adapt(DynamicJEEWebContainerView.class);
	}

}
