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

import java.util.List;

import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.ops4j.pax.web.service.spi.model.events.ElementEventData;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.whiteboard.ContextRelated;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Tracks objects published as services, that represent <em>web elements</em> (not <em>contexts</em>) - servlets,
 * filters, listeners, ....</p>
 *
 * <p>The biggest change comparing to Pax Web 7 is that incoming {@link ServiceReference references} are NOT
 * dereferenced immediately. Dereferencing happens when the target element (e.g., {@link javax.servlet.Servlet}) needs
 * to be registered in target runtime - sometimes in one, sometimes in more target servlet contexts. If the service is
 * actually a {@link org.osgi.framework.ServiceFactory} or {@link org.osgi.framework.PrototypeServiceFactory},
 * this is a must.</p>
 *
 * @param <S> as in {@link ServiceTrackerCustomizer} is the type of the <em>incoming</em> service as registered by user
 * @param <R> is the type parameter of {@link ElementModel}. It's separate type, because both
 *        {@link javax.servlet.Servlet} and {@link org.ops4j.pax.web.service.whiteboard.ServletMapping} should be
 *        tracked as {@link org.ops4j.pax.web.service.spi.model.elements.ServletModel}, which is
 *        {@code ElementModel<Servlet>}.
 * @param <D> type of {@link ElementEventData} representing DTO/read-only object carrying information about
 *        {@link ElementModel} being registered.
 * @param <T> as in {@link ServiceTrackerCustomizer} is the type of the actual tracked object (transformed/customized
 *        service) as required by internal Pax Web mechanisms and it should be (in case of <em>web element</em>) an
 *        instance of {@link ElementModel}.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, August 21, 2007
 */
