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
package org.ops4j.pax.web.extender.whiteboard;

import java.util.Map;

import org.osgi.service.http.HttpContext;

/**
 * Http context mapping.
 * 
 * @author Alin Dreghiciu
 * @since 0.4.0, March 16, 2008
 */
public interface HttpContextMapping {

	/**
	 * Getter.
	 * 
	 * @return context identifier; cannot be null
	 */
	String getHttpContextId();

	/**
	 * Getter.
	 * 
	 * @return context path as in servlet context path; can be null
	 */
	String getPath();

	/**
	 * Getter.
	 * 
	 * @return context parameters; can be null
	 */
	Map<String, String> getParameters();

	/**
	 * Getter.
	 * 
	 * @return associated HttpContext; can be null, case when a default http
	 *         context will be created and used
	 */
	HttpContext getHttpContext();

}