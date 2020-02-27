/*
 * Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.extender.whiteboard;

/**
 * Defines standard names for pax web extender related registration properties.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, December 20, 2007
 */
// CHECKSTYLE:OFF
public interface ExtenderConstants {

	/**
	 * The registration property for the alias to be used by a servlet/resource
	 * registration.
	 */
	String PROPERTY_ALIAS = "alias";
	/**
	 * The flag for a shared http context.
	 */
	String PROPERTY_HTTP_CONTEXT_SHARED = "httpContext.shared";
	/**
	 * The http context path property key.
	 */
	String PROPERTY_HTTP_CONTEXT_PATH = "httpContext.path";
	/**
	 * The http context virtual hosts property key.
	 */
	String PROPERTY_HTTP_VIRTUAL_HOSTS = "httpContext.virtualhosts";
	/**
	 * The http context connectors property key.
	 */
	String PROPERTY_HTTP_CONNECTORS = "httpContext.connectors";
	/**
	 * The registration property for the url patterns to be used by a filter
	 * registration.
	 */
	String PROPERTY_URL_PATTERNS = "urlPatterns";

	/**
	 * The registration property for the servlet names to be used by a filter
	 * registration.
	 */
	String PROPERTY_SERVLET_NAMES = "servletNames";

	/**
	 * The registration property for filtering init parameters. All init
	 * parameters starting with something different then the init-prefix will be
	 * ignored and not added to the init params of the servlet.
	 */
	String PROPERTY_INIT_PREFIX = "init-prefix";

	/**
	 * The default init-prefix: <b>.init</b>.
	 */
	String DEFAULT_INIT_PREFIX_PROP = "init.";

	/**
	 * A registration property for enabling filtering for WebSockets via Whitebox Tracking.
	 */
	String WEBSOCKET = "websocket";

}
