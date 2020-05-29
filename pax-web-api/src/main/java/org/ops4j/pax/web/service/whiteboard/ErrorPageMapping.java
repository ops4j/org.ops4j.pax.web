/*
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
package org.ops4j.pax.web.service.whiteboard;

import org.ops4j.pax.web.annotations.Review;

/**
 * Registers an error page to customize the response sent back to the web client
 * in case that an exception or error propagates back to the web container, or
 * the servlet/filter calls sendError() on the response object for a specific
 * status code.
 *
 * @author dsklyut
 * @since 0.7.0 Jun 23, 2009
 */
@Review("Not yet refactored")
public interface ErrorPageMapping {

	/**
	 * Getter.
	 *
	 * @return id of the http context this filter belongs to
	 */
	String getHttpContextId();

	/**
	 * Getter
	 *
	 * @return error code or an FQN of the exception
	 */
	String getError();

	/**
	 * Getter
	 *
	 * @return the request path that will fill the response page. The location
	 * must start with an "/"
	 */
	String getLocation();
}
