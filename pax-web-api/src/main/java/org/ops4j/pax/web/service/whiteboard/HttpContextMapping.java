/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.whiteboard;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * <p><em>HttpContext mapping</em> collects all the information required to register a {@link HttpContext} to
 * allow referencing it later.</p>
 *
 * <p>Registering a {@link HttpContext} can be done in two ways:<li>
 *     <li>registering a service with this interface - all the information is included in service itself
 *     (<em>explicit whiteboard approach</em>)</li>
 *     <li>registering a {@link HttpContext} service, while required properties (mapping, name, parameters) are
 *     specified using service registration properties (legacy Pax Web Whiteboard approach)</li>
 * </li></p>
 *
 * <p>This interface doesn't extend {@link ContextRelated}, because it represents the <em>context</em> itself.</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.4.0, March 16, 2008
 */
public interface HttpContextMapping extends ContextMapping {

	/**
	 * <p>Get actual context being registered. If specified, this is the way to provide the <em>behavioral</em>
	 * aspects of the <em>context</em> (e.g., {@link HttpContext#handleSecurity}). If not specified, default
	 * {@link HttpContext} will be created according to {@link ContextMapping#getContextId()}.</p>
	 *
	 * @return associated HttpContext; can be null, case when a default http context will be created and used
	 */
	HttpContext getHttpContext();

	/**
	 * <p>Get actual context being registered. This version accepts {@link Bundle} argument to reflect
	 * recommendations from Whiteboard Service specification, where
	 * {@link org.osgi.service.http.context.ServletContextHelper} is suggested to be registered as
	 * {@link org.osgi.framework.ServiceFactory}.</p>
	 *
	 * @param bundle
	 * @return
	 */
	default HttpContext getHttpContext(Bundle bundle) {
		return getHttpContext();
	}

	/**
	 * Whether the context should be shared - Whiteboard contexts are always shared, but Http Service doesn't
	 * define such flag at all.
	 * @return
	 */
	boolean isShared();

}
