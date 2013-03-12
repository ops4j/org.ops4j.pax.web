package org.ops4j.pax.web.service.tomcat.internal;

/**
 * Created with IntelliJ IDEA. User: romain.gilles Date: 6/11/12 Time: 2:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerStopException extends RuntimeException {
	/**
     *
     */
	private static final long serialVersionUID = 7694044273453973884L;

	public ServerStopException(String serverInfo, Throwable cause) {
		super(String.format("cannot stop server: '%s'", serverInfo), cause);
	}
}
