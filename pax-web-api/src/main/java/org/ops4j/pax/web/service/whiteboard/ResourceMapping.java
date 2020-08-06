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

import org.osgi.service.http.HttpContext;

/**
 * <p>Resource mapping. Even if "resource handling" is perfomed by servlet container specific {@link javax.servlet.Servlet},
 * we don't extend {@link ServletMapping} because the servlet is provided by Pax Web itself. The most important field
 * is single <em>path</em> property, which is:<ul>
 *     <li>For HttpService: 2nd parameter to
 *     {@link org.osgi.service.http.HttpService#registerResources(String, String, HttpContext)}</li>
 *     <li>For Whiteboard service:
 *     {@link org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_RESOURCE_PREFIX} service
 *     registration property</li>
 * </ul></p>
 *
 * <p>Relevant trackers take care to obtain required information carried by {@link ResourceMapping}.</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 05, 2008
 */
public interface ResourceMapping extends ContextRelated {

	/**
	 * Alias for backward compatibility - effectively it's converted to one-element array of {@link #getUrlPatterns()}
	 * @return
	 */
	String getAlias();

	/**
	 * URL mappings according to Servlet Specificication.
	 * @return
	 */
	String[] getUrlPatterns();

	/**
	 * Getter.
	 *
	 * @return local path in the bundle
	 */
	String getPath();

}
