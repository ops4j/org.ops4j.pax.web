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

import javax.servlet.Filter;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.internal.util.DictionaryUtils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.whiteboard.FilterMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardFilter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

/**
 * Registers/unregisters {@link FilterMapping} with {@link WebContainer}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class FilterWebElement<T extends Filter> extends WebElement<T> implements WhiteboardFilter {

	private FilterMapping filterMapping;

	/**
	 * Constructs a new FilterWebElement
	 * @param ref the service-reference behind the registered http-whiteboard-service
	 * @param filterMapping FilterMapping containing all necessary information
	 */
	public FilterWebElement(ServiceReference<T> ref, final FilterMapping filterMapping) {
		super(ref);
		NullArgumentException.validateNotNull(filterMapping, "Filter mapping");
		this.filterMapping = filterMapping;

		// validate
		if (filterMapping.getUrlPatterns() == null && filterMapping.getServletNames() == null) {
			valid = false;
		}
	}

	@Override
	public void register(final WebContainer webContainer, final HttpContext httpContext) throws Exception {
		//TODO: DispatcherTypes EnumSet !!
		//--> this might be done by adding those to the initParams as it's interpreted by the whiteboard-extender
//		webContainer.registerFilter(
//					filterMapping.getFilter()/*.getClass()*/,
//					filterMapping.getUrlPatterns(),
//					filterMapping.getServletNames(),
//					DictionaryUtils.adapt(filterMapping.getInitParams()),
//					filterMapping.getAsyncSupported(),
//					httpContext);
	}


	@Override
	public void unregister(final WebContainer webContainer, final HttpContext httpContext) {
		Filter filter = filterMapping.getFilter();
//		webContainer.unregisterFilter(filter);
	}

	@Override
	public String getHttpContextId() {
		return filterMapping.getHttpContextId();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{mapping=" + filterMapping + "}";
	}

	@Override
	public FilterMapping getFilterMapping() {
		return filterMapping;
	}
}