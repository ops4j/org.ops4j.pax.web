/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import java.util.Hashtable;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.whiteboard.ContextMapping;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Tracks objects published as services, that represent <em>contexts</em> (not <em>elements</em>). These are:<ul>
 *     <li>{@link org.osgi.service.http.context.ServletContextHelper} - the OSGi CMPN R7 Whiteboard</li>
 *     <li>{@link org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping} - the Pax Web approach</li>
 *     <li>{@link HttpContextMapping} - the Pax Web approach for Http Service</li>
 *     <li>{@link org.osgi.service.http.HttpContext} ... - for completeness</li>
 * </ul></p>
 *
 * <p>The <em>customized</em> object is always {@link OsgiContextModel}</p>
 *
 * @param <S> as in {@link ServiceTrackerCustomizer}, the type of the <em>incoming</em> service as registered by user
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, August 21, 2007
 */
public abstract class AbstractContextTracker<S> implements ServiceTrackerCustomizer<S, OsgiContextModel> {

	protected static final Logger LOG = LoggerFactory.getLogger(AbstractElementTracker.class);

	protected final BundleContext bundleContext;
	private final WhiteboardExtenderContext whiteboardExtenderContext;

	protected AbstractContextTracker(WhiteboardExtenderContext whiteboardExtenderContext, BundleContext bundleContext) {
		this.whiteboardExtenderContext = whiteboardExtenderContext;
		this.bundleContext = bundleContext;
	}

	protected final ServiceTracker<S, OsgiContextModel> create(final Class<? extends S> trackedClass) {
		return new ServiceTracker<>(bundleContext, Utils.createFilter(bundleContext, trackedClass), this);
	}

	/**
	 * <p>When user registers <em>something</em> that should represent a <em>context</em> - to configure things
	 * like context path or context parameters, we have to translate (<em>customize</em>) it into
	 * {@link OsgiContextModel} which can be passed further - down to actual server runtime implementation.</p>
	 *
	 * <p>Validation will be performed later in {@link OsgiContextModel#isValid()} - for all kinds of registered
	 * services. Validation will be done in the same thread, so user will have immediate feedback.</p>
	 *
	 * <p>This abstract method should be implemented to configure prepared {@link OsgiContextModel}.</p>
	 *
	 * @param serviceReference
	 * @param model
	 */
	protected abstract void configureContextModel(ServiceReference<S> serviceReference, OsgiContextModel model);

	/**
	 * When the service is unregistered, there may be cases, where particular context tracker has to do some cleanup
	 * @param serviceReference
	 * @param unpublished
	 */
	protected void cleanupContextModel(ServiceReference<S> serviceReference, OsgiContextModel unpublished) {
	}

	// --- implementation of org.osgi.util.tracker.ServiceTrackerCustomizer

	/**
	 * @see ServiceTracker#addingService(ServiceReference)
	 */
	@Override
	public OsgiContextModel addingService(final ServiceReference<S> serviceReference) {
		if (skipInternalService(serviceReference)) {
			return null;
		}

		LOG.debug("Processing new Whiteboard context reference: {}", serviceReference);

		// In Pax Web 7, "shared" flag was taken from httpContext.shared property, but was verified against
		// actual class of the service.
		// now we're doing the same, but at org.ops4j.pax.web.service.spi.model.OsgiContextModel.resolveHttpContext()
		// stage. Returned WebContainerContext has proper implementation of isShared() method

		Integer rank = (Integer) serviceReference.getProperty(Constants.SERVICE_RANKING);
		if (rank == null) {
			rank = 0;
		}
		Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
		if (serviceId == null) {
			serviceId = 0L;
		}
		OsgiContextModel model = new OsgiContextModel(serviceReference.getBundle(), rank, serviceId, true);
		model.setAsynchronusRegistration(true);

		LOG.debug("Configuring OSGi context model from Whiteboard service {} (id={})", serviceReference, serviceId);

		// the most important thing is - how WebContainerContext will be created within configured OsgiContextModel
		//
		// we support 4 service types to be _customized_ into OsgiContextModel:
		// 1. org.osgi.service.http.HttpContext - standard interface, but not designed to be whiteboarded.
		//    Handled through Whiteboard for backward compatibility
		// 2. org.osgi.service.http.context.ServletContextHelper - the standard from OSGi CMPN R7 Whiteboard spec
		// 3. org.ops4j.pax.web.service.whiteboard.HttpContextMapping - the legacy way to handle legacy _context_
		// 4. org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping - the legacy way to handle
		//    OSGi CMPN R7 Whiteboard context
		//
		// Here are the rules:
		//  - when registering a "mapping" (#3 and #4), we could not care if the service is
		//    org.osgi.framework.ServiceFactory, but we do. Even if initial design for "mapping" case was
		//    to register an object that can simply return HttpContext or ServletContextHelper - as singleton
		//    or new instance. So we can have "double factory" case here. Interesting...
		//  - for "mapping" we don't call HttpContextMapping.getHttpContext() or
		//    ServletContextHelperMapping.getServletContextHelper() immediately, but wrap it inside supplier.
		//    It's because the returned value may be bundle-related (the provider method accepts a Bundle).
		//    The supplier also dereferences the service reference in case it's a service factory
		//  - when registering #1 or #2, we should handle org.osgi.framework.ServiceFactory, so we never
		//    dereference the service, but pass the ServiceReference to OsgiContextModel
		//  - in #1 and #2, to get a service from the reference, we need a bundle context for the actual
		//    Whiteboard service used (e.g., from a javax.servlet.Servlet)
		//  - default implementation (which should always be available - unless overriden by same-named context
		//    with higher rank) of ServletContextHelper must use proper bundle to implement methods like
		//    getResource() or getResourcePaths(). Because custom version may do the same, it should always
		//    be registered as ServiceFactory

		configureContextModel(serviceReference, model);

		// Web context is configured, but validation has to be run separately/explicitly to handle "Failure DTO"
		if (model.isValid()) {
			// the successful DTO information will be propagated to HttpServiceRuntime (which is the ServerModel)
			// during registration of the web context
			whiteboardExtenderContext.addWebContext(serviceReference.getBundle(), model);
			return model;
		} else {
			// the failed DTO information have to be passed directly, because we're not registering the web context
			// model. Such failure DTO is never updated, instead its removed and added again, when for example
			// the service registration properties change
			whiteboardExtenderContext.configureFailedDTOs(model);
			return null;
		}
	}

