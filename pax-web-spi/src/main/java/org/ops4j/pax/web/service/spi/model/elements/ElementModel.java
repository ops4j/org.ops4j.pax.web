/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.spi.model.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.ops4j.pax.web.service.spi.model.Identity;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventData;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.ops4j.pax.web.service.whiteboard.ContextRelated;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
 * <p>Base class for all <em>models</em> representing actual elements of a <em>web application</em> (or
 * <em>context</em>), the most obvious <em>model</em> is representing {@link javax.servlet.Servlet}.</p>
 *
 * <p>What user does (through {@link org.osgi.service.http.HttpService} or by publishing Whiteboard services) is
 * a <em>registration</em> of web elements (like servlets) always within the context of ... well, a context
 * (possibly many, as permitted by Whiteboard Service specification).</p>
 *
 * <p>A <em>context</em> is not an element itself (thus - not a <em>model</em>), it's rather:<ul>
 *     <li>actual {@link javax.servlet.ServletContext} from server point of view</li>
 *     <li>OSGi abstraction ({@link org.osgi.service.http.HttpContext} or
 *     {@link org.osgi.service.http.context.ServletContextHelper}) providing additional/bridged functionality,
 *     when delegating some of it to actual {@link javax.servlet.ServletContext}</li>
 * </ul></p>
 *
 * <p>Each <em>element</em>, when registered through Whiteboard Service, may turn out to unregister some existing
 * <em>element</em> when it uses conflicting URL mapping but has lower ranking or service id. Such conflicts lead
 * to trivial {@link org.osgi.service.http.NamespaceException} when using Http Service.</p>
 *
 * @param <T> type of the service that user registers to be processed by Whiteboard extender (whether standard
 *            OSGi CMPN Whiteboard or Pax Web specific Whiteboard extender)
 * @param <D> type of the DTO-like object that carries registration information of real service - to be able
 *            to get notified about (un)registration without the way to change actual registration data
 */
