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
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.model.Identity;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

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
 */
public abstract class ElementModel<T> extends Identity implements Comparable<ElementModel<T>> {

	/**
	 * List of {@link OsgiContextModel osgi contexts} with which given {@link ElementModel} is associated.
	 * This list may be altered using {@link #addContextModel(OsgiContextModel)} ()}, but on first invocation of
	 * {@link #getContextModels()}, this list is <em>latched</em>, sorted and can't be modified any further.
	 */
	protected List<OsgiContextModel> contextModels = new ArrayList<>();

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
	private ServiceReference<? extends T> elementReference;

	/**
	 * Even if the element is not registered as Whiteboard service, we still need a bundle in which scope
	 * the element was registered (for example using
	 * {@link org.osgi.service.http.HttpService#registerServlet(String, Servlet, Dictionary, HttpContext)}), so
	 * we can use its {@link org.osgi.framework.BundleContext} to obtain proper reference to
	 * {@link org.osgi.service.http.context.ServletContextHelper} if needed.
	 */
	private Bundle registeringBundle;

	/**
	 * Get unmodifiable list of {@link OsgiContextModel osgi models} with which given {@link ElementModel}
	 * should be associated
	 *
	 * @return
	 */
	public List<OsgiContextModel> getContextModels() {
		if (!closed) {
			// sort according to specification, so when this list is traversed, each OsgiContextModel for
			// not yet visited ServletContextModel (with unique context path) is the "best" one according
			// to service rank / service id
			Collections.sort(contextModels);
			// make ummutable
			contextModels = Collections.unmodifiableList(contextModels);
			closed = true;
		}
		return contextModels;
	}

	public void addContextModel(OsgiContextModel model) {
		if (closed) {
			throw new IllegalStateException("Can't add new context models to " + this);
		} else {
			contextModels.add(model);
		}
	}

	public Set<ServletContextModel> getServletContextModels() {
		return contextModels.stream().map(OsgiContextModel::getServletContextModel).collect(Collectors.toSet());
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

	public ServiceReference<? extends T> getElementReference() {
		return elementReference;
	}

	public void setElementReference(ServiceReference<? extends T> elementReference) {
		this.elementReference = elementReference;
	}

	public Bundle getRegisteringBundle() {
		return registeringBundle;
	}

	public void setRegisteringBundle(Bundle registeringBundle) {
		this.registeringBundle = registeringBundle;
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
	public int compareTo(ElementModel o) {
		int c1 = this.serviceRank - o.serviceRank;
		if (c1 != 0) {
			// higher rank - "lesser" service in terms of order
			return -c1;
		}
		// higher service id - "greater" service in terms of order
		int c2 = (int)(this.serviceId - o.serviceId);
		if (c2 != 0) {
			return c2;
		}

		// we need some fallback here - prefer model created earlier
		return this.getNumericId() - o.getNumericId();
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
