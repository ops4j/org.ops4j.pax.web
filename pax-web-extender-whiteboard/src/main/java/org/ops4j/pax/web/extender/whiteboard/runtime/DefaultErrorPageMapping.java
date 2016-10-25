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
package org.ops4j.pax.web.extender.whiteboard.runtime;


import org.ops4j.pax.web.service.whiteboard.ErrorPageMapping;

/**
 * Default implementation of
 * {@link org.ops4j.pax.web.service.whiteboard.ErrorPageMapping}
 */
public class DefaultErrorPageMapping implements ErrorPageMapping {

	/**
	 * Http Context id.
	 */
	private String httpContextId;

	/**
	 * Error code or fqn of Exception
	 */
	private String error;

	/**
	 * Location of error page
	 */
	private String location;

	/**
	 * @see org.ops4j.pax.web.service.whiteboard.ErrorPageMapping#getHttpContextId()
	 */
	public String getHttpContextId() {
		return httpContextId;
	}

	/**
	 * @see org.ops4j.pax.web.service.whiteboard.ErrorPageMapping#getError()
	 */
	public String getError() {
		return error;
	}

	/**
	 * @see org.ops4j.pax.web.service.whiteboard.ErrorPageMapping#getLocation()
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Setter.
	 *
	 * @param httpContextId id of the http context this error page belongs to
	 */
	public void setHttpContextId(String httpContextId) {
		this.httpContextId = httpContextId;
	}

	/**
	 * Setter
	 *
	 * @param error code or FQN of Exception class
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * Setter
	 *
	 * @param location location of error page
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() +
				"{" + "httpContextId=" + httpContextId +
				",error=" + error + ",location=" +
				location + "}";
	}
}
