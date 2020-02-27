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

import java.util.Collection;
import java.util.List;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.internal.util.DictionaryUtils;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultErrorPageMapping;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardServlet;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME not sure if we need to track ServletMappings in addition to Servlets
 * Registers/unregisters {@link ServletMapping} with {@link WebContainer}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ServletMappingWebElement extends WebElement<ServletMapping> implements WhiteboardServlet {

	private static final Logger LOG = LoggerFactory.getLogger(ServletMappingWebElement.class);

	private ServletMapping servletMapping;
	private List<DefaultErrorPageMapping> errorMappings;

	/**
	 * Constructs a new ServletMappingWebElement
	 * @param ref the service-reference behind the registered http-whiteboard-service
	 * @param servletMapping ServletMapping containing all necessary information
	 * @param errorMappings error-page mappings containing all necessary information
	 */
	public ServletMappingWebElement(final ServiceReference<ServletMapping> ref, final ServletMapping servletMapping, List<DefaultErrorPageMapping> errorMappings) {
		super(ref);
		NullArgumentException.validateNotNull(servletMapping, "Servlet mapping");
		this.servletMapping = servletMapping;
		this.errorMappings = errorMappings;

		// validate
		final String servletName = servletMapping.getServletName();
		final String alias = servletMapping.getAlias();
		final String[] urlPatterns = servletMapping.getUrlPatterns();

		if (servletName != null && (servletName.length() == 0)) {
			LOG.warn("Registered servlet [{}] did not contain a valid servlet-name property.", getServiceID());
			valid = false;
		}
		if (alias != null && urlPatterns != null && urlPatterns.length != 0) {
			LOG.warn("Registered servlet [{}] cannot have both alias and url patterns.", getServiceID());
			valid = false;
		} else if (alias == null && urlPatterns == null) {
			LOG.warn("Registered servlet [{}] did not contain a valid alias or url patterns property.", getServiceID());
			valid = false;
		} else if (alias != null && alias.trim().length() == 0) {
			LOG.warn("Registered servlet [{}] did not contain a valid alias property.", getServiceID());
			valid = false;
		}
	}

	@Override
	public void register(final WebContainer webContainer,
						 final HttpContext httpContext) throws Exception {
		if (servletMapping.getAlias() != null) {
			webContainer.registerServlet(servletMapping.getAlias(),
					servletMapping.getServlet(),
					DictionaryUtils.adapt(servletMapping.getInitParams()),
					httpContext);
		} else {
				webContainer.registerServlet(
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
		//special handling for OSGi R6 registration of Servlet as ErrorHandler
		if (errorMappings != null) {
			for (DefaultErrorPageMapping errorPageMapping : errorMappings) {
//				webContainer.registerErrorPage(
//						errorPageMapping.getError(),
//						servletMapping.getAlias(), httpContext);
			}
		}
	}

	@Override
	public void unregister(final WebContainer webContainer,
						   final HttpContext httpContext) {
		if (servletMapping.getAlias() != null) {
			webContainer.unregister(servletMapping.getAlias());
		} else {
				webContainer.unregisterServlet(servletMapping.getServlet());
		}
	}

	@Override
	public String getHttpContextId() {
		return servletMapping.getContextId();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() +
				"{mapping=" + servletMapping + "}";
	}

	@Override
	public ServletMapping getServletMapping() {
		return servletMapping;
	}

	@Override
	public Collection<? extends ErrorPageMapping> getErrorPageMappings() {
		return errorMappings;
	}

	@Override
	public boolean isAliasRegistration() {
		return servletMapping.getAlias() != null && servletMapping.getUrlPatterns() == null;
	}
}