public abstract class AbstractElementTracker<S, R, D extends ElementEventData, T extends ElementModel<R, D>>
		implements ServiceTrackerCustomizer<S, T> {

	private static final String LEGACY_MAPPING_PACKAGE = ContextRelated.class.getPackage().getName();

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final BundleContext bundleContext;
	private final ExtenderContext extenderContext;

	protected AbstractElementTracker(ExtenderContext extenderContext, BundleContext bundleContext) {
		this.extenderContext = extenderContext;
		this.bundleContext = bundleContext;
	}

	/**
	 * Creates a new tracker that tracks services using {@link Constants#OBJECTCLASS} based filter.
	 *
	 * @param trackedClass the classes defining the service types to track. Should not be empty.
	 * @return a configured osgi service tracker
	 */
	@SafeVarargs
	protected final ServiceTracker<S, T> create(final Class<? extends S>... trackedClass) {
		return new ServiceTracker<>(bundleContext, Utils.createFilter(bundleContext, trackedClass), this);
	}

	/**
	 * Creates a new tracker that tracks services using generic {@link Filter}. For these Whiteboard <em>elements</em>
	 * that can't be described using simply a class (like Websockets).
	 *
	 * @param filter generic filter to use for tracker
	 * @return a configured osgi service tracker
	 */
	protected final ServiceTracker<S, T> create(String filter) {
		try {
			return new ServiceTracker<>(bundleContext, bundleContext.createFilter(filter), this);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Problem creating service tracker. Bad filter definition: "
					+ e.getMessage());
		}
	}

	/**
	 * <p>Factory method to create a <em>model</em> for given Whiteboard <em>element</em>.</p>
	 *
	 * <p>The point is to <em>transform</em> incoming {@link ServiceReference} into an object extending
	 * {@link ElementModel} that can then be passed to currently available instance of
	 * {@link org.ops4j.pax.web.service.WebContainer} together with <strong>all</strong> associated <em>contexts</em>,
	 * which may be represented by {@link org.osgi.service.http.context.ServletContextHelper} and/or
	 * {@link org.osgi.service.http.HttpContext}.</p>
	 *
	 * <p>If the registration cannot be created from the published service (e.g. not enough metadata) the register
	 * method should return {@code null}, fact that will cancel the registration of the service. Additionally it can
	 * log an error so the user is notified about the problem.</p>
	 *
	 * <p>In Pax Web 8 we try to keep the service in the form of {@link ServiceReference} as long as possible, because
	 * it may be a {@link org.osgi.framework.PrototypeServiceFactory} to be dereferenced when needed (possibly to
	 * install e.g., a {@link javax.servlet.Servlet} into more than one {@link javax.servlet.ServletContext}).</p>
	 *
	 * @param serviceReference service reference for published service
	 * @param serviceId
	 * @param rank
	 * @return
	 */
	protected abstract T createElementModel(ServiceReference<S> serviceReference, Integer rank, Long serviceId);

	// --- implementation of org.osgi.util.tracker.ServiceTrackerCustomizer

	@Override
	@SuppressWarnings("deprecation")
	public T addingService(final ServiceReference<S> serviceReference) {
		log.debug("Processing new Whiteboard service reference: {}", serviceReference);

		// each "element" should _always_ be associated with some _context_. This association is expressed directly
		// in case of Http Service spec and its
		// org.osgi.service.http.HttpService.registerServlet(alias, servlet, params, _context_) invocation
		//
		// in Whiteboard Service spec, the association is specified in different ways (in decreasing priority):
		//  - "osgi.http.whiteboard.context.select" propery in OSGi CMPN R7 Whiteboard Service that points to one or
		//    more "context" named using "osgi.http.whiteboard.context.name" property (see
		//    "140.3 Common Whiteboard Properties")
		//  - "httpContext.id" property in Pax Web (legacy) Whiteboard that points to _single_ http context
		//  - org.ops4j.pax.web.service.whiteboard.ContextRelated.getContextSelectFilter() - new method for legacy
		//    mappings in Pax Web 8
		//  - org.ops4j.pax.web.service.whiteboard.ContextRelated.getContextId() - old method for legacy Pax Web
		//    mapping
		//  - fallback "(osgi.http.whiteboard.context.name=default)", but only for services OTHER than ones
		//    from org.ops4j.pax.web.service.whiteboard package
		//
		// "shared" flag - that's something Pax Web specific, because initially (with Http Service spec only) nothing
		// was shared and HttpContext passed to register() methods was _always_ related to some bundle
		//  - "httpContext.shared" flag in Pax Web pre 8 could be a part of key that identifies target
		//    "web application" to register given element to. This flag was part of element registration, which
		//    is strage reverse of natural relation, because "shared" should be an attribute of the context, not
		//    of the incoming web element...
		//
		// "140.10 Integration with Http Service Contexts" says explicitly about registering filters/listeners/error
		// pages (not servlets!) to work with servlets registered with HttpService. Such e.g., filter should have
		// "context selection" property in the form of:
		//
		//     "osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.httpservice=*)"
		//
		// which means "[...] targeting a ServletContextHelper with the registration property
		// osgi.http.whiteboard.context.httpservice. The value for this property is not further specified.[...]"
		// This means - targeting as if there was an OSGi service with ServletContextHelper interface registered
		// and "osgi.http.whiteboard.context.httpservice" property. Specification doesn't even require such
		// ServletContextHelper to be really published in the registry!

					//		Boolean sharedHttpContext = ServicePropertiesUtils.extractSharedHttpContext(serviceReference);

		Integer rank = (Integer) serviceReference.getProperty(Constants.SERVICE_RANKING);
		if (rank == null) {
			rank = 0;
		}
		Long serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
		if (serviceId == null) {
			serviceId = 0L;
		}

		// turn a ServiceReference into ElementModel<R> that can be passed to HttpService/WebContainer
		// and contains almost _everything_ needed to process it later (for example after WebContainer becomes available)
		T webElement = createElementModel(serviceReference, rank, serviceId);

		if (webElement == null) {
			log.warn("No element model was created from reference {}", serviceReference);
			return null;
		}

		// Get a filter for target _context(s)_ with which given _element_ is associated
		Object legacyIdProperty = serviceReference.getProperty(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID);
		String legacyId = legacyIdProperty instanceof String ? ((String)legacyIdProperty) : null;
		if (legacyId != null) {
			log.warn("Legacy {} property used, please select context(s) using {} property.",
					PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID,
					HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
		}
		Object selectorProperty = serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
		if (selectorProperty != null && !(selectorProperty instanceof String)) {
			log.warn("{} context selection property is not of type String. Ignoring {}.",
					HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, serviceReference);
			return null;
		}

		// legacy mapping means that user has specified the context ID probably with ContextRelated.getContextId()
		// but still it could have been specified using "httpContext.id" service registration property (higher priority)
		// or even osgi.http.whiteboard.context.select
		boolean legacyMapping = false;
		String[] objectClasses = Utils.getObjectClasses(serviceReference);
		for (String oc : objectClasses) {
			if (oc.startsWith(LEGACY_MAPPING_PACKAGE)) {
				String oc2 = oc.substring(LEGACY_MAPPING_PACKAGE.length());
				if (oc2.startsWith(".") && oc2.indexOf('.', 1) == -1) {
					legacyMapping = true;
					break;
				}
			}
		}

		String selector = determineSelector(legacyMapping, legacyId, (String) selectorProperty, serviceReference);

		Filter contextFilter;
		try {
			contextFilter = bundleContext.createFilter(selector);
		} catch (InvalidSyntaxException e) {
			log.error("Can't register web element from reference {}, skipping registration."
							+ " Bad context selector: {}", serviceReference, selector, e);
			return null;
		}

		// 2. get the actual contexts - only after creating actual element. Because failure to resolve target
		//    contexts should result in specific FailureDTO (e.g., org.osgi.service.http.runtime.dto.FailedServletDTO)
		List<OsgiContextModel> contexts = extenderContext.resolveContexts(serviceReference.getBundle(), contextFilter);

		if (contexts.size() == 0) {
			// TODO: should result in DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING
			log.warn("Can't resolve target context(s) for Whiteboard element {}. Filter: {}", serviceReference, selector);
			return null;
		}

		// now set the target context models
		for (OsgiContextModel contextModel : contexts) {
			webElement.addContextModel(contextModel);
		}

		// Web element is created, but validation has to be run separately/explicitly to handle "Failure DTO"
		if (webElement.isValid()) {
			extenderContext.addWebElement(serviceReference.getBundle(), webElement);

			//		httpServiceRuntime.addWhiteboardElement(model);
			extenderContext.configureDTOs(webElement);
			return webElement;
		} else {
			extenderContext.configureFailedDTOs(webElement);
			return null;
		}
	}

	@Override
	public void modifiedService(ServiceReference<S> reference, T service) {
		log.debug("Processing Whiteboard service reference change: {}", reference);

		// currently registered Whiteboard service had its service properties changed using
		// org.osgi.framework.ServiceRegistration.setProperties()

		// what we have to do is simply unregister existing "model" and create new one based on new properties
		// from existing (changed) reference. The hashCode of reference didn't change

		// why it's important? Imagine change of Servlet's "osgi.http.whiteboard.context.select" property to
		// point to different ServletContextHelper with different "osgi.http.whiteboard.context.path" property - the
		// servlet simply has to be unregistered from e.g.m /context1 context and registered into e.g., /context2

		removedService(reference, service);
		addingService(reference);
	}

	@Override
	public void removedService(final ServiceReference<S> serviceReference, final T webElement) {
		log.debug("Whiteboard service removed: {}", serviceReference);

		//		httpServiceRuntime.removeWhiteboardElement(model);
		extenderContext.removeWebElement(serviceReference.getBundle(), webElement);
	}

	/**
	 * Get a selector for contexts in LDAP-filter syntax.
	 *
	 * @param legacyMapping if {@code true}, then no fallback selector will be created because context id/selector
	 *        SHOULD be specified using {@link ContextRelated#getContextId()} or
	 *        {@link ContextRelated#getContextSelectFilter()}
	 * @param legacyId legacy contextId (specified by {@link PaxWebConstants#SERVICE_PROPERTY_HTTP_CONTEXT_ID})
	 * @param selector new Whiteboard filter (specified usually by
	 *        {@link HttpWhiteboardConstants#HTTP_WHITEBOARD_CONTEXT_SELECT})
	 * @param serviceReference
	 * @return
	 */
	@SuppressWarnings("deprecation")
	protected String determineSelector(boolean legacyMapping, String legacyId, String selector,
			ServiceReference<S> serviceReference) {

		if (selector != null && legacyId != null) {
			log.warn("Both legacy {} and R7 {} properties are specified. Using R7 property: {}.",
					PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID,
					HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, selector);
			legacyId = null;
		}

		if (!legacyMapping && selector == null && legacyId != null) {
			// turn ID into selector
			// "140.10 Integration with Http Service Contexts":
			//
			//    A Http Whiteboard service which should be registered with a Http Context from the Http Service can
			//    achieve this by targeting a ServletContextHelper with the registration property
			//    osgi.http.whiteboard.context.httpservice.
			//    [...]
			//    This specification does not provide a way to select in individual Http Context from the Http Service,
			//    however a Http Whiteboard implementation may provide an implementation-specific mechanism to do this.
			//
			// I'm assuming that if someone registers a servlet (or other element) with httpContext.id, it is
			// exactly the above situation
			// btw, HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_FILTER is:
			//
			//    (osgi.http.whiteboard.context.httpservice=*)
			//
			// while we're doing this implementation-specific mechanism to target particular HttpContext
			selector = String.format("(%s=%s)", HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY, legacyId);
		}

		// I thought I could process annotations like
		// @org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect here, but these have
		// retention=CLASS to be processed by tools, like these related to SCR. So I'll skip it

		// fallback selector - but only if we're tracking object of class DIFFERENT than one from
		// org.ops4j.pax.web.service.whiteboard package, because those classes (related to elements, not contexts)
		// extend org.ops4j.pax.web.service.whiteboard.ContextRelated which specify context ref(s) directly
		if (!legacyMapping && selector == null) {
			selector = String.format("(%s=%s)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
					HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
		}

		return selector;
	}

	/**
	 * Helper method to get a service from a {@link ServiceReference} with tiny validation.
	 * @param reference
	 * @return
	 */
	protected S dereference(ServiceReference<S> reference) {
		S service = bundleContext.getService(reference);
		if (service == null) {
			throw new IllegalArgumentException("Can't create Whiteboard service, can't dereference " + reference);
		}

		return service;
	}

}
