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
package org.ops4j.pax.web.extender.whiteboard.internal;

import org.osgi.service.http.HttpService;

/**
 * Listener on http service availability.
 * 
 * @author Alin Dreghiciu
 * @since August 21, 2007
 */
public interface HttpServiceListener {

	/**
	 * Called when an http service becomes available.
	 * 
	 * @param httpService
	 *            an http service
	 * @throws Exception
	 */
	void available(HttpService httpService) throws Exception;

	/**
	 * Called when an http service becomes unavailable.
	 * 
	 * @param httpService
	 *            an http service
	 */
	void unavailable(HttpService httpService);

}
