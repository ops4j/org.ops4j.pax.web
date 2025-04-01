/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.runtime.dto.DTOConstants;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks services of {@link ServletContextHelper} class - <strong>the</strong> context for R7 Whiteboard
 * Service specification.
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.2.0, August 21, 2007
 */
public class ServletContextHelperTracker extends AbstractContextTracker<ServletContextHelper> {

	private final Pattern SN = Pattern.compile("^[\\p{Alnum}_-]+[.\\p{Alnum}]*$");

	private ServletContextHelperTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<ServletContextHelper, OsgiContextModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new ServletContextHelperTracker(whiteboardExtenderContext, bundleContext).create(ServletContextHelper.class);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void configureContextModel(ServiceReference<ServletContextHelper> serviceReference,
			OsgiContextModel model) {

		// always shared
		model.setShared(true);

		// 1. context name
		String name = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
				Utils::asString);
		if (name == null && serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME) != null) {
			model.setDtoFailureCode(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}
		if (name == null || "".equals(name.trim())) {
			name = HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;
		} else {
			// 140.16.2.7: The value must follow the "symbolic-name" specification from Section 1.3.2 of the OSGi Core Specification.
			// 1.3.2 General Syntax Definitions:
			// digit           ::= [0..9]
			// alpha           ::= [a..zA..Z]
			// alphanum        ::= alpha | digit
			// token           ::= ( alphanum | '_' | '-' )+
			// symbolic-name   ::= token ( '.' token )*
			if (!SN.matcher(name).matches()) {
				model.setDtoFailureCode(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			}
		}
		model.setName(name);

		// 2. context path
		// NOTE: Pax Web 7 was stripping leading "/" and was mixing concepts of "name" and "path"
		boolean propertiesInvalid = false;
		String contextPath = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
				Utils::asString);
		Object p = serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);
		if (!(p instanceof String)) {
			propertiesInvalid = true;
		}
		if (contextPath == null || "".equals(contextPath.trim())) {
			contextPath = PaxWebConstants.DEFAULT_CONTEXT_PATH;
		}
		model.setContextPath(contextPath);

		// 3. context params
		// NOTE: Pax Web 7 was passing ALL service registration properties as init parameters
		Map<String, String> initParams = new LinkedHashMap<>();
		for (String key : serviceReference.getPropertyKeys()) {
			if (key.startsWith(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX)) {
				String value = Utils.getStringProperty(serviceReference, key);
				if (value != null) {
					initParams.put(key.substring(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX.length()), value);
				}
			}
		}
		model.getContextParams().clear();
		model.getContextParams().putAll(initParams);

		// 4. pass all service registration properties...
		model.getContextRegistrationProperties().putAll(Utils.toMap(serviceReference));
		// ... but in case there was no osgi.http.whiteboard.context.path property, let's set it now
		model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, name);
		model.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);

		// 5. virtual hosts
		String[] virtualHosts = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.SERVICE_PROPERTY_VIRTUAL_HOSTS_LEGACY, PaxWebConstants.SERVICE_PROPERTY_VIRTUAL_HOSTS,
				(n, v) -> Utils.asStringArray(n, v, true));
		String[] connectors = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.SERVICE_PROPERTY_CONNECTORS_LEGACY, PaxWebConstants.SERVICE_PROPERTY_CONNECTORS,
				(n, v) -> Utils.asStringArray(n, v, true));

		model.getVirtualHosts().clear();
		model.getConnectors().clear();
		if (virtualHosts != null) {
			model.getVirtualHosts().addAll(Arrays.asList(virtualHosts));
		}
		if (connectors != null) {
			model.getConnectors().addAll(Arrays.asList(connectors));
		}

		// 6. source of the context
		// ServletContextHelper will be obtained from service reference on each call using correct bundle - always
		// this is the standard Whiteboard Service specification scenario
		// even if the scope is "singleton", remember the reference
		model.setContextReference(serviceReference);

		if (propertiesInvalid) {
			model.setDtoFailureCode(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}
	}

}