public abstract class ElementModel<T, D extends WebElementEventData>
		extends Identity implements Comparable<ElementModel<T, D>> {

	/**
	 * List of {@link OsgiContextModel osgi contexts} with which given {@link ElementModel} is associated.
	 * This list may be altered using {@link #addContextModel(OsgiContextModel)}, but on first invocation of
	 * {@link #getContextModels()}, this list is <em>latched</em>, sorted and can't be modified any further.
	 */
	protected List<OsgiContextModel> contextModels = new ArrayList<>();
	protected String contextModelsInfo;

	protected Boolean isValid;

	private volatile boolean closed = false;

	private int serviceRank = 0;
	private long serviceId = 0;

	/** Timestamp at which given element was created/updated */
	private long timestamp = 0;

	/**
	 * When an element is registered as Whiteboard service, we have to keep the reference here, so we can
	 * use {@link ServiceReference#getBundle() a bundle} and its context to obtain a reference to
	 * {@link org.osgi.service.http.context.ServletContextHelper} associated with the element.
	 */
	private ServiceReference<T> elementReference;

	/**
	 * Flag indicating whether a model that uses {@link ServiceReference} has {@code prototype} scope.
	 */
	private boolean prototype = false;

	/**
	 * Because user may specify Whiteboard service (e.g., {@link javax.servlet.Servlet}) using <em>legacy</em> service
	 * like {@link org.ops4j.pax.web.service.whiteboard.ServletMapping} we can't assume if method returning this
	 * servlet returns a singleton or an instance on each call. So we wrap such
	 * {@link org.ops4j.pax.web.service.whiteboard.ServletMapping} inside this {@link Supplier} to delay
	 * servlet creation to the moment when it's really needed.
	 *
	 * @param <T>
	 */
	private Supplier<? extends T> elementSupplier;

	/**
	 * Even if the element is not registered as Whiteboard service, we still need a bundle in which scope
	 * the element was registered (for example using
	 * {@link org.osgi.service.http.HttpService#registerServlet}), so
	 * we can use its {@link org.osgi.framework.BundleContext} to obtain proper reference to
	 * {@link org.osgi.service.http.context.ServletContextHelper} if needed.
	 */
	private Bundle registeringBundle;

	/**
	 * {@link Filter} to select associated {@link OsgiContextModel}s.
	 */
	private Filter contextFilter;

	/**
	 * We can set the context selector LDAP expression, so it doesn't have to be resolved from e.g.,
	 * service registration properties.
	 */
	private String contextSelector;

	/**
	 * <p>This method should be called from Whiteboard infrastructure to really perform the validation and set
	 * <em>isValid</em> flag, which is then used for "Failure DTO" purposes.</p>
	 *
	 * TODO_DTO: maybe we should accept some callback for DTO purposes.
	 */
	public boolean isValid() {
		if (isValid == null) {
			try {
				isValid = performValidation();
			} catch (Exception ignored) {
				isValid = false;
			}
		}
		return isValid;
	}

	/**
	 * <p>Perform element-specific validation and throws different exceptions for all element-specific validation
	 * problems. This method should not be called for Whiteboard purposes, where "failure DTO" has to be
	 * configured.</p>
	 *
	 * <p>This method should be called in Http Service scenario where we immediately need strong feedback - with
	 * exceptions thrown for all validation problems. In Whiteboard scenario, the exception is caught, logged and
	 * it's the tracker that prevents further registration.</p>
	 *
	 * <p>This method <em>may</em> alter the state of the model when (which is possible during validation) some
	 * extra information is obtained/compiled/processed.</p>
	 *
	 * @return
	 */
	public abstract Boolean performValidation() throws Exception;

	/**
	 * Get unmodifiable list of {@link OsgiContextModel osgi models} with which given {@link ElementModel}
	 * should be associated during registration.
	 *
	 * @return
	 */
	public List<OsgiContextModel> getContextModels() {
		if (!closed) {
			// sort according to specification, so when this list is traversed, each OsgiContextModel for
			// not yet visited ServletContextModel (with unique context path) is the "best" one according
			// to service rank / service id
			Collections.sort(contextModels);
			// make immutable
			contextModels = Collections.unmodifiableList(contextModels);
			contextModelsInfo = contextModels.stream()
					.map(ocm -> String.format("{%s,%s,%s,%s}", ocm.isWhiteboard() ? "WB" : "HS", ocm.getId(), ocm.getName(), ocm.getContextPath()))
					.collect(Collectors.joining(", ", "[", "]"));
			closed = true;
		}
		return contextModels;
	}

	public String getContextModelsInfo() {
		return contextModelsInfo;
	}

	/**
	 * This method is used to add {@link OsgiContextModel} when an {@link ElementModel} is created for the first
	 * time (whether it's Whiteboard or HttpService scenario).
	 * @param model
	 */
	public void addContextModel(OsgiContextModel model) {
		if (closed) {
			throw new IllegalStateException("Can't add new context models to " + this);
		} else {
			contextModels.add(model);
		}
	}

	/**
	 * This method replaces the models for existing {@Link ElementModel} when conditions change (when for example
	 * new {@link OsgiContextModel} is registered and element should be registered to it after it was already
	 * registered to other context matching the context selector).
	 */
	public void changeContextModels(List<OsgiContextModel> models) {
		List<OsgiContextModel> newModels = new ArrayList<>(models);
		Collections.sort(newModels);
		contextModels = Collections.unmodifiableList(newModels);
		contextModelsInfo = contextModels.stream()
				.map(ocm -> String.format("{%s,%s,%s,%s}", ocm.isWhiteboard() ? "WB" : "HS", ocm.getId(), ocm.getName(), ocm.getContextPath()))
				.collect(Collectors.joining(", ", "[", "]"));
	}

	/**
	 * Each {@link ElementModel} can register itself as Whiteboard element.
	 * @param view
	 */
	public abstract void register(WhiteboardWebContainerView view);

	/**
	 * Each {@link ElementModel} can unregister itself as Whiteboard element.
	 * @param view
	 */
	public abstract void unregister(WhiteboardWebContainerView view);

	/**
	 * When sending events related to {@link ElementModel} we can't use the same instance which is kept in
	 * {@link org.ops4j.pax.web.service.spi.model.ServerModel}, we <strong>have to</strong> copy relevant
	 * information to lightweight object.
	 * @return
	 */
	public abstract D asEventData();

	/**
	 * Set {@link ElementModel} information in {@link WebElementEventData} - to be called in specializations
	 * of {@link #asEventData()} method.
	 *
	 * @param data
	 */
	protected void setCommonEventProperties(WebElementEventData data) {
		data.setServiceRank(this.serviceRank);
		data.setServiceId(this.serviceId);
		data.setElementReference(this.elementReference);
		data.setOriginBundle(this.registeringBundle);
		this.contextModels.forEach(cm -> data.getContextNames().add(cm.getName()));
		if (this.contextModels.size() == 1 && this.contextModels.get(0).hasDirectHttpContextInstance()) {
			// special case of "old" HttpContext associated with the model
			data.setHttpContext(this.contextModels.get(0).getDirectHttpContextInstance());
		}
	}

	public boolean hasContextModels() {
		return contextModels.size() > 0;
	}

	public int getServiceRank() {
		return serviceRank;
	}

	public void setServiceRank(int serviceRank) {
		this.serviceRank = serviceRank;
	}

	public long getServiceId() {
		return serviceId;
	}

	public void setServiceId(long serviceId) {
		this.serviceId = serviceId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public ServiceReference<T> getElementReference() {
		return elementReference;
	}

	public void setElementReference(ServiceReference<T> elementReference) {
		this.elementReference = elementReference;
		if (elementReference != null) {
			this.prototype = Constants.SCOPE_PROTOTYPE.equals(elementReference.getProperty(Constants.SERVICE_SCOPE));
		}
	}

	/**
	 * Should be used only when {@link #getElementReference()} returns non-null value.
	 * @return
	 */
	public boolean isPrototype() {
		return prototype;
	}

	public Supplier<? extends T> getElementSupplier() {
		return elementSupplier;
	}

	public void setElementSupplier(Supplier<? extends T> elementSupplier) {
		this.elementSupplier = elementSupplier;
	}

	public Bundle getRegisteringBundle() {
		return registeringBundle;
	}

	public void setRegisteringBundle(Bundle registeringBundle) {
		this.registeringBundle = registeringBundle;
	}

	/**
	 * Some Whiteboard services specify context selector using
	 * {@link org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_CONTEXT_SELECT} service
	 * registration property and some (legacy) using {@link ContextRelated#getContextId()}/{@link ContextRelated#getContextSelectFilter()}.
	 * After setting the selector, we no longer have to resolve one.
	 * @param mappingSelector
	 */
	public void setContextSelector(String mappingSelector) {
		this.contextSelector = mappingSelector;
	}

	public String getContextSelector() {
		return contextSelector;
	}

	/**
	 * <p>Method corresponding to {@link ContextRelated#getContextSelectFilter()} that sets actual context-selection
	 * {@link Filter} to be used for this {@link ElementModel} after customizing <em>incoming</em> user-registered
	 * OSGi service.</p>
	 *
	 * <p>This filter can be changed only by calling {@link org.osgi.framework.ServiceRegistration#setProperties(Dictionary)}
	 * on existing registration, but it's rather rare scenario. The point is that this filter is called every time
	 * new {@link OsgiContextModel} is tracked (or changed or removed) to check whether existing set of contexts
	 * associated with given {@link ElementModel} has changed - to check whether this {@link ElementModel} should
	 * be added to new or removed from existing contexts (or both)</p>
	 *
	 * @param contextFilter
	 */
	public void setContextSelectFilter(Filter contextFilter) {
		this.contextFilter = contextFilter;
	}

	public Filter getContextFilter() {
		return contextFilter;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{id=" + getId() + ",contexts=" + contextModels + "}";
	}

	/**
	 * Returns negative value if this element is "lesser" than the argument, which means it should have higher
	 * priority.
	 *
	 * @param o
	 * @return
	 */
	@Override
	public int compareTo(ElementModel<T, D> o) {
		int c1 = Integer.compare(this.serviceRank, o.serviceRank);
		if (c1 != 0) {
			// higher rank - "lesser" service in terms of order
			return -c1;
		}
		// higher service id - "greater" service in terms of order
		int c2 = Long.compare(this.serviceId, o.serviceId);
		if (c2 != 0) {
			return c2;
		}

		// we need some fallback here - prefer model created earlier
		return Integer.compare(this.getNumericId(), o.getNumericId());
	}

	// ---+ equals() and hashCode() become final in ElementModel because this class requires strong identity
	//    |  - they can be kept in "disabled" collections
	//    |  - checking for uniqueness as before (for example by servlet name within context) is done explicitly
	//    |    during registration
	//    | don't make these methods non-final!

	@Override
	public final boolean equals(Object o) {
		return super.equals(o);
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

}
