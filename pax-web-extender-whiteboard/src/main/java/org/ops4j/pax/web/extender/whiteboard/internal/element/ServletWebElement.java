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

import java.util.List;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.ServletMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.util.DictionaryUtils;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultErrorPageMapping;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Registers/unregisters {@link ServletMapping} with {@link HttpService} /
 * {@link WebContainer}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ServletWebElement implements WebElement {

	/**
	 * Servlet mapping.
	 */
	private ServletMapping servletMapping;
	private List<DefaultErrorPageMapping> errorMappings;

	/**
	 * Constructor.
	 *
	 * @param servletMapping servlet mapping; cannot be null
	 * @param errorMappings
	 */
	public ServletWebElement(final ServletMapping servletMapping, List<DefaultErrorPageMapping> errorMappings) {
		NullArgumentException
				.validateNotNull(servletMapping, "Servlet mapping");
		this.servletMapping = servletMapping;
		this.errorMappings = errorMappings;
	}

	/**
	 * Registers servlet with http service / web container.
	 */
	public void register(final HttpService httpService,
						 final HttpContext httpContext) throws Exception {
		if (servletMapping.getAlias() != null) {
			httpService.registerServlet(servletMapping.getAlias(),
					servletMapping.getServlet(),
					DictionaryUtils.adapt(servletMapping.getInitParams()),
					httpContext);
		} else {
			if (WebContainerUtils.isWebContainer(httpService)) {
				((WebContainer) httpService).registerServlet(
						servletMapping.getServlet(),
						servletMapping.getServletName(),
						servletMapping.getUrlPatterns(),
						DictionaryUtils.adapt(servletMapping.getInitParams()),
						servletMapping.getLoadOnStartup(),
						servletMapping.getAsyncSupported(),
						servletMapping.getMultipartConfig(),
						httpContext);
				// ((WebContainer) httpService).end(httpContext);
			}
		}
		//special handling for OSGi R6 registration of Servlet as ErrorHandler
		if (errorMappings != null) {
			for (DefaultErrorPageMapping errorPageMapping : errorMappings) {
				((WebContainer) httpService).registerErrorPage(
						errorPageMapping.getError(),
						servletMapping.getAlias(), httpContext);
			}
		}
	}

	/**
	 * Unregisters servlet from http service / web container.
	 */
	public void unregister(final HttpService httpService,
						   final HttpContext httpContext) {
		if (servletMapping.getAlias() != null) {
			httpService.unregister(servletMapping.getAlias());
		} else {
			if (WebContainerUtils.isWebContainer(httpService)) {
				((WebContainer) httpService).unregisterServlet(servletMapping
						.getServlet());
			}
		}
	}

	public String getHttpContextId() {
		return servletMapping.getHttpContextId();
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("mapping=").append(servletMapping)
				.append("}").toString();
	}

}