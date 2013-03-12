/*
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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.element.FilterWebElement;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultFilterMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks {@link Filter}s.
 * 
 * @author Alin Dreghiciu
 * @author Thomas Joseph
 * @since 0.4.0, April 05, 2008
 */
public class FilterTracker extends AbstractTracker<Filter, FilterWebElement> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(FilterTracker.class);

	/**
	 * Constructor.
	 * 
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
	private FilterTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		super(extenderContext, bundleContext);
	}

	public static ServiceTracker<Filter, FilterWebElement> createTracker(
			final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		return new FilterTracker(extenderContext, bundleContext)
				.create(Filter.class);
	}

	/**
	 * @see AbstractTracker#createWebElement(ServiceReference, Object)
	 */
	@Override
	FilterWebElement createWebElement(
			final ServiceReference<Filter> serviceReference,
			final Filter published) {
		final Object urlPatternsProp = serviceReference
				.getProperty(ExtenderConstants.PROPERTY_URL_PATTERNS);
		String[] urlPatterns = null;
		if (urlPatternsProp != null) {
			if (urlPatternsProp instanceof String) {
				urlPatterns = new String[] { (String) urlPatternsProp };
			} else if (urlPatternsProp instanceof String[]) {
				urlPatterns = (String[]) urlPatternsProp;
			} else {
				LOG.warn("Registered filter ["
						+ published
						+ "] has an invalid url pattern property (must be String or String[])");
				return null;
			}
		}
		final Object servletNamesProp = serviceReference
				.getProperty(ExtenderConstants.PROPERTY_SERVLET_NAMES);
		String[] servletNAmes = null;
		if (servletNamesProp != null) {
			if (servletNamesProp instanceof String) {
				servletNAmes = new String[] { (String) servletNamesProp };
			} else if (servletNamesProp instanceof String[]) {
				servletNAmes = (String[]) servletNamesProp;
			} else {
				LOG.warn("Registered filter ["
						+ published
						+ "] has an invalid servlet names property (must be String or String[])");
				return null;
			}
		}
		if (urlPatterns == null && servletNAmes == null) {
			LOG.warn("Registered filter ["
					+ published
					+ "] did not contain a valid url pattern or servlet names property");
			return null;
		}
		Object httpContextId = serviceReference
				.getProperty(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID);
		if (httpContextId != null
				&& (!(httpContextId instanceof String) || ((String) httpContextId)
						.trim().length() == 0)) {
			LOG.warn("Registered filter [" + published
					+ "] did not contain a valid http context id");
			return null;
		}
		final String[] initParamKeys = serviceReference.getPropertyKeys();
		// make all the service parameters available as initParams to
		// registering the Filter
		Map<String, String> initParams = new HashMap<String, String>();
		for (String key : initParamKeys) {
			try {
				String value = serviceReference.getProperty(key) == null ? ""
						: serviceReference.getProperty(key).toString();
				initParams.put(key, value);
			} catch (Exception ignore) { // CHECKSTYLE:SKIP
				// ignore
			}
		}
		final DefaultFilterMapping mapping = new DefaultFilterMapping();
		mapping.setFilter(published);
		mapping.setHttpContextId((String) httpContextId);
		mapping.setUrlPatterns(urlPatterns);
		mapping.setServletNames(servletNAmes);
		mapping.setInitParams(initParams);
		return new FilterWebElement(mapping);
	}

}
