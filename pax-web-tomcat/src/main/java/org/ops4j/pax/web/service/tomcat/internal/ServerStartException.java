package org.ops4j.pax.web.service.tomcat.internal;

/**
 * @author Romaim Gilles
 */
public class ServerStartException extends RuntimeException {
	/**
     * 
     */
	private static final long serialVersionUID = 7068742702603346470L;

	public ServerStartException(String serverInfo, Throwable cause) {
		super(String.format("cannot start server: '%s'", serverInfo), cause);
	}
}
