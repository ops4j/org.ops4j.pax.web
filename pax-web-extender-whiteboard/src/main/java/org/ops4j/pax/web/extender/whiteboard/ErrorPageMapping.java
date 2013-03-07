package org.ops4j.pax.web.extender.whiteboard;

/**
 * Registers an error page to customize the response sent back to the web client
 * in case that an exception or error propagates back to the web container, or
 * the servlet/filter calls sendError() on the response object for a specific
 * status code.
 * 
 * @author dsklyut
 * @since 0.7.0 Jun 23, 2009
 */
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
	 *         must start with an "/"
	 */
	String getLocation();
}
