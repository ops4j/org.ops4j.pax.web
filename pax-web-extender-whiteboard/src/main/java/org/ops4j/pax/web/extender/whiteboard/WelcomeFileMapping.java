package org.ops4j.pax.web.extender.whiteboard;

/**
 * Welcome file mapping
 * 
 * @author dsklyut
 * @since 0.7.0
 */
public interface WelcomeFileMapping {

	/**
	 * Getter.
	 * 
	 * @return id of the http context this jsp belongs to
	 */
	String getHttpContextId();

	/**
	 * Getter
	 * 
	 * @return true if the client should be redirected to welcome file or false
	 *         if forwarded
	 */
	boolean isRedirect();

	/**
	 * Getter
	 * 
	 * @return an array of welcome files paths. Paths must not start or end with
	 *         "/"
	 */
	String[] getWelcomeFiles();

}
