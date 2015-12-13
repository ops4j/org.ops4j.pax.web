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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServlet;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ServletWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.util.ServicePropertiesUtils;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultErrorPageMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultServletMapping;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.utils.ServletAnnotationScanner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks {@link Servlet}s.
 * 
 * @author Alin Dreghiciu
 * @author Thomas Joseph
 * @since 0.4.0, April 05, 2008
 */
public class ServletTracker<T extends Servlet> extends
		AbstractTracker<T, ServletWebElement> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(ServletTracker.class);

	/**
	 * Constructor.
	 * 
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
	private ServletTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		super(extenderContext, bundleContext);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Servlet> ServiceTracker<T, ServletWebElement> createTracker(
			final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		return new ServletTracker<T>(extenderContext, bundleContext)
				.create(new Class[] {
                        Servlet.class,
                        HttpServlet.class
                });
	}

	/**
	 * @see AbstractTracker#createWebElement(ServiceReference, Object)
	 */
	@Override
	ServletWebElement createWebElement(
			final ServiceReference<T> serviceReference, final T published) {
		String alias = ServicePropertiesUtils.getStringProperty(serviceReference, ExtenderConstants.PROPERTY_ALIAS);
		Object urlPatternsProp = serviceReference.getProperty(ExtenderConstants.PROPERTY_URL_PATTERNS);
		
		String[] initParamKeys = serviceReference.getPropertyKeys();
		String initPrefixProp = ServicePropertiesUtils.getStringProperty(serviceReference, ExtenderConstants.PROPERTY_INIT_PREFIX);
		if (initPrefixProp == null) {
			initPrefixProp = ExtenderConstants.DEFAULT_INIT_PREFIX_PROP;
		}
		String servletName = ServicePropertiesUtils.getStringProperty(serviceReference,WebContainerConstants.SERVLET_NAME);
		
		if (urlPatternsProp == null) {
            urlPatternsProp = serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
        } else {
            String[] whiteBoardProp = ServicePropertiesUtils.getArrayOfStringProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
            urlPatternsProp = ServicePropertiesUtils.mergePropertyListOfStringsToArrayOfStrings(urlPatternsProp, Arrays.asList(whiteBoardProp));
        }
		
		
		if (servletName == null) {
            servletName = ServicePropertiesUtils.getStringProperty(serviceReference,HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME);
        }
		
		
		ServletAnnotationScanner annotationScan = new ServletAnnotationScanner(published.getClass());

		if (annotationScan.scanned) {
			if (urlPatternsProp == null) {
				urlPatternsProp = annotationScan.urlPatterns;
			} else {
			    List<String> annotationsUrlPatterns = Arrays.asList(annotationScan.urlPatterns);
				urlPatternsProp = ServicePropertiesUtils.mergePropertyListOfStringsToArrayOfStrings(urlPatternsProp, annotationsUrlPatterns);
			}
		}
		
		// special Whiteboard Error-Servlet handling
        String[] errorPageParams = ServicePropertiesUtils.getArrayOfStringProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE);

		if (servletName != null
				&& (!(servletName instanceof String) || servletName.toString()
						.trim().length() == 0)) {
			LOG.warn("Registered servlet [" + published
					+ "] did not contain a valid servlet-name property.");
			return null;
		}
		if (alias != null && urlPatternsProp != null) {
			LOG.warn("Registered servlet [" + published
					+ "] cannot have both alias and url patterns");
			return null;
		}
		
		if (errorPageParams != null) {
		    if (servletName == null) {
		        servletName = "errorServlet";
		    }
		    if (alias == null && urlPatternsProp == null) {
		        alias = "/errorServlet";
		    }
		}
		
		if (alias == null && urlPatternsProp == null) {
			LOG.warn("Registered servlet ["
					+ published
					+ "] did not contain a valid alias or url patterns property");
			return null;
		}
		if (alias != null
				&& (!(alias instanceof String) || ((String) alias).trim()
						.length() == 0)) {
			LOG.warn("Registered servlet [" + published
					+ "] did not contain a valid alias property");
			return null;
		}
		String[] urlPatterns = null;
		if (urlPatternsProp != null) {
			if (urlPatternsProp instanceof String
					&& ((String) urlPatternsProp).trim().length() != 0) {
				urlPatterns = new String[] { (String) urlPatternsProp };
			} else if (urlPatternsProp instanceof String[]) {
				urlPatterns = (String[]) urlPatternsProp;
			} else {
				LOG.warn("Registered servlet ["
						+ published
						+ "] has an invalid url pattern property (must be a non empty String or String[])");
				return null;
			}
		}
		String httpContextId = ServicePropertiesUtils.getStringProperty(serviceReference,ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID);
		
		//TODO: Make sure the current HttpContextSelect works together with R6
		if (httpContextId == null) {
		    String httpContextSelector = ServicePropertiesUtils.getStringProperty(serviceReference,HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
		    if (httpContextSelector != null) {
		        httpContextSelector = httpContextSelector.substring(1, httpContextSelector.length());
		        httpContextId = httpContextSelector.substring(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME.length()+1);
		        httpContextId = httpContextId.substring(0, httpContextId.length()-1);
		    }
		}
		
		// make all the service parameters available as initParams to
		// registering the Servlet
		Map<String, String> initParams = new HashMap<String, String>();
		Integer loadOnStartup = null;
		Boolean asyncSupported = null;
		for (String key : initParamKeys) {
			try {
				String value = serviceReference.getProperty(key) == null ? ""
						: serviceReference.getProperty(key).toString();

				// if the prefix is null or empty, match is true, otherwise its
				// only true if it matches the prefix
				if (key.startsWith(initPrefixProp == null ? "" : initPrefixProp)) {
					initParams.put(key.replaceFirst(initPrefixProp, ""), value);
				} else if (key.startsWith(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX)) {
				    initParams.put(key.replaceFirst(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX, ""), value);
				}
				if ("load-on-startup".equalsIgnoreCase(key) && value != null) {
					loadOnStartup = Integer.parseInt(value);
				}
				if ("async-supported".equalsIgnoreCase(key) && value != null) {
					asyncSupported = Boolean.parseBoolean(value);
				}
				//CHECKSTYLE:OFF
			} catch (Exception ignore) { 
				// ignore
			}
			//CHECKSTYLE:ON
		}
		
		if (asyncSupported == null)
		    asyncSupported = ServicePropertiesUtils.getBooleanProperty(serviceReference,HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED);

		if (annotationScan.scanned) {
			for (WebInitParam param : annotationScan.webInitParams) {
				String name = param.name();
				String value = param.value();
				initParams.put(name, value);
			}
		}

		if (annotationScan.scanned && annotationScan.asyncSupported != null) {
			asyncSupported = annotationScan.asyncSupported;
		}

		if (annotationScan.scanned && annotationScan.loadOnStartup != null) {
			loadOnStartup = Integer.valueOf(annotationScan.loadOnStartup);
		}
		
		DefaultServletMapping mapping = new DefaultServletMapping();
		mapping.setHttpContextId(httpContextId);
		mapping.setServlet(published);
		if (servletName != null) {
			mapping.setServletName(servletName.toString().trim());
		}
		mapping.setAlias((String) alias);
		mapping.setUrlPatterns(urlPatterns);
		mapping.setInitParams(initParams);
		mapping.setLoadOnStartup(loadOnStartup);
		mapping.setAsyncSupported(asyncSupported);
		
		List<DefaultErrorPageMapping> errorMappings = null;
		
		if (errorPageParams != null) {
		    errorMappings = new ArrayList<>();
		    for (String errorPageParam : errorPageParams) {
		        DefaultErrorPageMapping errorMapping = new DefaultErrorPageMapping();
		        errorMapping = new DefaultErrorPageMapping();
		        errorMapping.setHttpContextId(httpContextId);
		        errorMapping.setLocation(alias);
		        errorMapping.setError(errorPageParam);
                errorMappings.add(errorMapping);
            }
		}
		
		return new ServletWebElement(mapping, errorMappings);
	}

}
