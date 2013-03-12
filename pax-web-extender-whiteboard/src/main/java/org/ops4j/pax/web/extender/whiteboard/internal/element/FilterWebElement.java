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
import org.ops4j.pax.web.extender.whiteboard.FilterMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.util.DictionaryUtils;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Registers/unregisters {@link FilterMapping} with {@link WebContainer}.
 * 
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class FilterWebElement implements WebElement {

	/**
	 * Filter mapping.
	 */
	private FilterMapping filterMapping;

	/**
	 * Constructor.
	 * 
	 * @param filterMapping
	 *            filter mapping; cannot be null
	 */
	public FilterWebElement(final FilterMapping filterMapping) {
		NullArgumentException.validateNotNull(filterMapping, "Filter mapping");
		this.filterMapping = filterMapping;
	}

	/**
	 * Registers filter from http service.
	 */
	public void register(final HttpService httpService,
			final HttpContext httpContext) throws Exception {
		if (WebContainerUtils.isWebContainer(httpService)) {
			((WebContainer) httpService).registerFilter(
					filterMapping.getFilter(), filterMapping.getUrlPatterns(),
					filterMapping.getServletNames(),
					DictionaryUtils.adapt(filterMapping.getInitParams()),
					httpContext);
		} else {
			throw new UnsupportedOperationException(
					"Internal error: In use HttpService is not an WebContainer (from Pax Web)");
		}
	}

	/**
	 * Unregisters filter from http service.
	 */
	public void unregister(final HttpService httpService,
			final HttpContext httpContext) {
		if (WebContainerUtils.isWebContainer(httpService)) {
			((WebContainer) httpService).unregisterFilter(filterMapping
					.getFilter());
		}
	}

	public String getHttpContextId() {
		return filterMapping.getHttpContextId();
	}

	@Override
	public String toString() {
		return new StringBuffer().append(this.getClass().getSimpleName())
				.append("{").append("mapping=").append(filterMapping)
				.append("}").toString();
	}

}