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
import org.ops4j.pax.web.service.whiteboard.ResourceMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardResourceMapping;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

/**
 * FIXME not sure if we need to track ResourceMappings in addition to Resources
 * Registers/unregisters {@link ResourceMapping} with {@link WebContainer}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ResourceMappingWebElement extends WebElement<ResourceMapping> implements WhiteboardResourceMapping {

	private ResourceMapping resourceMapping;

	/**
	 * Constructs a new ResourceMappingWebElement
	 * @param ref the service-reference behind the registered http-whiteboard-service
	 * @param resourceMapping ResourceMapping containing all necessary information
	 */
	public ResourceMappingWebElement(final ServiceReference<ResourceMapping> ref, ResourceMapping resourceMapping) {
		super(ref);
		NullArgumentException.validateNotNull(resourceMapping, "Resource mapping");
		this.resourceMapping = resourceMapping;
	}

	@Override
	public void register(final WebContainer webContainer,
						 final HttpContext httpContext) throws Exception {
		webContainer.registerResources(
				resourceMapping.getAlias(),
				resourceMapping.getPath(),
				httpContext);
	}

	@Override
	public String getHttpContextId() {
		return resourceMapping.getHttpContextId();
	}

	@Override
	public void unregister(final WebContainer webContainer,
						   final HttpContext httpContext) {
		webContainer.unregister(resourceMapping.getAlias());
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{mapping=" + resourceMapping + "}";
	}

	@Override
	public ResourceMapping getResourceMapping() {
		return resourceMapping;
	}
}