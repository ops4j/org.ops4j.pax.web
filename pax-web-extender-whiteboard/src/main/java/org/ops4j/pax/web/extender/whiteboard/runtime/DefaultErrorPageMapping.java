package org.ops4j.pax.web.extender.whiteboard.runtime;

import org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping;

/**
 * Default implementation of
 * {@link org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping}
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
	 * @see org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping#getHttpContextId()
	 */
	public String getHttpContextId() {
		return httpContextId;
	}

	/**
	 * @see org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping#getError()
	 */
	public String getError() {
		return error;
	}

	/**
	 * @see org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping#getLocation()
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Setter.
	 * 
	 * @param httpContextId
	 *            id of the http context this error page belongs to
	 */
	public void setHttpContextId(String httpContextId) {
		this.httpContextId = httpContextId;
	}

	/**
	 * Setter
	 * 
	 * @param error
	 *            code or FQN of Exception class
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * Setter
	 * 
	 * @param location
	 *            location of error page
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(this.getClass().getSimpleName())
				.append("{").append("httpContextId=").append(httpContextId)
				.append(",error=").append(error).append(",location=")
				.append(location).append("}").toString();
	}
}
