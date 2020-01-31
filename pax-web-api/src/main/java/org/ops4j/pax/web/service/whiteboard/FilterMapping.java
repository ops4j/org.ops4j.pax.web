/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.whiteboard;

import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.ops4j.pax.web.service.PaxWebConstants;

/**
 * Filter mapping.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public interface FilterMapping {

	/**
	 * Getter.
	 *
	 * @return id of the http context this filter belongs to
	 */
	String getHttpContextId();

	/**
	 * Getter.
	 *
	 * @return filter
	 */
	Filter getFilter();

	/**
	 * Getter.
	 *
	 * @return an array of url patterns filter maps to
	 */
	String[] getUrlPatterns();

	/**
	 * Getter.
	 *
	 * @return an array of servlet names the filter was registered for
	 */
	String[] getServletNames();

	/**
	 * Getter.
	 *
	 * @return map of initialization parameters.
	 */
	Map<String, String> getInitParams();


	/**
	 * Getter
	 *
	 * @return if filter supports async communication.
	 */
	Boolean getAsyncSupported();


	/**
	 * Getter
	 *
	 * @return an array of DispatcherTypes the filter was registered for
	 * @see PaxWebConstants#FILTER_MAPPING_DISPATCHER
	 */
    DispatcherType[] getDispatcherType();

	/**
	 * Getter
	 *
	 * @return if supported, returns the name of this Filter
	 */
	String getName();
}