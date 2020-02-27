/*
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
package org.ops4j.pax.web.extender.whiteboard.internal.element;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardErrorPage;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

/**
 * Registers/unregisters
 * {@link org.ops4j.pax.web.service.whiteboard.ErrorPageMapping} with
 * {@link WebContainer}.
 *
 * @author dsklyut
 * @since 0.7.0
 */
public class ErrorPageWebElement extends WebElement<ErrorPageMapping> implements WhiteboardErrorPage {

	private final ErrorPageMapping errorPageMapping;

	/**
	 * Constructs a new ErrorPageWebElement
	 * @param ref the service-reference behind the registered http-whiteboard-service
	 * @param errorPageMapping ErrorPageMapping containing all necessary information
	 */
	public ErrorPageWebElement(final ServiceReference<ErrorPageMapping> ref, final ErrorPageMapping errorPageMapping) {
		super(ref);
		NullArgumentException.validateNotNull(errorPageMapping, "error page errorPageMapping");
		this.errorPageMapping = errorPageMapping;
	}

	@Override
	public void register(WebContainer webContainer, HttpContext httpContext)
			throws Exception {
//			webContainer.registerErrorPage(
//					errorPageMapping.getError(),
//					errorPageMapping.getLocation(), httpContext);
	}

	@Override
	public void unregister(WebContainer webContainer, HttpContext httpContext) {
//			webContainer.unregisterErrorPage(
//					errorPageMapping.getError(), httpContext);
	}

	@Override
	public String getHttpContextId() {
		return errorPageMapping.getHttpContextId();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{mapping=" + errorPageMapping + "}";
	}


	@Override
	public ErrorPageMapping getErrorPageMapping() {
		return errorPageMapping;
	}
}
