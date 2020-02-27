/*
 * Copyright 2007 Damian Golda.
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal.element;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardElement;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

/**
 * TODO Add java doc
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public abstract class WebElement<T> implements WhiteboardElement {

	protected final ServiceReference<T> serviceReference;
	/**
	 * Elements can define custom validation:
	 * Only valid WebElements registered via whiteboard will be published to the {@link WebContainer}.
	 */
	protected boolean valid = true;

	/**
	 * Subclasses must provide the service-reference.
	 * @param ref the service-reference of the registered service. Used to extract further properties.
	 */
	WebElement(ServiceReference<T> ref) {
		NullArgumentException.validateNotNull(ref, "service-reference");
		this.serviceReference = ref;
	}

	/**
	 * Called by {@link org.ops4j.pax.web.extender.whiteboard.internal.WebApplication#registerWebElement(WebElement)}
	 * only when {@link #isValid()} resolves to true
	 * @param webContainer Pax-Web HttpService-implementation
	 * @param httpContext http context to be assicated to the WebElement
	 * @throws Exception registration-exception
	 */
	public abstract void register(WebContainer webContainer, HttpContext httpContext)
			throws Exception;

	/**
	 * Called by {@link org.ops4j.pax.web.extender.whiteboard.internal.WebApplication#unregisterWebElement(WebElement)}
	 * only when {@link #isValid()} resolves to true
	 * @param webContainer Pax-Web HttpService-implementation
	 * @param httpContext http context assicated to the WebElement
	 */
	public abstract void unregister(WebContainer webContainer, HttpContext httpContext);

	/**
	 * Implementation must provide the ID of the associated HttpContext from their particular Service-Mapping
	 *
	 * @return the ID of the associated HttpContext
	 */
	public abstract String getHttpContextId();

	@Override
	public long getServiceID() {
		return (Long)serviceReference.getProperty(Constants.SERVICE_ID);
	}

	/**
	 * Elements can define custom validation:
	 * Only valid WebElements registered via whiteboard will be published to the {@link WebContainer}.
	 * @return the validation-state of this element
	 * @see #valid
	 */
	@Override
	public boolean isValid() {
		return valid;
	}
}