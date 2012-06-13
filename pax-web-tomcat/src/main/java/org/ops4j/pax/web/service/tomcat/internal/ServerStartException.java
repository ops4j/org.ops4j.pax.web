package org.ops4j.pax.web.service.tomcat.internal;

import org.apache.catalina.LifecycleException;

/**
 * Created with IntelliJ IDEA.
 * User: romain.gilles
 * Date: 6/11/12
 * Time: 2:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerStartException extends RuntimeException {
    public ServerStartException(String serverInfo, Throwable cause) {
        super(String.format("cannot start server: '%s'", serverInfo), cause);
    }
}
