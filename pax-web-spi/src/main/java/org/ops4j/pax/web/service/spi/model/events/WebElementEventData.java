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
package org.ops4j.pax.web.service.spi.model.events;

import java.util.LinkedList;
import java.util.List;

import org.ops4j.pax.web.service.WebContainerContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Base class for data for events related to registration/unregistration of <em>web elements</em> like
 * {@link jakarta.servlet.Servlet servlets} or {@link jakarta.servlet.Filter filters}.
 */
public abstract class WebElementEventData {

	private int serviceRank;
	private long serviceId;
	private Bundle originBundle;
	private ServiceReference<?> elementReference;
	private final List<String> contextNames = new LinkedList<>();

	// stored only if element is associated with single, directly specified HttpContext/WebContainerContext
	private WebContainerContext httpContext;

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

	public ServiceReference<?> getElementReference() {
		return elementReference;
	}

	public void setElementReference(ServiceReference<?> elementReference) {
		this.elementReference = elementReference;
	}

	public List<String> getContextNames() {
		return contextNames;
	}

	public Bundle getOriginBundle() {
		return originBundle;
	}

	public void setOriginBundle(Bundle originBundle) {
		this.originBundle = originBundle;
	}

	public void setHttpContext(WebContainerContext httpContext) {
		this.httpContext = httpContext;
	}

	public WebContainerContext getHttpContext() {
		return httpContext;
	}

}
