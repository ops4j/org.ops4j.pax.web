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
import org.ops4j.pax.web.extender.whiteboard.ResourceMapping;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Registers/unregisters {@link ResourceMapping} with {@link HttpService}.
 * 
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ResourceWebElement implements WebElement {

	/**
	 * Resource mapping.
	 */
	private ResourceMapping resourceMapping;

	/**
	 * Constructor.
	 * 
	 * @param resourceMapping
	 *            resource mapping; cannot be null
	 */
	public ResourceWebElement(final ResourceMapping resourceMapping) {
		NullArgumentException.validateNotNull(resourceMapping,
				"Resource mapping");
		this.resourceMapping = resourceMapping;
	}

	/**
	 * Registers resource with http service.
	 */
	public void register(final HttpService httpService,
			final HttpContext httpContext) throws Exception {
		httpService.registerResources(resourceMapping.getAlias(),
				resourceMapping.getPath(), httpContext);
	}

	public String getHttpContextId() {
		return resourceMapping.getHttpContextId();
	}

	/**
	 * Unregisters resource from http service.
	 */
	public void unregister(final HttpService httpService,
			final HttpContext httpContext) {
		httpService.unregister(resourceMapping.getAlias());
	}

	@Override
	public String toString() {
		return new StringBuffer().append(this.getClass().getSimpleName())
				.append("{").append("mapping=").append(resourceMapping)
				.append("}").toString();
	}

}