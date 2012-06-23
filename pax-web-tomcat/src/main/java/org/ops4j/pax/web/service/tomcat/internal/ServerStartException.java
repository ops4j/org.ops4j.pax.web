package org.ops4j.pax.web.service.tomcat.internal;

/**
 * @author Romaim Gilles
 */
public class ServerStartException extends RuntimeException
{
    public ServerStartException(String serverInfo, Throwable cause)
    {
        super( String.format( "cannot start server: '%s'", serverInfo ), cause );
    }
}
