/*  Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.jetty.internal;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An HttpServletResponseWrapper that exposes the status of response.
 * 
 * @author Alin Dreghiciu
 * @since 0.2.3, December 21, 2007
 */
class HttpServiceResponseWrapper extends HttpServletResponseWrapper {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServiceResponseWrapper.class);
	/**
	 * Response status.
	 */
	private int status;

	/**
	 * Constructs a response adaptor wrapping the given response.
	 * 
	 * @param response
	 *            http servlet response to be wrapped
	 * 
	 * @throws IllegalArgumentException
	 *             if the response is null
	 */
	public HttpServiceResponseWrapper(final HttpServletResponse response) {
		super(response);
	}

	@Override
	public void setStatus(int sc) {
		LOG.debug("Response status set to [" + sc + "]");
		super.setStatus(sc);
		status = sc;
	}

	/**
	 * Returns the status if it had been set, or zero if not set.
	 * 
	 * @return status
	 */
	@Override
	public int getStatus() {
		return status;
	}

	/**
	 * Returns true if status has been set.
	 * 
	 * @return true if status has been set.
	 */
	public boolean isStatusSet() {
		return status != 0;
	}
}