	@Override
	public void modifiedService(ServiceReference<S> reference, OsgiContextModel model) {
		if (skipInternalService(reference)) {
			return;
		}

		LOG.debug("Processing Whiteboard context reference change: {}", reference);

		removedService(reference, model);

		// no service ID change, but some other properties actually MAY change
		Integer rank = (Integer) reference.getProperty(Constants.SERVICE_RANKING);
		if (rank == null) {
			rank = 0;
		}
		model.setServiceRank(rank);

		// reconfiguration of EXISTING OsgiContextModel
		configureContextModel(reference, model);

		// Web context is configured, but validation has to be run separately/explicitly to handle "Failure DTO"
		if (model.isValid()) {
			whiteboardExtenderContext.addWebContext(reference.getBundle(), model);
		} else {
			whiteboardExtenderContext.configureFailedDTOs(model);
		}
	}

	@Override
	public void removedService(final ServiceReference<S> serviceReference, final OsgiContextModel unpublished) {
		if (skipInternalService(serviceReference)) {
			return;
		}

		LOG.debug("Whiteboard context removed: {}", serviceReference);

		whiteboardExtenderContext.removeWebContext(serviceReference.getBundle(), unpublished);

		cleanupContextModel(serviceReference, unpublished);
	}

	private boolean skipInternalService(ServiceReference<S> serviceReference) {
		return Utils.getBooleanProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_INTERNAL);
	}

	/**
	 * If the <em>context</em> was registered from "legacy" services ({@link HttpContextMapping} or
	 * {@link org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping}), then context name, path and
	 * init parameters are passed directly through {@link ContextMapping} interface, not as service registration
	 * properties. This method creates these properties from actual service's object.
	 *
	 * @param model
	 * @param mapping
	 * @param whiteboard whether OSGi CMPN Whiteboard or legacy whiteboar (pax-web) properties should be added
	 */
	@SuppressWarnings("deprecation")
	protected void setupArtificialServiceRegistrationProperties(OsgiContextModel model, ContextMapping mapping,
			boolean whiteboard) {
		final Hashtable<String, Object> registration = model.getContextRegistrationProperties();
		String contextId = mapping.getContextId();
		if (contextId == null) {
			contextId = HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
		}
		String contextPath = mapping.getContextPath();
		if (contextPath == null) {
			contextPath = PaxWebConstants.DEFAULT_CONTEXT_PATH;
		}
		if (whiteboard) {
			registration.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextId);
			registration.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);
		} else {
			registration.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, contextId);
			registration.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, contextPath);
		}
		if (mapping.getInitParameters() != null) {
			mapping.getInitParameters().forEach((k, v)
					-> registration.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX + k, v));
		}
	}

	protected String setupName(OsgiContextModel model, ContextMapping mapping) {
		String name = mapping.getContextId();
		if (name == null || "".equals(name.trim())) {
			name = HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
		}
		model.setName(name);
		return name;
	}

	protected String setupContextPath(OsgiContextModel model, ContextMapping mapping) {
		String contextPath = mapping.getContextPath();
		if (contextPath == null || "".equals(contextPath.trim())) {
			contextPath = PaxWebConstants.DEFAULT_CONTEXT_PATH;
		}
		model.setContextPath(contextPath);
		return contextPath;
	}

}
