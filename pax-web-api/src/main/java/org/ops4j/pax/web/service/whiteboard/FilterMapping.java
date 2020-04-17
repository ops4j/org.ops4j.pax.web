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

import java.util.Map;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;

/**
 * <p><em>Filter mapping</em> contains all the information required to register a {@link Filter}.</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 05, 2008
 */
public interface FilterMapping extends ContextRelated {

	/**
	 * <p>Get an actual {@link Filter} instance to register.</p>
	 *
	 * @return the filter to register
	 */
	Filter getFilter();

	/**
	 * <p>Get a class of {@link Filter} to register. Matches {@code <filter>/<filter-class>} element from
	 * {@code web.xml}. If {@link #getFilter()} is also specified, servlet class isn't used.</p>
	 * <p>There's no <em>whiteboard</em> specific method to specify this class.</p>
	 *
	 * @return the filter's class to instantiate and register
	 */
	Class<? extends Filter> getFilterClass();

	/**
	 * <p>Get a name of the filter being registered. Matches {@code <filter>/<filter-name>} element from
	 * {@code web.xml}.</p>
	 * <p>In <em>whiteboard</em> method, this can be specified as:<ul>
	 *     <li>{@link org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_FILTER_NAME}
	 *     property</li>
	 *     <li>{@code filter-name} service registration property (legacy Pax Web Whiteboard approach)</li>
	 * </ul></p>
	 * <p>If not specified, the name defaults to fully qualified class name of the filter.</p>
	 *
	 * @return name of the Servlet being mapped.
	 */
	String getFilterName();

	/**
	 * <p>Get URL patterns for the filter mapping.</p>
	 *
	 * @return an array of url patterns filter maps to
	 */
	String[] getUrlPatterns();

	/**
	 * <p>Get Regex patterns for the filter mapping.</p>
	 *
	 * @return an array of regex patterns filter maps to
	 */
	String[] getRegexPatterns();

	/**
	 * <p>Get Servlet names for the filter mapping.</p>
	 *
	 * @return an array of servlet names the filter was registered for
	 */
	String[] getServletNames();

	/**
	 * <p>Get an array of {@link DispatcherType} for this filter.</p>
	 *
	 * @return
	 */
	DispatcherType[] getDispatcherTypes();

	/**
	 * <p>Get flag for supporting asynchronous filter invocation.</p>
	 *
	 * @return Filter async-supported flag
	 */
	Boolean getAsyncSupported();

	/**
	 * <p>Get init parameters for the filter being registered.</p>
	 *
	 * @return map of initialization parameters.
	 */
	Map<String, String> getInitParameters();

}
